package uk.gov.moj.cpp.listing.command.handler;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static java.util.UUID.fromString;
import static java.util.UUID.randomUUID;
import static uk.gov.justice.services.test.utils.core.enveloper.EnvelopeFactory.createEnvelope;
import static uk.gov.justice.services.test.utils.core.random.RandomGenerator.STRING;

import uk.gov.justice.listing.events.TypeOfList;
import uk.gov.justice.services.common.converter.ZonedDateTimes;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.listing.command.utils.FileUtil;
import uk.gov.moj.cpp.listing.domain.Address;
import uk.gov.moj.cpp.listing.domain.CourtApplicationParty;
import uk.gov.moj.cpp.listing.domain.BailStatus;
import uk.gov.moj.cpp.listing.domain.CaseIdentifier;
import uk.gov.moj.cpp.listing.domain.CourtApplication;
import uk.gov.moj.cpp.listing.domain.CourtApplicationPartyType;
import uk.gov.moj.cpp.listing.domain.Defendant;
import uk.gov.moj.cpp.listing.domain.HearingLanguageNeeds;
import uk.gov.moj.cpp.listing.domain.JudicialRole;
import uk.gov.moj.cpp.listing.domain.JudicialRoleType;
import uk.gov.moj.cpp.listing.domain.JurisdictionType;
import uk.gov.moj.cpp.listing.domain.ListedCase;
import uk.gov.moj.cpp.listing.domain.Offence;
import uk.gov.moj.cpp.listing.domain.Prosecutor;
import uk.gov.moj.cpp.listing.domain.StatementOfOffence;
import uk.gov.moj.cpp.listing.domain.Type;

import java.io.StringReader;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import javax.json.Json;
import javax.json.JsonReader;

public class UnscheduledListingCommandBuilder {

