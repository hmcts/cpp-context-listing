package uk.gov.moj.cpp.listing.event.processor.xhibit.courtlist.mapper;

import static java.lang.String.format;
import static java.lang.String.valueOf;
import static java.util.UUID.fromString;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static uk.gov.moj.cpp.listing.domain.xhibit.generated.ProsecutingAuthorityType.CROWN_PROSECUTION_SERVICE;
import static uk.gov.moj.cpp.listing.domain.xhibit.generated.ProsecutingAuthorityType.OTHER_PROSECUTOR;
import static uk.gov.moj.cpp.listing.event.processor.xhibit.courtlist.XmlUtils.convertDate;

import uk.gov.moj.cpp.listing.domain.utils.DateAndTimeUtils;
import uk.gov.moj.cpp.listing.domain.xhibit.CourtLocation;
import uk.gov.moj.cpp.listing.domain.xhibit.generated.CasesStructure;
import uk.gov.moj.cpp.listing.domain.xhibit.generated.ChargeStructure;
import uk.gov.moj.cpp.listing.domain.xhibit.generated.CitizenNameStructure;
import uk.gov.moj.cpp.listing.domain.xhibit.generated.CourtHouseStructure;
import uk.gov.moj.cpp.listing.domain.xhibit.generated.CourtType;
import uk.gov.moj.cpp.listing.domain.xhibit.generated.DefenceStructure;
import uk.gov.moj.cpp.listing.domain.xhibit.generated.DefendantStructure;
import uk.gov.moj.cpp.listing.domain.xhibit.generated.DocumentIDstructure;
import uk.gov.moj.cpp.listing.domain.xhibit.generated.FixtureStructure;
import uk.gov.moj.cpp.listing.domain.xhibit.generated.HearingStructure;
import uk.gov.moj.cpp.listing.domain.xhibit.generated.HearingTypeStructure;
import uk.gov.moj.cpp.listing.domain.xhibit.generated.JudiciaryStructure;
import uk.gov.moj.cpp.listing.domain.xhibit.generated.ListHeaderStructure;
import uk.gov.moj.cpp.listing.domain.xhibit.generated.ObjectFactory;
import uk.gov.moj.cpp.listing.domain.xhibit.generated.OrganisationStructure;
import uk.gov.moj.cpp.listing.domain.xhibit.generated.PartyStructure;
import uk.gov.moj.cpp.listing.domain.xhibit.generated.PersonalDetailsStructure;
import uk.gov.moj.cpp.listing.domain.xhibit.generated.ProsecutingAuthorityType;
import uk.gov.moj.cpp.listing.domain.xhibit.generated.ProsecutionStructure;
import uk.gov.moj.cpp.listing.domain.xhibit.generated.SittingStructure;
import uk.gov.moj.cpp.listing.domain.xhibit.generated.SolicitorStructure;
import uk.gov.moj.cpp.listing.domain.xhibit.generated.YesNoType;
import uk.gov.moj.cpp.listing.event.processor.xhibit.XhibitReferenceDataService;
import uk.gov.moj.cpp.listing.event.processor.xhibit.courtlist.CourtListGenerationContext;
import uk.gov.moj.cpp.listing.event.processor.xhibit.courtlist.XmlUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.json.JsonArray;
import javax.json.JsonObject;

/**
 * Map to elements defined in CourtServices.xsd
 */
public class CourtServicesMapper {

