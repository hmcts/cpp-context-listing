package uk.gov.moj.cpp.listing.event.processor.xhibit.courtlist.generate;

import static java.lang.String.valueOf;
import static java.util.UUID.fromString;
import static uk.gov.moj.cpp.listing.domain.xhibit.generated.ProsecutingAuthorityType.CROWN_PROSECUTION_SERVICE;
import static uk.gov.moj.cpp.listing.domain.xhibit.generated.ProsecutingAuthorityType.OTHER_PROSECUTOR;
import static uk.gov.moj.cpp.listing.event.processor.xhibit.courtlist.XmlUtils.convertDate;

import uk.gov.moj.cpp.listing.domain.xhibit.CourtLocation;
import uk.gov.moj.cpp.listing.domain.xhibit.generated.ChargeStructure;
import uk.gov.moj.cpp.listing.domain.xhibit.generated.CitizenNameStructure;
import uk.gov.moj.cpp.listing.domain.xhibit.generated.CourtHouseStructure;
import uk.gov.moj.cpp.listing.domain.xhibit.generated.CourtType;
import uk.gov.moj.cpp.listing.domain.xhibit.generated.DefendantStructure;
import uk.gov.moj.cpp.listing.domain.xhibit.generated.DocumentIDstructure;
import uk.gov.moj.cpp.listing.domain.xhibit.generated.HearingStructure;
import uk.gov.moj.cpp.listing.domain.xhibit.generated.HearingTypeStructure;
import uk.gov.moj.cpp.listing.domain.xhibit.generated.JudiciaryStructure;
import uk.gov.moj.cpp.listing.domain.xhibit.generated.ListHeaderStructure;
import uk.gov.moj.cpp.listing.domain.xhibit.generated.ObjectFactory;
import uk.gov.moj.cpp.listing.domain.xhibit.generated.PersonalDetailsStructure;
import uk.gov.moj.cpp.listing.domain.xhibit.generated.ProsecutingAuthorityType;
import uk.gov.moj.cpp.listing.domain.xhibit.generated.ProsecutionStructure;
import uk.gov.moj.cpp.listing.domain.xhibit.generated.SittingStructure;
import uk.gov.moj.cpp.listing.domain.xhibit.generated.YesNoType;
import uk.gov.moj.cpp.listing.event.processor.xhibit.XhibitReferenceDataService;
import uk.gov.moj.cpp.listing.event.processor.xhibit.courtlist.CourtListGenerationContext;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import javax.json.JsonArray;
import javax.json.JsonObject;

public class CourtServicesGenerator {

    private static final String JUDICIAL_ID = "judicialId";
    private static final String NONE = "NONE";
    private static final ObjectFactory objectFactory = new ObjectFactory();
    private static final String CPS_PROSECUTOR_CODE = "CPS";
    private static final List JUDGE_JUDICIARY_TYPES = new ArrayList<>(Arrays.asList("DISTRICT_JUDGE", "CIRCUIT_JUDGE", "RECORDER"));
    private CourtListGenerationContext context;

    private XhibitReferenceDataService xhibitReferenceDataService;

    public CourtServicesGenerator(final CourtListGenerationContext context, final XhibitReferenceDataService xhibitReferenceDataService) {
        this.context = context;
        this.xhibitReferenceDataService = xhibitReferenceDataService;
    }

    public DocumentIDstructure generateDocumentID() {

        final DocumentIDstructure documentIDstructure = objectFactory.createDocumentIDstructure();

        documentIDstructure.setDocumentName(context.getMetadata().getFilename());
        documentIDstructure.setDocumentType(context.getParameters().getPublishCourtListType().getDocumentType());
        documentIDstructure.setUniqueID(context.getMetadata().getDocumentUniqueId());

        return documentIDstructure;
    }

