package uk.gov.moj.cpp.listing.domain.event;


import static java.util.stream.Collectors.toList;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;

import uk.gov.justice.core.courts.Address;
import uk.gov.justice.core.courts.BailStatus;
import uk.gov.justice.core.courts.CustodyTimeLimit;
import uk.gov.justice.core.courts.Defendant;
import uk.gov.justice.core.courts.JurisdictionType;
import uk.gov.justice.core.courts.LaaReference;
import uk.gov.justice.core.courts.Marker;
import uk.gov.justice.core.courts.Offence;
import uk.gov.justice.core.courts.Organisation;
import uk.gov.justice.core.courts.Person;
import uk.gov.justice.core.courts.PersonDefendant;
import uk.gov.justice.core.courts.ProsecutionCase;
import uk.gov.justice.core.courts.ProsecutionCaseIdentifier;
import uk.gov.justice.core.courts.Prosecutor;
import uk.gov.justice.core.courts.ReportingRestriction;
import uk.gov.justice.core.courts.SeedingHearing;
import uk.gov.justice.listing.events.CaseIdentifier;
import uk.gov.justice.listing.events.ListedCase;
import uk.gov.justice.listing.events.StatementOfOffence;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.junit.jupiter.api.Test;

public class CourtToEventConverterTest {


    private static final UUID CASE_ID = UUID.randomUUID();
    private static final UUID AUTHORITY_ID = UUID.randomUUID();
    private static final String AUTHORITY_CODE = "TVL";
    private static final String CASE_URN = "92GD4714020";
    private static final UUID MARKER_ID = UUID.randomUUID();
    private static final String MARKER_TYPE_CODE = "HT";
    private static final String MARKER_TYPE_DESCRIPTION = "Human";
    private static final UUID MARKER_TYPEID = UUID.randomUUID();
    private static final String LAST_NAME = "SMITH";
    private static final String FIRST_NAME = "TIM";
    private static final String ADDRESS_1 = "74542 Aniya Junctions";
    private static final String POST_CODE = "WA9 1AB";
    private static final String DATE_OF_BIRTH = "2000-10-23";
    private static final UUID OFFENCE_ID = UUID.randomUUID();
    private static final String OFFENCE_END_DATE = "05-05-2020";
    private static final UUID LA_REFERENCE_ID = UUID.randomUUID();
    private static final String LAA_REFERENCE_DESCRIPTION = "Laa Description";
    private static final String LAA_REFERENCE_STATUS_DATE = "07-05-2020";
    private static final String LAA_REFERENCE_STATUS_CODE = "LA Status Code";
    private static final String LAA_REFERENCE_START_DATE = "04-05-2020";
    private static final String LAA_REFERENCE_END_DATE = "06-05-2020";
    private static final String LAA_APPLICATION_REFERENCE = "Application Reference";
    private static final String OFFENCE_START_DATE = "04-04-2020";
    private static final String OFFENCE_CODE = "CA03014";
    private static final String OFFENCE_WORDING = "Some Offence Wording";
    private static final String WELSH_TITLE = "Some Welsh Title";
    private static final String LEGISLATION_WELSH = "Some Welsh Section";
    private static final String OFFENCE_LEGISLATION = "Section 18";
    private static final String OFFENCE_TITLE = "Some Title";
    private static final String LAID_DATE = "07-04-2020";
    private static final UUID DEFENDANT_ID = UUID.randomUUID();
    private static final UUID MASTER_DEFENDANT_ID = UUID.randomUUID();
    private static final ZonedDateTime COURT_PROCEEDINGS_INITIATED = ZonedDateTime.now();
    private static final UUID BAIL_ID = UUID.randomUUID();
    private static final String BAIL_DESCRIPTION = "BAIL_First hearing";
    private static final String BAIL_CODE = "C";
    private static final boolean YOUTH = true;
    private static final String CASE_STATUS = "CASE STATUS";
    private static final String ORGANIZATION_NAME = "Organization Name";
    private static final int DAYS_SPENT = 1;
    private static final String TIME_LIMIT = "Time Limit";
    private static final UUID RR_ID = UUID.randomUUID();
    private static final UUID RR_JUDICIAL_RESULT_ID = UUID.randomUUID();
    private static final String RR_LABEL = "RR Label 1";
    private static final LocalDate RR_ORDERED_DATE = LocalDate.now();
    private static final UUID SEEDING_HEARING_ID = UUID.randomUUID();
    private static final String SITTING_DAY = LocalDate.now().toString();