    public static final String LINK_ACTION_TYPE = "LINK";
    static final UUID PERSON_ID = randomUUID();
    static final UUID DEFENDANT_ID1 = randomUUID();
    static final UUID DEFENDANT_ID2 = randomUUID();
    static final UUID DEFENDANT_ID3 = randomUUID();
    static final UUID OFFENCE_ID1 = randomUUID();
    static final UUID UPDATED_OFFENCE_ID1 = randomUUID();
    static final UUID UPDATED_OFFENCE_ID2 = randomUUID();
    static final UUID UPDATED_OFFENCE_ID3 = randomUUID();
    static final UUID ADDED_OFFENCE_ID1 = randomUUID();
    static final UUID ADDED_OFFENCE_ID2 = randomUUID();
    static final UUID ADDED_OFFENCE_ID3 = randomUUID();
    static final UUID DELETED_OFFENCE_ID1 = randomUUID();
    static final UUID DELETED_OFFENCE_ID2 = randomUUID();
    static final UUID DELETED_OFFENCE_ID3 = randomUUID();
    static final UUID DELETED_OFFENCE_ID4 = randomUUID();
    static final UUID SEED_HEARING_ID_1 = randomUUID();
    static final UUID HEARING_ID_1 = randomUUID();
    static final UUID HEARING_ID_2 = randomUUID();
    static final UUID CASE_ID = randomUUID();
    static final UUID APPLICATION_ID = randomUUID();
    static final UUID ADDED_OFFENCE_CASE_ID = randomUUID();
    static final UUID UPDATED_OFFENCE_CASE_ID = randomUUID();
    static final UUID DELETED_OFEENCE_CASE_ID = randomUUID();
    static final UUID COURT_CENTRE_ID = randomUUID();
    static final String FIRST_NAME = "Test Recipe";
    static final String LAST_NAME = "Last Name";
    static final String DATE_OF_BIRTH = "1980-07-15";
    static final String PTP_TYPE = "PTP";
    static final String SENTENCE_TYPE = "Sentence";
    static final String INITIAL_START_DATE = "2018-05-30";
    static final String END_DATE = "2018-06-03";
    static final LocalDate WEEK_COMMENCING_START_DATE = LocalDate.now();
    private static final int OFFENCE_COUNT = 1;
    private static final int OFFENCE_ORDER_INDEX = 0;
    static final Integer WEEK_COMMENCING_DURATION = 1;
    static final LocalDate WEEK_COMMENCING_END_DATE = LocalDate.now().plusWeeks(WEEK_COMMENCING_DURATION);
    static final String UPDATED_START_DATE = "2018-06-01";
    static final String UPDATED_END_DATE = "2018-06-03";
    static final String UPDATED_START_TIME = "2018-06-01T11:30:00Z";
    static final String OFFENCE_START_DATE = "2018-06-01";
    static final String OFFENCE_END_DATE = "2018-06-07";
    static final String NON_SITTING_DAY = "2018-06-02";
    static final ZonedDateTime COURT_PROCEEDINGS_INITIATED = ZonedDateTime.of(2019, 1, 1, 0, 0, 0, 0, ZoneId.of("UTC"));
    static final List<LocalDate> NON_SITTING_DAYS1 = singletonList(LocalDate.parse(NON_SITTING_DAY));
    static final String NON_DEFAULT_DAY = "2018-06-04T11:00:00Z";
    static final int INITIAL_ESTIMATE_MINUTES = 640;
    static final int UPDATED_ESTIMATE_MINUTES = 720;
    static final String DEFENCE_ORGANISATION_NAME = "XYZ Organisation";
    static final UUID DEFENCE_ORGANISATION_ID = randomUUID();
    static final String URN = "urn";
    static final UUID JUDGE_ID = randomUUID();
    static final UUID COURT_ROOM_ID = randomUUID();
    static final String STATEMENT_OF_OFFENCE_TITLE = "title";
    static final String STATEMENT_OF_OFFENCE_LEGISLATION = "Legislation";
    static final String START_TIME = "10:30";
    static final String CUSTODY_TIME_LIMIT = "2017-10-05";
    static final String ADDRESS_LINE_2 = "Address line 1";
    static final String ADDRESS_LINE_3 = "Address line 1";
    static final String ADDRESS_LINE_4 = "Address line 1";
    static final String POSTCODE = "Postcode";
    static final String PERSON_TITLE = "Doctor";
    static final String PERSON_FIRST_NAME1 = "firstName";
    static final String PERSON_LAST_NAME1 = "lastName";
    static final String PERSON_DOB = "1980-06-01";
    static final String PERSON_NATIONALITY = "La La Land";
    static final String MODIFIED_DATE = "2018-01-01";
    static final String OFFENCE_WORDING = "wording";
    static final String OFFENCE_CONVICTION_DATE = "2017-10-05";
    static final String LISTING_DIRECTIONS = "wheelchair access required";
    static final LocalDate START_DATE = LocalDate.parse("2018-01-02");
    static final String REPORTING_RESTRICTIONS = "Automatic anonymity under the Sexual Offences (Amendment) Act 1992";
    static final String PROSECUTOR_DATES_TO_AVOID = "Can't do Mondays & Tuesdays";
    static final JurisdictionType JURISDICTION_TYPE = JurisdictionType.CROWN;
    static final Type HEARING_TYPE = Type.type()
            .withId(fromString("6e1bef55-7e13-4615-b3ba-8663f4438e16"))
            .withDescription("Trial")
            .build();
    static final List NON_SITTING_DAYS = Collections.EMPTY_LIST;
    static final List NON_DEFAULT_DAYS = Collections.EMPTY_LIST;
    static final String EARLIEST_START_TIME = "2012-12-12T01:02:33Z";
    static final UUID JUDICIAL_ID_1 = randomUUID();
    static final UUID JUDICIAL_ID_2 = randomUUID();
    static final String JUDICIAL_ROLE_TYPE = "MAGISTRATE";
    static final Boolean IS_DEPUTY = false;
    static final Boolean IS_BENCH_CHAIRMAN = true;
    static final UUID AUTHORITY_ID = randomUUID();
    static final String HEARING_LANGUAGE = "ENGLISH";
    static final String DEFAULT_DURATION = "6";
    static final String DEFAULT_START_TIME = "10:30";
    static final String SEQUENCE_1 = "1";
    static final String SEQUENCE_2 = "2";
    static final String HEARING_DATE_1 = "2012-12-11";
    static final String HEARING_DATE_2 = "2012-12-12";
    static final UUID COURT_APPLICATION_ID = randomUUID();
    static final String COURT_APPLICATION_TYPE = STRING.next();
    static final UUID COURT_CENTRE_ID_ONE = UUID.fromString("89592405-c29b-3706-b1d3-b1dd3a08b227");
    static final UUID COURT_CENTRE_ID_TWO = UUID.fromString("44497da7-ec8d-3137-94ad-ff7c0c57827a");
    static final String HEARING_ID = "hearingId";
    static final String PROSECUTION_CASE_ID = "prosecutionCaseId";
    static final String FIELD_APPLICATION_ID = "applicationId";
    static final String REMOVAL_REASON = "removalReason";
    static final String SITTING_DAY = "2021-02-21";
    static final TypeOfList TYPE_OF_LIST = TypeOfList.typeOfList()
            .withId(fromString("0b1e1e98-a5b2-460a-a851-17dd6f47c1a7"))
            .withDescription("Warrant for arrest without bail").build();