    public ListHeaderStructure generateListHeader() {

        final ListHeaderStructure listHeaderStructure = objectFactory.createListHeaderStructure();

        listHeaderStructure.setListCategory("Criminal");
        listHeaderStructure.setStartDate(context.getParameters().getStartDate());
        listHeaderStructure.setEndDate(context.getParameters().getEndDate());
        listHeaderStructure.setVersion("NOT VERSIONED");
        final String publishedDate = context.getMetadata().getCreatedDate().toInstant().toString();
        listHeaderStructure.setPublishedTime(convertDate(publishedDate));

        return listHeaderStructure;
    }

    public CourtHouseStructure generateCourtHouseStructure(final UUID courtCentreId) {

        final CourtLocation courtLocation = xhibitReferenceDataService.getCourtDetails(context.getEnvelope(), courtCentreId);

        final CourtHouseStructure courtHouseStructure = objectFactory.createCourtHouseStructure();

        courtHouseStructure.setCourtHouseType(CourtType.valueOf(courtLocation.getCourtType()));
        courtHouseStructure.setCourtHouseCode(generateCourtHouseCode(courtLocation));
        courtHouseStructure.setCourtHouseName(courtLocation.getCourtFullName());

        return courtHouseStructure;
    }

    private CourtHouseStructure.CourtHouseCode generateCourtHouseCode(final CourtLocation courtLocation) {

        final CourtHouseStructure.CourtHouseCode courtHouseCode = objectFactory.createCourtHouseStructureCourtHouseCode();
        courtHouseCode.setValue(courtLocation.getCrestCourtSiteId());
        courtHouseCode.setCourtHouseShortName(courtLocation.getCourtShortName());

        return courtHouseCode;
    }

    public SittingStructure generateSittingStructure(final JsonObject hearing, final int sittingSequenceNumber) {

        final SittingStructure sittingStructure = objectFactory.createSittingStructure();

        final UUID courtRoomId = fromString(hearing.getString("courtRoomId"));

        sittingStructure.setCourtRoomNumber(xhibitReferenceDataService.getCourtRoomNumber(context.getEnvelope(), courtRoomId));
        sittingStructure.setSittingSequenceNo(valueOf(sittingSequenceNumber));
        sittingStructure.setSittingPriority("T");
        sittingStructure.setJudiciary(generateJudiciaryStructure(hearing.getJsonArray("judiciary").getValuesAs(JsonObject.class)));
        sittingStructure.setHearings(generateSittingStructureHearings(hearing));

        return sittingStructure;
    }

    private JudiciaryStructure generateJudiciaryStructure(final List<JsonObject> judiciaryList) {

        final JudiciaryStructure judiciaryStructure = objectFactory.createJudiciaryStructure();

        // Backstop to avoid missing mandatory Judge element
        judiciaryStructure.setJudge(defaultJudgeStructure());

        for (final JsonObject judiciaryReference : judiciaryList) {

            if (isJudge(judiciaryReference.getJsonObject("judicialRoleType").getString("judiciaryType"))) {
                final JudiciaryStructure.Judge judgeStructure = generateJudgeStructure(fromString(judiciaryReference.getString(JUDICIAL_ID)));
                judiciaryStructure.setJudge(judgeStructure);
            } else {
                final JudiciaryStructure.Justice justiceStructure = generateJusticeStructure(fromString(judiciaryReference.getString(JUDICIAL_ID)));
                judiciaryStructure.getJustice().add(justiceStructure);
            }
        }

        return judiciaryStructure;
    }

    @SuppressWarnings({"squid:S2250"})
    // The contains() method on a fixed list of 3 strings is unlikely to be a 'performance hot spot'
    private boolean isJudge(final String judiciaryType) {

        return JUDGE_JUDICIARY_TYPES.contains(judiciaryType);
    }

    private JudiciaryStructure.Judge defaultJudgeStructure() {

        final JudiciaryStructure.Judge judgeStructure = objectFactory.createJudiciaryStructureJudge();

        judgeStructure.setCitizenNameRequestedName(NONE);
        judgeStructure.setCitizenNameSurname(NONE);

        return judgeStructure;
    }