    private static final String PROSECUTOR_NAME = "prosecutorName";
    private static final String PROSECUTOR_CODE = "prosecutorCode";
    private static final String ADDRESS1 = "address1";
    private static final String ADDRESS2 = "address2";

    @Test
    public void shouldConvert() {
        final ListedCase listedCase = CourtToEventConverter.buildListedCase(getSampleProsecutionCase(), Collections.emptyList());

        assertThat(CASE_ID, is(listedCase.getId()));
        assertThat(CASE_STATUS, is(listedCase.getCaseStatus()));
        assertThat(false, is(listedCase.getRestrictFromCourtList()));
        assertThat(listedCase.getShadowListed(), is(false));

        final CaseIdentifier caseIdentifier = listedCase.getCaseIdentifier();
        assertThat(CASE_URN, is(caseIdentifier.getCaseReference()));
        assertThat(AUTHORITY_CODE, is(caseIdentifier.getAuthorityCode()));
        assertThat(AUTHORITY_ID, is(caseIdentifier.getAuthorityId()));

        final uk.gov.justice.listing.events.Marker marker = listedCase.getMarkers().get(0);
        assertThat(MARKER_ID, is(marker.getId()));
        assertThat(MARKER_TYPE_CODE, is(marker.getMarkerTypeCode()));
        assertThat(MARKER_TYPE_DESCRIPTION, is(marker.getMarkerTypeDescription()));
        assertThat(MARKER_TYPEID, is(marker.getMarkerTypeid()));

        final uk.gov.justice.listing.events.Defendant defendant = listedCase.getDefendants().get(0);
        assertThat(DEFENDANT_ID, is(defendant.getId()));
        assertThat(MASTER_DEFENDANT_ID, is(defendant.getMasterDefendantId()));
        assertThat(COURT_PROCEEDINGS_INITIATED, is(defendant.getCourtProceedingsInitiated()));
        assertThat(DATE_OF_BIRTH, is(defendant.getDateOfBirth()));
        assertThat(FIRST_NAME, is(defendant.getFirstName()));
        assertThat(LAST_NAME, is(defendant.getLastName()));
        assertThat(ORGANIZATION_NAME, is(defendant.getOrganisationName()));

        final BailStatus bailStatus = defendant.getBailStatus();
        assertThat(BAIL_CODE, is(bailStatus.getCode()));
        assertThat(BAIL_DESCRIPTION, is(bailStatus.getDescription()));
        assertThat(BAIL_ID, is(bailStatus.getId()));

        final CustodyTimeLimit custodyTimeLimit = bailStatus.getCustodyTimeLimit();
        assertThat(DAYS_SPENT, is(custodyTimeLimit.getDaysSpent()));
        assertThat(TIME_LIMIT, is(custodyTimeLimit.getTimeLimit()));

        assertThat(false, is(defendant.getRestrictFromCourtList()));
        assertThat(YOUTH, is(defendant.getIsYouth()));
        assertThat(ADDRESS_1, is(defendant.getAddress().getAddress1()));
        assertThat(defendant.getAddress().getAddress2(), is(nullValue()));
        assertThat(defendant.getAddress().getAddress3(), is(nullValue()));
        assertThat(defendant.getAddress().getAddress4(), is(nullValue()));
        assertThat(defendant.getAddress().getAddress5(), is(nullValue()));
        assertThat(POST_CODE, is(defendant.getAddress().getPostcode()));


        final uk.gov.justice.listing.events.Offence offence = defendant.getOffences().get(0);
        assertThat(OFFENCE_ID, is(offence.getId()));
        assertThat(OFFENCE_START_DATE, is(offence.getStartDate()));
        assertThat(OFFENCE_END_DATE, is(offence.getEndDate()));
        assertThat(OFFENCE_CODE, is(offence.getOffenceCode()));
        assertThat(OFFENCE_WORDING, is(offence.getOffenceWording()));
        assertThat(Boolean.FALSE, is(offence.getRestrictFromCourtList()));
        assertThat(LAID_DATE, is(offence.getLaidDate()));
        assertThat(offence.getShadowListed(), is(false));

        final uk.gov.justice.listing.events.LaaReference laaReference = offence.getLaaApplnReference();
        assertThat(LAA_APPLICATION_REFERENCE, is(laaReference.getApplicationReference()));
        assertThat(LAA_REFERENCE_END_DATE, is(laaReference.getEffectiveEndDate()));
        assertThat(LAA_REFERENCE_START_DATE, is(laaReference.getEffectiveStartDate()));
        assertThat(LAA_REFERENCE_STATUS_CODE, is(laaReference.getStatusCode()));
        assertThat(LAA_REFERENCE_STATUS_DATE, is(laaReference.getStatusDate()));
        assertThat(LAA_REFERENCE_DESCRIPTION, is(laaReference.getStatusDescription()));
        assertThat(LA_REFERENCE_ID, is(laaReference.getStatusId()));

        final StatementOfOffence statementOfOffence = offence.getStatementOfOffence();
        assertThat(OFFENCE_LEGISLATION, is(statementOfOffence.getLegislation()));
        assertThat(OFFENCE_TITLE, is(statementOfOffence.getTitle()));
        assertThat(WELSH_TITLE, is(statementOfOffence.getWelshTitle()));
        assertThat(LEGISLATION_WELSH, is(statementOfOffence.getWelshLegislation()));

        final uk.gov.justice.listing.events.Prosecutor prosecutor = listedCase.getProsecutor();
        assertThat(prosecutor.getProsecutorId(), is(notNullValue()));
        assertThat(PROSECUTOR_NAME, is(prosecutor.getProsecutorName()));
        assertThat(PROSECUTOR_CODE, is(prosecutor.getProsecutorCode()));
        assertThat(ADDRESS1, is(prosecutor.getAddress().getAddress1()));
        assertThat(ADDRESS2, is(prosecutor.getAddress().getAddress2()));
        assertThat(prosecutor.getAddress().getAddress3(), is(nullValue()));

        final uk.gov.justice.listing.events.SeedingHearing seedingHearing = offence.getSeedingHearing();
        assertThat(seedingHearing.getSeedingHearingId(), is(SEEDING_HEARING_ID));
        assertThat(seedingHearing.getJurisdictionType(), is(JurisdictionType.CROWN));
        assertThat(seedingHearing.getSittingDay(), is(SITTING_DAY));

    }