    private final static Optional<String> APPLICATION_PARTICULARS = of("Application particulars");
    static final UUID PROSECUTOR_ID = UUID.randomUUID();
    static final String PROSECUTOR_CODE = "CPS-SW";
    static final String PROSECUTOR_NAME = "Prosecution South West";



    static JsonEnvelope listUnscheduledCourtHearingCommandEnvelope() {
        return listUnscheduledCourtHearingCommandEnvelopeFor("/test-data/listing.command.list-unscheduled-court-hearing.json");
    }

    static JsonEnvelope listUnscheduledNextHearingCommandEnvelope() {
        return listUnscheduledNextHearingCommandEnvelopeFor("/test-data/listing.command.list-unscheduled-next-hearing.json");
    }

    static JsonEnvelope listUnscheduledNextHearingWithProsecutorCommandEnvelope() {
        return listUnscheduledNextHearingCommandEnvelopeFor("/test-data/listing.command.list-unscheduled-next-hearing-with-prosecutor.json");
    }

    static JsonEnvelope listUnscheduledCourtHearingCommandEnvelopeFor(final String testDataFileLocation) {
        final String jsonString = FileUtil.givenPayload(testDataFileLocation).toString()
                .replace("HEARING_ID", HEARING_ID_1.toString())
                .replace("OFFENCE_ID", OFFENCE_ID1.toString())
                .replace("REPORTING_RESTRICTIONS", REPORTING_RESTRICTIONS)
                .replace("PROSECUTOR_DATES_TO_AVOID", PROSECUTOR_DATES_TO_AVOID)
                .replace("JURISDICTION_TYPE", JURISDICTION_TYPE.toString())
                .replace("JUDICIAL_ID", JUDICIAL_ID_1.toString())
                .replace("PROSECUTOR_ID", PROSECUTOR_ID.toString())
                .replace("PROSECUTOR_CODE", PROSECUTOR_CODE)
                .replace("PROSECUTOR_NAME", PROSECUTOR_NAME)
                .replace("AUTHORITY_ID", AUTHORITY_ID.toString())
                .replace("CUSTODY_TIME_LIMIT", CUSTODY_TIME_LIMIT)
                .replace("DEFAULT_DURATION", DEFAULT_DURATION)
                .replace("DEFAULT_START_TIME", DEFAULT_START_TIME)
                .replaceAll("COURT_CENTRE_ID", COURT_CENTRE_ID.toString())
                .replace("COURT_ROOM_ID", COURT_ROOM_ID.toString())
                .replace("LISTING_DIRECTIONS", LISTING_DIRECTIONS)
                .replace("DATE_OF_BIRTH", DATE_OF_BIRTH)
                .replaceAll("DEFENDANT_ID", DEFENDANT_ID1.toString())
                .replaceAll("CASE_ID", CASE_ID.toString())
                .replace("EARLIEST_START_TIME", EARLIEST_START_TIME)
                .replace("WEEK_COMMENCING_START_DATE", WEEK_COMMENCING_START_DATE.toString())
                .replace("WEEK_COMMENCING_DURATION", WEEK_COMMENCING_DURATION.toString());
        try {
            final JsonReader jsonReader = Json.createReader(new StringReader(jsonString));
            return createEnvelope("listing.command.list-unscheduled-court-hearing", jsonReader.readObject());
        } catch (final Exception e) {
            throw new RuntimeException(e);
        }
    }