    public static final String MASKED_VALUE = "******";
    public static final String OFFENCES = "offences";
    public static final String TITLE_PREFIX = "titlePrefix";
    public static final String TITLE_SUFFIX = "titleSuffix";
    public static final String FIRST_NAME = "firstName";
    public static final String LAST_NAME = "lastName";
    public static final String FORENAMES = "forenames";
    public static final String SURNAME = "surname";
    private static final String CASE_IDENTIFIER = "caseIdentifier";
    private static final String CASE_REFERENCE = "caseReference";
    private static final String APPLICATION_REFERENCE = "applicationReference";
    private static final String JUDICIAL_ID = "judicialId";
    private static final String NONE = "NONE";
    private static final String CPS_PROSECUTOR_CODE = "CPS";
    private static final List<String> JUDGE_JUDICIARY_TYPES = new ArrayList<>(Arrays.asList("DISTRICT_JUDGE", "CIRCUIT_JUDGE", "RECORDER"));
    private static final String RESTRICT_FROM_COURT_LIST = "restrictFromCourtList";
    private static final ObjectFactory objectFactory = new ObjectFactory();
    private static final int UNMAPPED_COURT_ROOM = 99;
    public static final String CPP_CASE_NUMBER = "CPP";
    public static final String DEFENCE_ORGANISATION = "defenceOrganisation";
    private static final String AM_PM_TIME_FORMAT = "h:mm a";
    private static final DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern(AM_PM_TIME_FORMAT, Locale.ENGLISH);
    private static final DateTimeFormatter sittingAtFormatter = DateTimeFormatter.ofPattern("HH:mm:ss");
    private static final String TIME_MARKING_NOTE_TEXT = "NOT BEFORE %s";
    private CourtListGenerationContext context;

    private XhibitReferenceDataService xhibitReferenceDataService;