    @Test
    public void shouldSetShadowListedFlag() {
        final ListedCase listedCase = CourtToEventConverter.buildListedCase(getSampleProsecutionCase(), Arrays.asList(OFFENCE_ID));

        assertThat(listedCase.getShadowListed(), is(true));
        assertThat(listedCase.getDefendants().get(0).getOffences().get(0).getShadowListed(), is(true));
    }

    @Test
    public void shouldSetProsecutorObject() {
        final ListedCase listedCase = CourtToEventConverter.buildListedCase(getSampleProsecutionCaseWithProsecutorMadatoryFields(), Arrays.asList(OFFENCE_ID));

        final uk.gov.justice.listing.events.Prosecutor prosecutor = listedCase.getProsecutor();
        assertThat(prosecutor.getProsecutorId(), is(notNullValue()));
        assertThat(prosecutor.getProsecutorName(), is(nullValue()));
        assertThat(PROSECUTOR_CODE, is(prosecutor.getProsecutorCode()));
        assertThat(prosecutor.getAddress(), is(nullValue()));
    }

    @Test
    public void shouldNotCarryShadowListedFlagWhenOneOffenceIsNotShadowListed() {
        final ProsecutionCase prosecutionCase = getProsecutionCaseWithMultipleDefendantsAndOffences(2, 2);
        final List<UUID> shadowListedOffences = prosecutionCase.getDefendants().get(0)
                .getOffences().stream().map(offence -> offence.getId())
                .collect(Collectors.toList());

        final ListedCase listedCase = CourtToEventConverter.buildListedCase(prosecutionCase, shadowListedOffences);
        final uk.gov.justice.listing.events.Defendant defendant1 = listedCase.getDefendants().get(0);
        final uk.gov.justice.listing.events.Defendant defendant2 = listedCase.getDefendants().get(1);

        assertThat(listedCase.getShadowListed(), is(false));

        assertThat(defendant1.getOffences().get(0).getShadowListed(), is(true));
        assertThat(defendant1.getOffences().get(1).getShadowListed(), is(true));

        assertThat(defendant2.getOffences().get(0).getShadowListed(), is(false));
        assertThat(defendant2.getOffences().get(1).getShadowListed(), is(false));
    }