    private JudiciaryStructure.Judge generateJudgeStructure(final UUID judgeId) {

        final JsonObject judge = xhibitReferenceDataService.getJudge(context.getEnvelope(), judgeId);

        final JudiciaryStructure.Judge judgeStructure = objectFactory.createJudiciaryStructureJudge();

        judgeStructure.setCitizenNameRequestedName(judge.getString("firstName"));
        judgeStructure.setCitizenNameSurname(judge.getString("lastName"));

        return judgeStructure;
    }

    private JudiciaryStructure.Justice generateJusticeStructure(final UUID judiciaryId) {

        final JsonObject judiciary = xhibitReferenceDataService.getJudiciary(context.getEnvelope(), judiciaryId);

        final JudiciaryStructure.Justice justice = objectFactory.createJudiciaryStructureJustice();

        justice.setCitizenNameRequestedName(judiciary.getString("forenames"));
        justice.setCitizenNameSurname(judiciary.getString("surname"));

        return justice;
    }

    @SuppressWarnings({"squid:CommentedOutCodeLine"})
    private SittingStructure.Hearings generateSittingStructureHearings(final JsonObject hearing) {

        final SittingStructure.Hearings sittingStructureHearings = objectFactory.createSittingStructureHearings();

        int hearingSequenceNumber = 1;

        for (final JsonObject listedCase : hearing.getJsonArray("listedCases").getValuesAs(JsonObject.class)) {
            sittingStructureHearings.getHearing().add(generateHearingStructureForListedCase(hearing,
                    listedCase, hearingSequenceNumber++));
        }

        for (final JsonObject courtApplication : hearing.getJsonArray("courtApplications").getValuesAs(JsonObject.class)) {
            sittingStructureHearings.getHearing().add(generateHearingStructureForCourtApplication(hearing,
                    courtApplication, hearingSequenceNumber++));
        }

        return sittingStructureHearings;
    }

    private HearingStructure generateHearingStructureForListedCase(final JsonObject hearing,
                                                                   final JsonObject listedCase,
                                                                   final int hearingSequenceNumber) {

        final HearingStructure hearingStructure = objectFactory.createHearingStructure();

        hearingStructure.setHearingSequenceNumber(hearingSequenceNumber);
        hearingStructure.setCaseNumber("A12345678");    // TODO SCSL-187 Use dummy value until new schema from CGI supports CPP URNs in CaseNumber
        hearingStructure.setHearingDetails(generateHearingTypeStructure(hearing));
        hearingStructure.setProsecution(generateProsecutionStructure(listedCase));
        hearingStructure.setCommittingCourt(generateCourtHouseStructure(fromString(hearing.getString("courtCentreId"))));
        hearingStructure.setDefendants(generateHearingStructureDefendantsForCase(listedCase));

        return hearingStructure;
    }

    private HearingStructure generateHearingStructureForCourtApplication(final JsonObject hearing,
                                                                         final JsonObject courtApplication,
                                                                         final int hearingSequenceNumber) {
        final HearingStructure hearingStructure = objectFactory.createHearingStructure();

        hearingStructure.setHearingSequenceNumber(hearingSequenceNumber);
        hearingStructure.setCaseNumber("A12345678");    // TODO SCSL-187 Use dummy value until new schema from CGI supports CPP URNs in CaseNumber
        hearingStructure.setHearingDetails(generateHearingTypeStructure(hearing));

        // Map applicant to defendant
        hearingStructure.setDefendants(generateHearingStructureDefendantsForCourtApplication(courtApplication));

        return hearingStructure;
    }

    private HearingTypeStructure generateHearingTypeStructure(final JsonObject hearing) {

        final HearingTypeStructure hearingTypeStructure = objectFactory.createHearingTypeStructure();

        hearingTypeStructure.setHearingDescription(hearing.getJsonObject("type").getString("description")); // TODO SCSL-85 Use XHIBIT hearing type
        hearingTypeStructure.setHearingDate(LocalDate.parse(hearing.getString("startDate")));

        return hearingTypeStructure;
    }