    static JsonEnvelope listUnscheduledNextHearingCommandEnvelopeFor(final String testDataFileLocation) {
        final String jsonString = FileUtil.givenPayload(testDataFileLocation).toString()
                .replace("HEARING_ID", HEARING_ID_1.toString())
                .replace("OFFENCE_ID", OFFENCE_ID1.toString())
                .replace("REPORTING_RESTRICTIONS", REPORTING_RESTRICTIONS)
                .replace("PROSECUTOR_DATES_TO_AVOID", PROSECUTOR_DATES_TO_AVOID)
                .replace("JURISDICTION_TYPE", JURISDICTION_TYPE.toString())
                .replace("JUDICIAL_ID", JUDICIAL_ID_1.toString())
                .replace("AUTHORITY_ID", AUTHORITY_ID.toString())
                .replace("PROSECUTOR_ID", PROSECUTOR_ID.toString())
                .replace("PROSECUTOR_CODE", PROSECUTOR_CODE)
                .replace("PROSECUTOR_NAME", PROSECUTOR_NAME)
                .replace("CUSTODY_TIME_LIMIT", CUSTODY_TIME_LIMIT)
                .replace("DEFAULT_DURATION", DEFAULT_DURATION)
                .replace("DEFAULT_START_TIME", DEFAULT_START_TIME)
                .replaceAll("COURT_CENTRE_ID", COURT_CENTRE_ID.toString())
                .replace("COURT_ROOM_ID", COURT_ROOM_ID.toString())
                .replace("LISTING_DIRECTIONS", LISTING_DIRECTIONS)
                .replace("DATE_OF_BIRTH", DATE_OF_BIRTH)
                .replaceAll("DEFENDANT_ID", DEFENDANT_ID1.toString())
                .replaceAll("CASE_ID", CASE_ID.toString())
                .replace("EARLIEST_START_TIME", EARLIEST_START_TIME)
                .replace("WEEK_COMMENCING_START_DATE", WEEK_COMMENCING_START_DATE.toString())
                .replace("WEEK_COMMENCING_DURATION", WEEK_COMMENCING_DURATION.toString());
        try {
            final JsonReader jsonReader = Json.createReader(new StringReader(jsonString));
            return createEnvelope("listing.command.list-unscheduled-next-hearing", jsonReader.readObject());
        } catch (final Exception e) {
            throw new RuntimeException(e);
        }
    }

    static JsonEnvelope listUnscheduledCourtHearingForApplicationsCommandEnvelope() {
        return listUnscheduledCourtHearingCommandEnvelopeFor("/test-data/listing.command.list-unscheduled-court-hearing-for-application.json");
    }

    static ListedCase createdListedCase() {
        return ListedCase.listedCase()
                .withId(CASE_ID)
                .withCaseIdentifier(CaseIdentifier.caseIdentifier()
                        .withAuthorityCode("TFL")
                        .withCaseReference("TFL12345")
                        .withAuthorityId(AUTHORITY_ID)
                        .build())
                .withDefendants(Arrays.asList(createDomainDefendant()))
                .build();
    }

    static ListedCase createdListedCaseWithProsecutor() {
        return ListedCase.listedCase().withValuesFrom(createdListedCase())
                .withProsecutor(Prosecutor.prosecutor()
                        .withProsecutorId(PROSECUTOR_ID)
                        .withProsecutorCode(PROSECUTOR_CODE)
                        .build()
                )
                .withDefendants(Arrays.asList(createDomainDefendant()))
                .build();
    }

    static Defendant createDomainDefendant() {
        return Defendant.defendant()
                .withBailStatus(of(new BailStatus.Builder().withCode("C").withDescription("Custody or remanded into custody").withId(UUID.fromString("12e69486-4d01-3403-a50a-7419ca040635")).build()))
                .withCustodyTimeLimit(of(CUSTODY_TIME_LIMIT))
                .withDateOfBirth(of(DATE_OF_BIRTH))
                .withDatesToAvoid(of("wednesdays"))
                .withDefenceOrganisation(Optional.empty())
                .withFirstName(of("Harry"))
                .withLastName(of("Kane Junior"))
                .withHearingLanguageNeeds(of(HearingLanguageNeeds.ENGLISH))
                .withId(DEFENDANT_ID1)
                .withMasterDefendantId(Optional.of(DEFENDANT_ID1))
                .withCourtProceedingsInitiated(Optional.of(ZonedDateTimes.fromString("2020-03-05T14:24:03.148Z").withZoneSameInstant(ZoneId.of("UTC"))))
                .withOrganisationName(Optional.empty())
                .withSpecificRequirements(of("Screen"))
                .withIsYouth(empty())
                .withOffences(Arrays.asList(Offence.offence()
                        .withId(OFFENCE_ID1)
                        .withOffenceCode("AAA")
                        .withStartDate("2018-01-01")
                        .withEndDate(of("2018-01-01"))
                        .withLaidDate(Optional.of("2019-05-01"))
                        .withOffenceWording("No Travel Card")
                        .withCount(OFFENCE_COUNT)
                        .withOrderIndex(OFFENCE_ORDER_INDEX)
                        .withLaaApplnReference(Optional.empty())
                        .withShadowListed(of(Boolean.FALSE))
                        .withSeedingHearing(empty())
                        .withStatementOfOffence(StatementOfOffence.statementOfOffence()
                                .withWelshTitle("a title in Welsh")
                                .withWelshLegislation(of("legislation in Welsh"))
                                .withLegislation(of("legislation"))
                                .withTitle("a title")
                                .build())

                        .build()))
                .build();
    }