    @Test
    public void shouldPopulateReportingRestrictions() {
        final ListedCase listedCase = CourtToEventConverter.buildListedCase(getSampleProsecutionCase(), Arrays.asList(OFFENCE_ID));
        final uk.gov.justice.listing.events.ReportingRestriction rr = listedCase.getDefendants().get(0).getOffences().get(0).getReportingRestrictions().get(0);

        assertThat(rr.getId(), is(RR_ID));
        assertThat(rr.getJudicialResultId(), is(RR_JUDICIAL_RESULT_ID));
        assertThat(rr.getLabel(), is(RR_LABEL));
        assertThat(rr.getOrderedDate(), is(RR_ORDERED_DATE));
    }

    private ProsecutionCase getSampleProsecutionCase() {
        return ProsecutionCase.prosecutionCase()
                .withId(CASE_ID)
                .withProsecutionCaseIdentifier(getSampleCaseIdentifier())
                .withCaseMarkers(getSampleCaseMarkers())
                .withDefendants(getSampleDefendants())
                .withCaseStatus(CASE_STATUS)
                .withProsecutor(getProsecutor())
                .build();

    }

    private ProsecutionCase getSampleProsecutionCaseWithProsecutorMadatoryFields() {
        return ProsecutionCase.prosecutionCase()
                .withId(CASE_ID)
                .withProsecutionCaseIdentifier(getSampleCaseIdentifier())
                .withCaseMarkers(getSampleCaseMarkers())
                .withDefendants(getSampleDefendants())
                .withCaseStatus(CASE_STATUS)
                .withProsecutor(getProsecutorWithMandatoryFields())
                .build();

    }

    private Prosecutor getProsecutor(){
        return Prosecutor.prosecutor()
                .withProsecutorId(UUID.randomUUID())
                .withProsecutorName(PROSECUTOR_NAME)
                .withProsecutorCode(PROSECUTOR_CODE)
                .withAddress(Address.address()
                        .withAddress1(ADDRESS1)
                        .withAddress2(ADDRESS2)
                        .build())
                .build();
    }

    private Prosecutor getProsecutorWithMandatoryFields(){
        return Prosecutor.prosecutor()
                .withProsecutorId(UUID.randomUUID())
                .withProsecutorCode(PROSECUTOR_CODE)
                .build();
    }

    private List<Defendant> getSampleDefendants() {
        return Arrays.asList(Defendant.defendant()
                .withId(DEFENDANT_ID)
                .withMasterDefendantId(MASTER_DEFENDANT_ID)
                .withCourtProceedingsInitiated(COURT_PROCEEDINGS_INITIATED)
                .withDefenceOrganisation(Organisation.organisation()
                        .withName(ORGANIZATION_NAME)
                        .build())
                .withIsYouth(YOUTH)
                .withPersonDefendant(getSamplePersonDefendant())
                .withOffences(getSampleOffences())
                .build());
    }

    private List<Offence> getSampleOffences() {
        return Arrays.asList(Offence.offence()
                .withId(OFFENCE_ID)
                .withEndDate(OFFENCE_END_DATE)
                .withStartDate(OFFENCE_START_DATE)
                .withOffenceCode(OFFENCE_CODE)
                .withWording(OFFENCE_WORDING)
                .withOffenceLegislation(OFFENCE_LEGISLATION)
                .withOffenceTitle(OFFENCE_TITLE)
                .withOffenceLegislationWelsh(LEGISLATION_WELSH)
                .withOffenceTitleWelsh(WELSH_TITLE)
                .withLaaApplnReference(getSampleLaaReference())
                .withLaidDate(LAID_DATE)
                .withReportingRestrictions(getSampleReportingRestrictions())
                .withSeedingHearing(getSampleSeedingHearing())
                .build());
    }

    private LaaReference getSampleLaaReference() {
        return LaaReference.laaReference()
                .withApplicationReference(LAA_APPLICATION_REFERENCE)
                .withEffectiveEndDate(LAA_REFERENCE_END_DATE)
                .withEffectiveStartDate(LAA_REFERENCE_START_DATE)
                .withStatusCode(LAA_REFERENCE_STATUS_CODE)
                .withStatusDate(LAA_REFERENCE_STATUS_DATE)
                .withStatusDescription(LAA_REFERENCE_DESCRIPTION)
                .withStatusId(LA_REFERENCE_ID)
                .build();
    }