    private ProsecutionStructure generateProsecutionStructure(final JsonObject listedCase) {

        final ProsecutionStructure prosecutionStructure = objectFactory.createProsecutionStructure();

        prosecutionStructure.setProsecutingReference(listedCase.getJsonObject("caseIdentifier").getString("caseReference"));

        final String authorityType = listedCase.getJsonObject("caseIdentifier").getString("authorityCode");

        final ProsecutingAuthorityType prosecutingAuthorityType = CPS_PROSECUTOR_CODE.equals(authorityType)
                ? CROWN_PROSECUTION_SERVICE
                : OTHER_PROSECUTOR;

        prosecutionStructure.setProsecutingAuthority(prosecutingAuthorityType);

        return prosecutionStructure;
    }

    private HearingStructure.Defendants generateHearingStructureDefendantsForCase(final JsonObject listedCase) {

        final HearingStructure.Defendants defendants = objectFactory.createHearingStructureDefendants();

        for (final JsonObject defendant : listedCase.getJsonArray("defendants").getValuesAs(JsonObject.class)) {
            defendants.getDefendant().add(generateDefendantStructureForDefendant(defendant));
        }

        return defendants;
    }

    private HearingStructure.Defendants generateHearingStructureDefendantsForCourtApplication(final JsonObject courtApplication) {

        final HearingStructure.Defendants defendants = objectFactory.createHearingStructureDefendants();

        defendants.getDefendant().add(generateDefendantStructureForApplicant(courtApplication.getJsonObject("applicant")));

        return defendants;
    }

    private DefendantStructure generateDefendantStructureForDefendant(final JsonObject defendant) {

        final DefendantStructure defendantStructure = objectFactory.createDefendantStructure();

        defendantStructure.setPersonalDetails(generatePersonalDetailsStructure(defendant));
        defendantStructure.setCharges(generateDefendantStructureCharges(defendant.getJsonArray("offences")));

        return defendantStructure;
    }

    private DefendantStructure generateDefendantStructureForApplicant(final JsonObject applicant) {

        final DefendantStructure defendantStructure = objectFactory.createDefendantStructure();

        defendantStructure.setPersonalDetails(generatePersonalDetailsStructure(applicant));

        return defendantStructure;
    }

    private PersonalDetailsStructure generatePersonalDetailsStructure(final JsonObject defendant) {

        final PersonalDetailsStructure personalDetailsStructure = objectFactory.createPersonalDetailsStructure();

        personalDetailsStructure.setName(generateCitizenNameStructure(defendant));
        personalDetailsStructure.setIsMasked(generateYesNoType(defendant.getBoolean("restrictFromCourtList")));

        return personalDetailsStructure;
    }

    private YesNoType generateYesNoType(final boolean yes) {

        return yes ? YesNoType.YES : YesNoType.NO;
    }

    private CitizenNameStructure generateCitizenNameStructure(final JsonObject defendant) {

        final CitizenNameStructure citizenNameStructure = objectFactory.createCitizenNameStructure();

        citizenNameStructure.setCitizenNameSurname(defendant.getString("lastName"));
        citizenNameStructure.setCitizenNameRequestedName(defendant.getString("firstName"));

        return citizenNameStructure;
    }

    private DefendantStructure.Charges generateDefendantStructureCharges(final JsonArray offences) {

        final DefendantStructure.Charges charges = objectFactory.createDefendantStructureCharges();

        for (final JsonObject offence : offences.getValuesAs(JsonObject.class)) {

            charges.getCharge().add(generateChargeStructure(offence));
        }

        return charges;
    }

    @SuppressWarnings({"squid:CommentedOutCodeLine"})   // TODO Fix date conversion
    private ChargeStructure generateChargeStructure(final JsonObject offence) {

        final ChargeStructure chargeStructure = objectFactory.createChargeStructure();

        chargeStructure.setCJSoffenceCode(offence.getString("offenceCode"));
        chargeStructure.setOffenceStatement(offence.getJsonObject("statementOfOffence").getString("title"));
//        chargeStructure.setOffenceStartDateTime(convertDate(offence.getString("startDate")));
//        if (offence.containsKey("endDate")) {
//            chargeStructure.setOffenceEndDateTime(convertDate(offence.getString("endDate")));
//        }

        return chargeStructure;
    }
}