    static CourtApplication getCourtApplication() {
        return CourtApplication.courtApplication()
                .withLinkedCaseIds(singletonList(fromString("19e9d562-6abb-4871-bfb3-2d777aa90371")))
                .withParentApplicationId(fromString("9d9a431a-0f12-4386-878a-2bf6c4a0877e"))
                .withApplicationType("App Type")
                .withId(fromString("26b856a8-ae01-4aad-814c-7cdff8db19bf"))
                .withApplicant(CourtApplicationParty.courtApplicationParty()
                        .withIsRespondent(false)
                        .withId(fromString("22b1078b-9430-4cef-ba46-eea40a129ca8"))
                        .withFirstName("Fred")
                        .withLastName("Perry")
                        .withCourtApplicationPartyType(CourtApplicationPartyType.PERSON)
                        .withAddress(getAddress())
                        .build())
                .withRespondents(Collections.singletonList(CourtApplicationParty.courtApplicationParty()
                        .withIsRespondent(true)
                        .withId(fromString("48ddbd0a-31db-4814-b052-aa3ba9afb800"))
                        .withFirstName("Dan")
                        .withLastName("Brown")
                        .withCourtApplicationPartyType(CourtApplicationPartyType.PERSON)
                        .withAddress(getAddress())
                        .build()))
                .withApplicationReference(Optional.of("REF-1"))
                .withApplicationParticulars(APPLICATION_PARTICULARS)
                .withOffences(emptyList())
                .build();
    }

    static CourtApplication getCourtApplicationForApplicationOnly() {
        return CourtApplication.courtApplication()
                .withApplicationType("Application for a sexual offences prevention order")
                .withId(fromString("bf5e2df3-0c37-490e-8b92-f2ebdd5f6723"))
                .withLinkedCaseIds(Collections.emptyList())
                .withApplicant(CourtApplicationParty.courtApplicationParty()
                        .withIsRespondent(false)
                        .withId(fromString("9f85ccfb-e57d-4339-adb2-4764f5cd1e5f"))
                        .withFirstName("Katelynn")
                        .withLastName("Nienow")
                        .withCourtApplicationPartyType(CourtApplicationPartyType.PERSON)
                        .withAddress(Address.address()
                                .withAddress1("9 Beverley Heights")
                                .withAddress2(Optional.of("Elise Forge"))
                                .withAddress3(Optional.of("London"))
                                .withPostcode(Optional.of("SA7 0AN"))
                                .build())
                        .build())
                .withRespondents(singletonList(CourtApplicationParty.courtApplicationParty()
                        .withIsRespondent(true)
                        .withId(fromString("2ada34cf-5fbf-4526-974e-feb073b032bf"))
                        .withFirstName("Renna")
                        .withLastName("Jaskolski")
                        .withCourtApplicationPartyType(CourtApplicationPartyType.PERSON)
                        .withAddress(Address.address()
                                .withAddress1("56 Gottlieb Court")
                                .withAddress2(Optional.of("Welch Islands"))
                                .withAddress3(Optional.of("Bristol"))
                                .withPostcode(Optional.of("BA9 2AS"))
                                .build())
                        .build()))
                .withApplicationReference(of("QCIUATYHQN"))
                .withApplicationParticulars(empty())
                .withOffences(emptyList())
                .build();
    }

    static Address getAddress() {
        return Address
                .address()
                .withAddress1("Address1")
                .withAddress2(of("Address1"))
                .withAddress3(of("Address1"))
                .withAddress4(of("Address1"))
                .withAddress5(of("Address1"))
                .withPostcode(of("SW13 0AA"))
                .build();
    }


    static List<JudicialRole> createJudicalRoles() {
        return singletonList(JudicialRole.judicialRole().withJudicialId(JUDICIAL_ID_1)
                .withJudicialRoleType(
                        JudicialRoleType.judicialRoleType()
                                .withJudiciaryType(JUDICIAL_ROLE_TYPE)
                                .build())
                .withIsBenchChairman(of(IS_BENCH_CHAIRMAN))
                .withIsDeputy(of(IS_DEPUTY)).build());
    }

}