    private PersonDefendant getSamplePersonDefendant() {
        return PersonDefendant.personDefendant()
                .withPersonDetails(getSamplePersonDetail())
                .withBailStatus(BailStatus.bailStatus()
                        .withCode(BAIL_CODE)
                        .withDescription(BAIL_DESCRIPTION)
                        .withId(BAIL_ID)
                        .withCustodyTimeLimit(CustodyTimeLimit.custodyTimeLimit()
                                .withDaysSpent(DAYS_SPENT)
                                .withTimeLimit(TIME_LIMIT)
                                .build())
                        .build())
                .build();
    }

    private Person getSamplePersonDetail() {
        return Person.person()
                .withLastName(LAST_NAME)
                .withFirstName(FIRST_NAME)
                .withAddress(getSampleAddress())
                .withDateOfBirth(DATE_OF_BIRTH)
                .build();
    }

    private Address getSampleAddress() {
        return Address.address()
                .withAddress1(ADDRESS_1)
                .withPostcode(POST_CODE)
                .build();
    }

    private ProsecutionCaseIdentifier getSampleCaseIdentifier() {
        return ProsecutionCaseIdentifier.prosecutionCaseIdentifier()
                .withCaseURN(CASE_URN)
                .withProsecutionAuthorityCode(AUTHORITY_CODE)
                .withProsecutionAuthorityId(AUTHORITY_ID)
                .build();

    }

    private List<Marker> getSampleCaseMarkers() {
        return Arrays.asList(Marker.marker()
                .withId(MARKER_ID)
                .withMarkerTypeCode(MARKER_TYPE_CODE)
                .withMarkerTypeDescription(MARKER_TYPE_DESCRIPTION)
                .withMarkerTypeid(MARKER_TYPEID)
                .build());
    }

    private ProsecutionCase getProsecutionCaseWithMultipleDefendantsAndOffences(int defendantCount, int offencesCount) {
        return ProsecutionCase.prosecutionCase()
                .withId(CASE_ID)
                .withProsecutionCaseIdentifier(getSampleCaseIdentifier())
                .withCaseMarkers(getSampleCaseMarkers())
                .withDefendants(getMultipleDefendants(defendantCount, offencesCount))
                .withCaseStatus(CASE_STATUS)
                .build();
    }

    private List<Defendant> getMultipleDefendants(int defendantCount, int offencesCount) {
        return IntStream.range(0, defendantCount)
                .mapToObj((int i) -> getRandomDefendant(offencesCount))
                .collect(toList());
    }

    private Defendant getRandomDefendant(int offencesCount) {
        return Defendant.defendant()
                .withId(UUID.randomUUID())
                .withMasterDefendantId(UUID.randomUUID())
                .withCourtProceedingsInitiated(COURT_PROCEEDINGS_INITIATED)
                .withDefenceOrganisation(Organisation.organisation()
                        .withName(ORGANIZATION_NAME)
                        .build())
                .withIsYouth(YOUTH)
                .withPersonDefendant(getSamplePersonDefendant())
                .withOffences(getMultipleOffences(offencesCount))
                .build();
    }

    private List<Offence> getMultipleOffences(int offencesCount) {
        return IntStream.range(0, offencesCount)
                .mapToObj((int i) -> getRandomOffence())
                .collect(toList());
    }

    private Offence getRandomOffence() {
        return Offence.offence()
                .withId(UUID.randomUUID())
                .withEndDate(OFFENCE_END_DATE)
                .withStartDate(OFFENCE_START_DATE)
                .withOffenceCode(OFFENCE_CODE)
                .withWording(OFFENCE_WORDING)
                .withOffenceLegislation(OFFENCE_LEGISLATION)
                .withOffenceTitle(OFFENCE_TITLE)
                .withOffenceLegislationWelsh(LEGISLATION_WELSH)
                .withOffenceTitleWelsh(WELSH_TITLE)
                .withLaaApplnReference(getSampleLaaReference())
                .withLaidDate(LAID_DATE)
                .build();
    }

    private List<ReportingRestriction> getSampleReportingRestrictions() {
        return Arrays.asList(ReportingRestriction.reportingRestriction()
                .withId(RR_ID)
                .withJudicialResultId(RR_JUDICIAL_RESULT_ID)
                .withLabel(RR_LABEL)
                .withOrderedDate(RR_ORDERED_DATE.toString())
                .build());
    }

    private SeedingHearing getSampleSeedingHearing(){
        return SeedingHearing.seedingHearing()
                .withSeedingHearingId(SEEDING_HEARING_ID)
                .withJurisdictionType(JurisdictionType.CROWN)
                .withSittingDay(SITTING_DAY)
                .build();
    }
}