    public CourtServicesMapper(final CourtListGenerationContext context,
                               final XhibitReferenceDataService xhibitReferenceDataService) {
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

    public CourtHouseStructure generateCrownCourtStructure(final UUID courtCentreId) {

        final CourtLocation courtLocation = xhibitReferenceDataService.getCourtDetails(context.getEnvelope(), courtCentreId);

        final CourtHouseStructure courtHouseStructure = objectFactory.createCourtHouseStructure();

        courtHouseStructure.setCourtHouseType(CourtType.valueOf(courtLocation.getCourtType()));
        courtHouseStructure.setCourtHouseCode(generateCrownCourtCode(courtLocation));
        courtHouseStructure.setCourtHouseName(courtLocation.getCourtName());

        return courtHouseStructure;
    }

    public CourtHouseStructure generateCourtHouseStructure(final UUID courtCentreId) {

        final CourtLocation courtLocation = xhibitReferenceDataService.getCourtDetails(context.getEnvelope(), courtCentreId);

        final CourtHouseStructure courtHouseStructure = objectFactory.createCourtHouseStructure();

        courtHouseStructure.setCourtHouseType(CourtType.valueOf(courtLocation.getCourtType()));
        courtHouseStructure.setCourtHouseCode(generateCourtHouseCode(courtLocation.getCrestCourtSiteId()));
        courtHouseStructure.setCourtHouseName(courtLocation.getCourtSiteName());

        return courtHouseStructure;
    }

    public CourtHouseStructure generateCourtHouseStructure(final JsonObject crestCourtSite) {

        final CourtHouseStructure courtHouseStructure = objectFactory.createCourtHouseStructure();

        courtHouseStructure.setCourtHouseType(CourtType.valueOf(crestCourtSite.getString("courtType", EMPTY)));
        courtHouseStructure.setCourtHouseCode(generateCourtHouseCode(crestCourtSite.getString("crestCourtSiteId", EMPTY)));
        courtHouseStructure.setCourtHouseName(crestCourtSite.getString("crestCourtSiteName", EMPTY));

        return courtHouseStructure;
    }

    private CourtHouseStructure.CourtHouseCode generateCrownCourtCode(final CourtLocation courtLocation) {
        final CourtHouseStructure.CourtHouseCode courtHouseCode = objectFactory.createCourtHouseStructureCourtHouseCode();
        courtHouseCode.setValue(courtLocation.getCrestCourtId());
        courtHouseCode.setCourtHouseShortName(courtLocation.getCourtShortName());

        return courtHouseCode;
    }

    private CourtHouseStructure.CourtHouseCode generateCourtHouseCode(final String crestCourtSiteId) {

        final CourtHouseStructure.CourtHouseCode courtHouseCode = objectFactory.createCourtHouseStructureCourtHouseCode();
        courtHouseCode.setValue(crestCourtSiteId);

        return courtHouseCode;
    }

    public SittingStructure generateSittingStructure(final JsonObject sittingJson, final int sittingSequenceNumber) {

        final SittingStructure sittingStructure = objectFactory.createSittingStructure();

        if (sittingJson.containsKey("courtRoomId")) {

            final UUID courtRoomId = fromString(sittingJson.getString("courtRoomId"));
            final UUID courtCentreId = context.getParameters().getCourtCentreId();

            sittingStructure.setCourtRoomNumber(xhibitReferenceDataService.getCourtRoomNumber(courtCentreId, courtRoomId));
            sittingStructure.setSittingPriority("T");
        } else {
            sittingStructure.setCourtRoomNumber(UNMAPPED_COURT_ROOM);
            sittingStructure.setSittingPriority("F");
        }

        sittingStructure.setSittingSequenceNo(valueOf(sittingSequenceNumber));
        sittingStructure.setJudiciary(generateJudiciaryStructure(sittingJson.getJsonArray("judiciary").getValuesAs(JsonObject.class)));
        sittingStructure.setHearings(generateSittingStructureHearings(sittingJson));

        final Optional<LocalDateTime> minimumStartTime = sittingJson.getJsonArray("hearings")
                .getValuesAs(JsonObject.class).stream()
                .map(hearingJson -> LocalDateTime.parse(hearingJson.getString("startTime")))
                .min(LocalDateTime::compareTo);

        if (minimumStartTime.isPresent()) {
            final ZonedDateTime localTime = DateAndTimeUtils.convertUTCToLocalTime(minimumStartTime.get());
            sittingStructure.setSittingAt(XmlUtils.convertDate(localTime.format(sittingAtFormatter)));
        }
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
        judgeStructure.getCitizenNameForename().add(NONE);
        judgeStructure.setCitizenNameSurname(NONE);

        return judgeStructure;
    }

    private JudiciaryStructure.Judge generateJudgeStructure(final UUID judiciaryId) {

        final JsonObject judiciary = xhibitReferenceDataService.getJudiciary(context.getEnvelope(), judiciaryId);

        final JudiciaryStructure.Judge judgeStructure = objectFactory.createJudiciaryStructureJudge();

        judgeStructure.setCitizenNameRequestedName(
                buildCitizenRequestedName(
                        judiciary.getString(TITLE_PREFIX, ""), judiciary.getString(FORENAMES), judiciary.getString(SURNAME), judiciary.getString(TITLE_SUFFIX, "")
                ));
        judgeStructure.getCitizenNameForename().add(judiciary.getString(FORENAMES));
        judgeStructure.setCitizenNameSurname(judiciary.getString(SURNAME));

        return judgeStructure;
    }

    private JudiciaryStructure.Justice generateJusticeStructure(final UUID judiciaryId) {

        final JsonObject judiciary = xhibitReferenceDataService.getJudiciary(context.getEnvelope(), judiciaryId);

        final JudiciaryStructure.Justice justice = objectFactory.createJudiciaryStructureJustice();

        justice.setCitizenNameRequestedName(
                buildCitizenRequestedName(
                        judiciary.getString(TITLE_PREFIX, ""), judiciary.getString(FORENAMES), judiciary.getString(SURNAME), judiciary.getString(TITLE_SUFFIX, "")
                ));
        justice.getCitizenNameForename().add(judiciary.getString(FORENAMES));
        justice.setCitizenNameSurname(judiciary.getString(SURNAME));

        return justice;
    }

    public SittingStructure.Hearings generateSittingStructureHearings(final JsonObject sittingJson) {

        final SittingStructure.Hearings sittingStructureHearings = objectFactory.createSittingStructureHearings();
        final boolean weekCommencing = sittingJson.getBoolean("weekCommencing");

        int hearingSequenceNumber = 1;

        final List<JsonObject> hearingJsonList = sittingJson.getJsonArray("hearings")
                .getValuesAs(JsonObject.class)
                .stream()
                .sorted(Comparator.comparing(hearing -> LocalDateTime.parse(((JsonObject) hearing).getString("startTime"))))
                .collect(Collectors.toList());

        for (final JsonObject hearingJson : hearingJsonList) {

            if (!hearingJson.getBoolean(RESTRICT_FROM_COURT_LIST)) {
                if (hearingJson.containsKey(CASE_IDENTIFIER)) {
                    sittingStructureHearings.getHearing().add(generateHearingStructureForListedCase(hearingJson, hearingSequenceNumber++, weekCommencing));
                }

                if (hearingJson.containsKey(APPLICATION_REFERENCE)) {
                    sittingStructureHearings.getHearing().add(generateHearingStructureForCourtApplication(hearingJson, hearingSequenceNumber++, weekCommencing));
                }
            }
        }

        return sittingStructureHearings;
    }

    private HearingStructure generateHearingStructureForListedCase(final JsonObject hearingJson,
                                                                   final int hearingSequenceNumber,
                                                                   final boolean weekCommencing) {

        final HearingStructure hearingStructure = objectFactory.createHearingStructure();

        hearingStructure.setHearingSequenceNumber(hearingSequenceNumber);
        hearingStructure.setCaseNumber(CPP_CASE_NUMBER);
        hearingStructure.setHearingDetails(generateHearingTypeStructure(hearingJson));
        hearingStructure.setProsecution(generateProsecutionStructure(hearingJson));
        hearingStructure.setDefendants(generateHearingStructureDefendantsForCase(hearingJson));

        if (!weekCommencing) {
            generateAndSetTimeMarkingNote(hearingJson, hearingStructure);
        }

        if (hearingJson.containsKey("courtCentreId")) {
            hearingStructure.setCommittingCourt(generateCourtHouseStructure(fromString(hearingJson.getString("courtCentreId"))));
        }

        return hearingStructure;
    }

    private HearingStructure generateHearingStructureForCourtApplication(final JsonObject hearingJson,
                                                                         final int hearingSequenceNumber,
                                                                         final boolean weekCommencing) {
        final HearingStructure hearingStructure = objectFactory.createHearingStructure();

        hearingStructure.setHearingSequenceNumber(hearingSequenceNumber);
        hearingStructure.setCaseNumber(CPP_CASE_NUMBER);
        hearingStructure.setHearingDetails(generateHearingTypeStructure(hearingJson));

        if (!weekCommencing) {
            generateAndSetTimeMarkingNote(hearingJson, hearingStructure);
        }

        // Map applicant to defendant
        hearingStructure.setDefendants(generateHearingStructureDefendantsForCourtApplication(hearingJson));

        return hearingStructure;
    }

    private HearingTypeStructure generateHearingTypeStructure(final JsonObject hearing) {

        final HearingTypeStructure hearingTypeStructure = objectFactory.createHearingTypeStructure();

        final JsonObject xhibitHearingType = xhibitReferenceDataService.getXhibitHearingType(context.getEnvelope(),
                fromString(hearing.getJsonObject("hearingType").getString("id")));

        hearingTypeStructure.setHearingDescription(xhibitHearingType.getString("exhibitHearingDescription"));

        if (hearing.containsKey("startTime")) {
            hearingTypeStructure.setHearingDate(LocalDateTime.parse(hearing.getString("startTime")).toLocalDate());
        }
        hearingTypeStructure.setHearingType(getHearingTypeForHearing(hearing));


        return hearingTypeStructure;
    }

    private ProsecutionStructure generateProsecutionStructure(final JsonObject listedCase) {

        final ProsecutionStructure prosecutionStructure = objectFactory.createProsecutionStructure();

        final String authorityType = listedCase.getJsonObject(CASE_IDENTIFIER).getString("authorityCode");

        final ProsecutingAuthorityType prosecutingAuthorityType = CPS_PROSECUTOR_CODE.equals(authorityType)
                ? CROWN_PROSECUTION_SERVICE
                : OTHER_PROSECUTOR;

        prosecutionStructure.setProsecutingAuthority(prosecutingAuthorityType);
        prosecutionStructure.setProsecutingReference(prosecutingAuthorityType.value());
        prosecutionStructure.setProsecutingOrganisation(generateProsecutingOrganisation(prosecutingAuthorityType));

        return prosecutionStructure;
    }

    private OrganisationStructure generateProsecutingOrganisation(final ProsecutingAuthorityType prosecutingAuthorityType) {
        final OrganisationStructure organisationStructure = objectFactory.createOrganisationStructure();
        organisationStructure.setOrganisationName(prosecutingAuthorityType.value());
        return organisationStructure;
    }

    private HearingStructure.Defendants generateHearingStructureDefendantsForCase(final JsonObject listedCase) {

        final HearingStructure.Defendants defendants = objectFactory.createHearingStructureDefendants();

        for (final JsonObject defendant : listedCase.getJsonArray("defendants").getValuesAs(JsonObject.class)) {
            defendants.getDefendant().add(generateDefendantStructureForDefendant(defendant,
                    listedCase.getJsonObject(CASE_IDENTIFIER).getString(CASE_REFERENCE)));
        }

        return defendants;
    }

    private HearingStructure.Defendants generateHearingStructureDefendantsForCourtApplication(final JsonObject courtApplication) {

        final HearingStructure.Defendants defendants = objectFactory.createHearingStructureDefendants();

        defendants.getDefendant().add(generateDefendantStructureForApplicant(courtApplication.getJsonObject("applicant"),
                courtApplication.getString(APPLICATION_REFERENCE)));

        return defendants;
    }

    private DefendantStructure generateDefendantStructureForDefendant(final JsonObject defendant, final String caseUrn) {

        final DefendantStructure defendantStructure = objectFactory.createDefendantStructure();

        defendantStructure.setURN(caseUrn);
        defendantStructure.setPersonalDetails(generatePersonalDetailsStructure(defendant));
        if (defendant.containsKey(OFFENCES)) {  // Court Applications don't have offences
            defendantStructure.setCharges(generateDefendantStructureCharges(defendant.getJsonArray(OFFENCES)));
        }
        if (defendant.containsKey(DEFENCE_ORGANISATION)) {
            defendantStructure.getCounsel().add(generateDefenceStructure(defendant.getString(DEFENCE_ORGANISATION)));
        }

        return defendantStructure;
    }

    private DefenceStructure generateDefenceStructure(final String defenceOrganisation) {

        final DefenceStructure defenceStructure = objectFactory.createDefenceStructure();

        defenceStructure.getSolicitor().add(generateSolictorStructure(defenceOrganisation));

        return defenceStructure;
    }

    private SolicitorStructure generateSolictorStructure(final String defenceOrganisation) {

        final SolicitorStructure solicitorStructure = objectFactory.createSolicitorStructure();

        solicitorStructure.setParty(generatePartyStructure(defenceOrganisation));

        return solicitorStructure;
    }

    private PartyStructure generatePartyStructure(final String defenceOrganisation) {

        final OrganisationStructure organisationStructure = objectFactory.createOrganisationStructure();

        organisationStructure.setOrganisationName(defenceOrganisation);

        final PartyStructure partyStructure = objectFactory.createPartyStructure();

        partyStructure.setOrganisation(organisationStructure);

        return partyStructure;
    }

    private DefendantStructure generateDefendantStructureForApplicant(final JsonObject applicant, final String urn) {

        final DefendantStructure defendantStructure = objectFactory.createDefendantStructure();

        defendantStructure.setURN(urn);
        defendantStructure.setPersonalDetails(generatePersonalDetailsStructure(applicant));

        return defendantStructure;
    }

    private PersonalDetailsStructure generatePersonalDetailsStructure(final JsonObject defendant) {

        final PersonalDetailsStructure personalDetailsStructure = objectFactory.createPersonalDetailsStructure();

        personalDetailsStructure.setName(generateCitizenNameStructure(defendant));
        personalDetailsStructure.setIsMasked(generateYesNoType(defendant.getBoolean(RESTRICT_FROM_COURT_LIST)));
        if (defendant.getBoolean(RESTRICT_FROM_COURT_LIST)) {
            personalDetailsStructure.setMaskedName(MASKED_VALUE);
        }

        return personalDetailsStructure;
    }

    private YesNoType generateYesNoType(final boolean yes) {

        return yes ? YesNoType.YES : YesNoType.NO;
    }

    private CitizenNameStructure generateCitizenNameStructure(final JsonObject defendant) {

        final CitizenNameStructure citizenNameStructure = objectFactory.createCitizenNameStructure();

        citizenNameStructure.getCitizenNameForename().add(defendant.getString(FIRST_NAME));
        citizenNameStructure.setCitizenNameSurname(defendant.getString(LAST_NAME));
        citizenNameStructure.setCitizenNameRequestedName(
                buildCitizenRequestedName(
                        defendant.getString(FIRST_NAME), defendant.getString(LAST_NAME)
                ));

        return citizenNameStructure;
    }

    private DefendantStructure.Charges generateDefendantStructureCharges(final JsonArray offences) {

        final DefendantStructure.Charges charges = objectFactory.createDefendantStructureCharges();

        for (final JsonObject offence : offences.getValuesAs(JsonObject.class)) {
            if (offence.getBoolean(RESTRICT_FROM_COURT_LIST)) {
                charges.getCharge().add(generateRestrictedChargeStructure());
            } else {
                charges.getCharge().add(generateChargeStructure(offence));
            }
        }

        return charges;
    }

    private ChargeStructure generateRestrictedChargeStructure() {

        final ChargeStructure chargeStructure = objectFactory.createChargeStructure();

        chargeStructure.setOffenceStatement(MASKED_VALUE);

        return chargeStructure;
    }

    @SuppressWarnings({"squid:CommentedOutCodeLine"})   // TODO Fix date conversion
    private ChargeStructure generateChargeStructure(final JsonObject offence) {

        final ChargeStructure chargeStructure = objectFactory.createChargeStructure();

        chargeStructure.setCJSoffenceCode(offence.getString("offenceCode"));
        chargeStructure.setOffenceStatement(offence.getJsonObject("statementOfOffence").getString("title"));

        return chargeStructure;
    }


    public String getHearingTypeForHearing(final JsonObject hearing) {

        final UUID cppHearingId = fromString(hearing.getJsonObject("hearingType").getString("id"));

        return getHearingTypeForHearing(cppHearingId);
    }

    public String getHearingTypeForHearing(final UUID cppHearingId) {

        if (cppHearingId == null) {
            return null;
        }

        return xhibitReferenceDataService.getXhibitHearingType(context.getEnvelope(), cppHearingId).getString("exhibitHearingCode");
    }

    public FixtureStructure generateFixtureStructure(final JsonObject sittingJson, final UUID hearingTypeId) {

        final FixtureStructure fixtureStructure = objectFactory.createFixtureStructure();

        if (!sittingJson.getBoolean("weekCommencing")) {
            fixtureStructure.setFixedDate(LocalDate.parse(sittingJson.getString("sittingDate")));

        }

        fixtureStructure.setCases(generateCasesStructure(sittingJson, hearingTypeId));

        return fixtureStructure;
    }

    private CasesStructure generateCasesStructure(final JsonObject sittingJson, final UUID pHearingTypeId) {

        final List<UUID> processedHearingTypes = new ArrayList<>();
        final CasesStructure casesStructure = objectFactory.createCasesStructure();

        for (final JsonObject hearingJson : sittingJson.getJsonArray("hearings").getValuesAs(JsonObject.class)) {

            final UUID hearingTypeId = fromString(hearingJson.getJsonObject("hearingType").getString("id"));
                verifyCaseStructureGeneration(pHearingTypeId, processedHearingTypes, casesStructure, hearingJson, hearingTypeId);
        }
        return casesStructure;
    }

    private void verifyCaseStructureGeneration(final UUID pHearingTypeId, final List<UUID> processedHearingTypes, final CasesStructure casesStructure, final JsonObject hearingJson, final UUID hearingTypeId) {
        if (hearingTypeId.equals(pHearingTypeId)) {
            processedHearingTypes.add(hearingTypeId);

            if(!hearingJson.getBoolean(RESTRICT_FROM_COURT_LIST)){
                generateCaseStructureForCaseOrCourtApplication(casesStructure, hearingJson);
            }
        }
    }

    private void generateCaseStructureForCaseOrCourtApplication(final CasesStructure casesStructure, final JsonObject hearingJson) {
        if (hearingJson.containsKey(CASE_IDENTIFIER)) {
            casesStructure.getCase().add(generateCaseStructureForCase(hearingJson));
        }

        if (hearingJson.containsKey(APPLICATION_REFERENCE)) {
            casesStructure.getCase().add(generateCaseStructureForCourtApplication(hearingJson));
        }
    }

    private CasesStructure.Case generateCaseStructureForCase(final JsonObject listedCase) {

        final CasesStructure.Case casesStructureCase = objectFactory.createCasesStructureCase();

        casesStructureCase.setCaseNumber(CPP_CASE_NUMBER);
        casesStructureCase.setHearing(generateHearingTypeStructure(listedCase));
        casesStructureCase.setProsecution(generateProsecutionStructure(listedCase));

        for (final JsonObject defendant : listedCase.getJsonArray("defendants").getValuesAs(JsonObject.class)) {
            casesStructureCase.getDefendants().add(generateCaseStructureCaseDefendants(defendant, listedCase.getJsonObject(CASE_IDENTIFIER).getString(CASE_REFERENCE)));
        }

        return casesStructureCase;
    }

    private CasesStructure.Case generateCaseStructureForCourtApplication(final JsonObject courtApplication) {
        final CasesStructure.Case casesStructureCase = objectFactory.createCasesStructureCase();

        casesStructureCase.setCaseNumber(CPP_CASE_NUMBER);
        casesStructureCase.setHearing(generateHearingTypeStructure(courtApplication));

        if (!courtApplication.getBoolean(RESTRICT_FROM_COURT_LIST)) {
            casesStructureCase.getDefendants().add(generateCaseStructureCaseDefendants(courtApplication.getJsonObject("applicant"), courtApplication.getString(APPLICATION_REFERENCE)));
        }

        return casesStructureCase;
    }

    private CasesStructure.Case.Defendants generateCaseStructureCaseDefendants(final JsonObject defendant, final String caseUrn) {

        final CasesStructure.Case.Defendants caseStructureCaseDefendants = objectFactory.createCasesStructureCaseDefendants();

        caseStructureCaseDefendants.setDefendant(generateDefendantStructureForDefendant(defendant, caseUrn));

        return caseStructureCaseDefendants;
    }

    private String buildCitizenRequestedName(final String forenames, final String surname) {
        return buildCitizenRequestedName("", forenames, surname, "");
    }

    private String buildCitizenRequestedName(final String titlePrefix, final String forenames, final String surname, final String titleSuffix) {
        return format("%s %s %s %s", titlePrefix, forenames, surname, titleSuffix).trim();
    }

    private void generateAndSetTimeMarkingNote(final JsonObject hearingJson, final HearingStructure hearingStructure) {
        final ZonedDateTime localTime = DateAndTimeUtils.convertUTCToLocalTime(LocalDateTime.parse(hearingJson.getString("startTime")));
        hearingStructure.setTimeMarkingNote(String.format(TIME_MARKING_NOTE_TEXT, localTime.format(timeFormatter)));
    }

}
