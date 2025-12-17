package uk.gov.moj.cpp.listing.query.view.courtlist;

import static java.util.Objects.nonNull;
import static java.util.UUID.randomUUID;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;

import uk.gov.justice.services.test.utils.core.random.Generator;
import uk.gov.justice.services.test.utils.core.random.StringGenerator;
import uk.gov.moj.cpp.listing.query.view.courtlist.pojo.FlatHearing;
import uk.gov.moj.cpp.listing.query.view.courtlist.pojo.Hearing;
import uk.gov.moj.cpp.listing.query.view.courtlist.pojo.Sitting;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

import org.junit.jupiter.api.Test;

public class SittingsPojoBuilderTest {

    public static final Generator<String> STRING = new StringGenerator();


    @Test
    public void shouldPutHearingsInTheSameSittingIfTheyHaveTheSameSittingDetails() {


        final UUID courtRoomId = randomUUID();
        final UUID judicialId = randomUUID();
        final LocalDate startDate = LocalDate.parse("2019-12-11");
        final String endDate = "2019-12-11";

        final FlatHearing flatHearing1 = buildFlatHearing("2019-12-11", courtRoomId, judicialId);
        final FlatHearing flatHearing2 = buildFlatHearing("2019-12-11", courtRoomId, judicialId);

        final List<FlatHearing> flatHearings = Arrays.asList(flatHearing1, flatHearing2);

        final List<Sitting> sittingsMap = SittingsPojoBuilder.assignFlatHearingsToSittings(flatHearings, startDate, endDate);

        assertThat(sittingsMap.size(), is(1));
    }

    @Test
    public void shouldPutHearingsInSeparateSittingsIfInDifferentCourtRooms() {

        final UUID judicialId = randomUUID();
        final LocalDate startDate = LocalDate.parse("2019-12-11");
        final String endDate = "2019-12-11";

        final FlatHearing flatHearing1 = buildFlatHearing("2019-12-11", randomUUID(), judicialId);
        final FlatHearing flatHearing2 = buildFlatHearing("2019-12-11", randomUUID(), judicialId);

        final List<FlatHearing> flatHearings = Arrays.asList(flatHearing1, flatHearing2);

        final List<Sitting> sittingsMap = SittingsPojoBuilder.assignFlatHearingsToSittings(flatHearings, startDate, endDate);

        assertThat(sittingsMap.size(), is(2));
    }

    @Test
    public void shouldPutHearingsInTheSameSittingIfTheyHaveTheSameSittingDetailsWithVideoLinkDetails() {

        final UUID courtRoomId = randomUUID();
        final UUID judicialId = randomUUID();
        final LocalDate startDate = LocalDate.parse("2019-12-11");
        final String endDate = "2019-12-11";

        final FlatHearing flatHearing1 = buildFlatHearingWithVideoLinkDetails("2019-12-11", courtRoomId, judicialId, true, "Pre Conf");
        final FlatHearing flatHearing2 = buildFlatHearingWithVideoLinkDetails("2019-12-11", courtRoomId, judicialId, true, "Test Conf");

        final List<FlatHearing> flatHearings = Arrays.asList(flatHearing1, flatHearing2);

        final List<Sitting> sittingsMap = SittingsPojoBuilder.assignFlatHearingsToSittings(flatHearings, startDate, endDate);

        assertThat(sittingsMap.size(), is(1));
        assertThat(sittingsMap.get(0).getHearings().get(0).hasVideoLink(), is(true));
        assertThat(sittingsMap.get(0).getHearings().get(0).getPublicListNote(), is("Pre Conf"));

        assertThat(sittingsMap.get(0).getHearings().get(1).hasVideoLink(), is(true));
        assertThat(sittingsMap.get(0).getHearings().get(1).getPublicListNote(), is("Test Conf"));
    }


    @Test
    public void shouldPutHearingsInTheSameSittingIfTheyHaveTheSameSittingDetailsWithOneHasVideoLinkDetails() {

        final UUID courtRoomId = randomUUID();
        final UUID judicialId = randomUUID();
        final LocalDate startDate = LocalDate.parse("2023-09-01");
        final String endDate = "2023-09-01";

        final FlatHearing flatHearing1 = buildFlatHearingWithVideoLinkDetails("2023-09-01", courtRoomId, judicialId, true, "Pre Conf");
        final FlatHearing flatHearing2 = buildFlatHearingWithVideoLinkDetails("2023-09-01", courtRoomId, judicialId, true, null);


        final List<FlatHearing> flatHearings = Arrays.asList(flatHearing1, flatHearing2);

        final List<Sitting> sittingsMap = SittingsPojoBuilder.assignFlatHearingsToSittings(flatHearings, startDate, endDate);

        assertThat(sittingsMap.size(), is(1));
        assertThat(sittingsMap.get(0).getHearings().get(0).hasVideoLink(), is(true));
        assertThat(sittingsMap.get(0).getHearings().get(0).getPublicListNote(), is("Pre Conf"));

        assertThat(sittingsMap.get(0).getHearings().get(1).hasVideoLink(), is(true));
        assertThat(sittingsMap.get(0).getHearings().get(1).getPublicListNote(), nullValue());
    }

    @Test
    public void shouldPutHearingsInTheSameSittingWithMultipleHearingIfTheyHaveTheSameSittingDetailsAndHAveMultipleCaseWithSameHearing() {

        final UUID courtRoomId = randomUUID();
        final UUID judicialId = randomUUID();
        final LocalDate startDate = LocalDate.parse("2023-09-01");
        final String endDate = "2023-09-01";

        final FlatHearing flatHearing1 = buildFlatHearingWithVideoLinkDetails("2023-09-01", courtRoomId, judicialId, true, "Pre Conf");
        final FlatHearing flatHearing2 = buildFlatHearingWithVideoLinkDetails("2023-09-01", courtRoomId, judicialId, true, null);


        final String hearingStartTime = "2023-09-01T11:00:00.000Z";
        final String hearingEndTime = "2020-09-01T12:00:00.000Z";
        final String defendant1FirstName = STRING.next();
        final String defendant1LastName = STRING.next();
        final String case1Reference = STRING.next();
        final String defendant2FirstName = STRING.next();
        final String defendant21LastName = STRING.next();
        final String case2Reference = STRING.next();


        final FlatHearing flatHearing3 = buildFlatHearingWithHearingDaysAndMultipleCases("2023-09-01", hearingStartTime, hearingEndTime, courtRoomId, judicialId
                , defendant1FirstName, defendant1LastName, case1Reference, defendant2FirstName, defendant21LastName, case2Reference);


        final List<FlatHearing> flatHearings = Arrays.asList(flatHearing1, flatHearing2, flatHearing3);

        final List<Sitting> sittingsMap = SittingsPojoBuilder.assignFlatHearingsToSittings(flatHearings, startDate, endDate);

        assertThat(sittingsMap.size(), is(1));
        assertThat(sittingsMap.get(0).getHearings().size(), is(4));
        assertThat(sittingsMap.get(0).getHearings().get(0).hasVideoLink(), is(true));
        assertThat(sittingsMap.get(0).getHearings().get(0).getPublicListNote(), is("Pre Conf"));

        assertThat(sittingsMap.get(0).getHearings().get(1).hasVideoLink(), is(true));
        assertThat(sittingsMap.get(0).getHearings().get(1).getPublicListNote(), nullValue());

        assertThat(sittingsMap.get(0).getHearings().get(2).getCaseDetails().get().getCaseIdentifier().getString("caseReference"), is(case1Reference));
        assertThat(sittingsMap.get(0).getHearings().get(2).getCaseDetails().get().getDefendants().getJsonObject(0).getString("firstName"), is(defendant1FirstName));
        assertThat(sittingsMap.get(0).getHearings().get(2).getCaseDetails().get().getDefendants().getJsonObject(0).getString("lastName"), is(defendant1LastName));

        assertThat(sittingsMap.get(0).getHearings().get(3).getCaseDetails().get().getCaseIdentifier().getString("caseReference"), is(case2Reference));
        assertThat(sittingsMap.get(0).getHearings().get(3).getCaseDetails().get().getDefendants().getJsonObject(0).getString("firstName"), is(defendant2FirstName));
        assertThat(sittingsMap.get(0).getHearings().get(3).getCaseDetails().get().getDefendants().getJsonObject(0).getString("lastName"), is(defendant21LastName));
    }

    @Test
    public void shouldGetHearingStartTimeFromHearingDayIfWeekCommencingStartDateIsEmpty() {

        final UUID courtRoomId = randomUUID();
        final UUID judicialId = randomUUID();
        final LocalDate startDate = LocalDate.parse("2020-10-05");
        final String endDate = "2020-10-05";
        final String hearingStartTime = "2020-10-05T11:00:00.000Z";
        final String hearingEndTime = "2020-10-05T12:00:00.000Z";
        final LocalDateTime expectedHearingStartTime = ZonedDateTime.parse(hearingStartTime).toLocalDateTime();
        final LocalDateTime expectedHearingEndTime = ZonedDateTime.parse(hearingEndTime).toLocalDateTime();

        final FlatHearing flatHearing = buildFlatHearingWithHearingDays("2020-10-05", hearingStartTime, hearingEndTime, courtRoomId, judicialId);

        final List<FlatHearing> flatHearings = Arrays.asList(flatHearing);

        final List<Sitting> sittingsMap = SittingsPojoBuilder.assignFlatHearingsToSittings(flatHearings, startDate, endDate);

        assertThat(sittingsMap.size(), is(1));
        assertThat(sittingsMap.get(0).getHearings().size(), is(1));
        final Hearing hearing = sittingsMap.get(0).getHearings().get(0);
        assertThat(hearing.getStartTime(), is(expectedHearingStartTime));
        assertThat(hearing.getEndTime().get(), is(expectedHearingEndTime));
    }

    @Test
    public void shouldCreateAsManyHearingAsCasesIfMultipleCasesArePresentInSameHearing() {
        final UUID courtRoomId = randomUUID();
        final UUID judicialId = randomUUID();
        final LocalDate startDate = LocalDate.parse("2023-09-01");
        final String endDate = "2023-09-01";
        final String hearingStartTime = "2023-09-01T11:00:00.000Z";
        final String hearingEndTime = "2020-09-01T12:00:00.000Z";
        final String defendant1FirstName = STRING.next();
        final String defendant1LastName = STRING.next();
        final String case1Reference = STRING.next();
        final String defendant2FirstName = STRING.next();
        final String defendant21LastName = STRING.next();
        final String case2Reference = STRING.next();


        final FlatHearing flatHearing = buildFlatHearingWithHearingDaysAndMultipleCases("2023-09-01", hearingStartTime, hearingEndTime, courtRoomId, judicialId
                , defendant1FirstName, defendant1LastName, case1Reference, defendant2FirstName, defendant21LastName, case2Reference);

        final List<FlatHearing> flatHearings = Arrays.asList(flatHearing);

        final List<Sitting> sittingsMap = SittingsPojoBuilder.assignFlatHearingsToSittings(flatHearings, startDate, endDate);

        assertThat(sittingsMap.size(), is(1));
        assertThat(sittingsMap.get(0).getHearings().size(), is(2));

        assertThat(sittingsMap.get(0).getHearings().get(0).getCaseDetails().get().getCaseIdentifier().getString("caseReference"), is(case1Reference));
        assertThat(sittingsMap.get(0).getHearings().get(0).getCaseDetails().get().getDefendants().getJsonObject(0).getString("firstName"), is(defendant1FirstName));
        assertThat(sittingsMap.get(0).getHearings().get(0).getCaseDetails().get().getDefendants().getJsonObject(0).getString("lastName"), is(defendant1LastName));

        assertThat(sittingsMap.get(0).getHearings().get(1).getCaseDetails().get().getCaseIdentifier().getString("caseReference"), is(case2Reference));
        assertThat(sittingsMap.get(0).getHearings().get(1).getCaseDetails().get().getDefendants().getJsonObject(0).getString("firstName"), is(defendant2FirstName));
        assertThat(sittingsMap.get(0).getHearings().get(1).getCaseDetails().get().getDefendants().getJsonObject(0).getString("lastName"), is(defendant21LastName));

    }

    @Test
    public void shouldCreateAsManyHearingsAsApplicationsIfMultipleApplicationsArePresentInTheSameHearing() {

        final UUID courtRoomId = randomUUID();
        final UUID judicialId = randomUUID();
        final LocalDate startDate = LocalDate.parse("2023-09-01");
        final String endDate = "2023-09-01";
        final String hearingStartTime = "2023-09-01T11:00:00.000Z";
        final String hearingEndTime = "2020-09-01T12:00:00.000Z";
        final String application1Reference = STRING.next();
        final String application2Reference = STRING.next();


        final FlatHearing flatHearing = buuildFlatHearingWithHearingDaysAndMultipleApplications("2023-09-01", hearingStartTime, hearingEndTime, courtRoomId, judicialId, application1Reference, application2Reference);
        final List<FlatHearing> flatHearings = Arrays.asList(flatHearing);
        final List<Sitting> sittingsMap = SittingsPojoBuilder.assignFlatHearingsToSittings(flatHearings, startDate, endDate);

        assertThat(sittingsMap.size(), is(1));
        assertThat(sittingsMap.get(0).getHearings().size(), is(2));
        assertThat(sittingsMap.get(0).getHearings().get(0).getCourtApplicationDetails().get().getApplicationReference(), is(application1Reference));
        assertThat(sittingsMap.get(0).getHearings().get(1).getCourtApplicationDetails().get().getApplicationReference(), is(application2Reference));

        // Verify subject is not set when subject is not present in JSON
        assertThat(sittingsMap.get(0).getHearings().get(0).getCourtApplicationDetails().get().getSubject(), nullValue());
        assertThat(sittingsMap.get(0).getHearings().get(1).getCourtApplicationDetails().get().getSubject(), nullValue());

    }

    @Test
    public void shouldSetSubjectToProvidedValueWhenSubjectIsExplicitlyProvidedInJson() {

        final UUID courtRoomId = randomUUID();
        final UUID judicialId = randomUUID();
        final LocalDate startDate = LocalDate.parse("2023-09-01");
        final String endDate = "2023-09-01";
        final String hearingStartTime = "2023-09-01T11:00:00.000Z";
        final String hearingEndTime = "2020-09-01T12:00:00.000Z";
        final String applicationReference = STRING.next();

        final JsonObject applicant = Json.createObjectBuilder()
                .add("id", randomUUID().toString())
                .add("lastName", "Applicant")
                .add("isRespondent", false)
                .add("restrictFromCourtList", false)
                .add("courtApplicationPartyType", "PERSON")
                .build();

        final JsonObject subject = Json.createObjectBuilder()
                .add("id", randomUUID().toString())
                .add("lastName", "Subject")
                .add("isRespondent", false)
                .add("restrictFromCourtList", false)
                .add("courtApplicationPartyType", "PERSON")
                .build();

        final JsonObject caseHearings = buildCaseHearingsWithHearingDaysAndApplicationWithSubject("2023-09-01", hearingStartTime, hearingEndTime, courtRoomId, judicialId, applicationReference, applicant, subject);
        final FlatHearing flatHearing = new FlatHearing(LocalDate.parse("2023-09-01"), caseHearings.getJsonArray("judiciary"),
                Optional.of(courtRoomId), caseHearings, false);
        final List<FlatHearing> flatHearings = Arrays.asList(flatHearing);
        final List<Sitting> sittingsMap = SittingsPojoBuilder.assignFlatHearingsToSittings(flatHearings, startDate, endDate);

        assertThat(sittingsMap.size(), is(1));
        assertThat(sittingsMap.get(0).getHearings().size(), is(1));
        assertThat(sittingsMap.get(0).getHearings().get(0).getCourtApplicationDetails().get().getApplicationReference(), is(applicationReference));

        // Verify subject is set to the explicitly provided subject value, not applicant
        assertThat(sittingsMap.get(0).getHearings().get(0).getCourtApplicationDetails().get().getSubject().getString("lastName"), is("Subject"));
        assertThat(sittingsMap.get(0).getHearings().get(0).getCourtApplicationDetails().get().getApplicant().getString("lastName"), is("Applicant"));
        assertThat(sittingsMap.get(0).getHearings().get(0).getCourtApplicationDetails().get().getSubject(), is(subject));

    }

    private FlatHearing buildFlatHearing(final String startDate, final UUID courtRoomId, final UUID judicialId) {

        final JsonObject caseHearings = buildWeekCommencingCaseHearings(startDate, courtRoomId, judicialId);

        return new FlatHearing(LocalDate.parse(startDate), caseHearings.getJsonArray("judiciary"),
                Optional.of(courtRoomId), caseHearings, true);
    }

    private FlatHearing buildFlatHearingWithHearingDays(final String startDate,
                                                        final String hearingStartTime,
                                                        final String hearingEndTime,
                                                        final UUID courtRoomId,
                                                        final UUID judicialId) {

        final JsonObject caseHearings = buildCaseHearingsWithHearingDays(startDate, hearingStartTime, hearingEndTime, courtRoomId, judicialId);

        return new FlatHearing(LocalDate.parse(startDate), caseHearings.getJsonArray("judiciary"),
                Optional.of(courtRoomId), caseHearings, false);
    }

    private FlatHearing buildFlatHearingWithHearingDaysAndMultipleCases(final String startDate,
                                                                        final String hearingStartTime,
                                                                        final String hearingEndTime,
                                                                        final UUID courtRoomId,
                                                                        final UUID judicialId, final String defendant1FirstName, final String defendant1lastName, final String case1Reference,
                                                                        final String defendant2FirstName, final String defendant2LastName, final String case2Reference) {

        final JsonObject caseHearings = buildCaseHearingsWithHearingDaysAndMultipleCases(startDate, hearingStartTime, hearingEndTime, courtRoomId, judicialId
                , defendant1FirstName, defendant1lastName, case1Reference,
                defendant2FirstName, defendant2LastName, case2Reference);
        return new FlatHearing(LocalDate.parse(startDate), caseHearings.getJsonArray("judiciary"),
                Optional.of(courtRoomId), caseHearings, false);

    }

    private FlatHearing buuildFlatHearingWithHearingDaysAndMultipleApplications(final String startDate,
                                                                                final String hearingStartTime,
                                                                                final String hearingEndTime,
                                                                                final UUID courtRoomId,
                                                                                final UUID judicialId,
                                                                                final String applicationRefence1,
                                                                                final String applicationRefence2) {

        final JsonObject caseHearings = buildCaseHearingsWithHearingDaysAndMultipleApplications(startDate, hearingStartTime, hearingEndTime, courtRoomId, judicialId, applicationRefence1, applicationRefence2);
        return new FlatHearing(LocalDate.parse(startDate), caseHearings.getJsonArray("judiciary"),
                Optional.of(courtRoomId), caseHearings, false);

    }

    private FlatHearing buildFlatHearingWithVideoLinkDetails(final String startDate, final UUID courtRoomId, final UUID judicialId, final boolean hasVideoLink, final String publicListNote) {

        final JsonObject caseHearings = buildWeekCommencingCaseHearingsWithVideoLink(startDate, courtRoomId, judicialId, hasVideoLink, publicListNote);

        return new FlatHearing(LocalDate.parse(startDate), caseHearings.getJsonArray("judiciary"),
                Optional.of(courtRoomId), caseHearings, true);
    }

    private JsonObject buildCaseHearingsWithHearingDays(final String hearingDate, final String hearingStartTime, final String hearingEndTime, final UUID courtRoomId, final UUID judicialId) {
        return Json.createObjectBuilder()
                .add("courtRoomId", courtRoomId.toString())
                .add("judiciary", Json.createArrayBuilder().add(
                        Json.createObjectBuilder().add("judicialId", judicialId.toString()))
                )
                .add("listedCases", Json.createArrayBuilder()
                        .add(buildCaseDetailsJson()))
                .add("nonDefaultDays", Json.createArrayBuilder()
                        .add(buildNonDefaultDaysJson()))
                .add("hearingDays", Json.createArrayBuilder()
                        .add(buildHearingDaysJson(hearingDate, hearingStartTime, hearingEndTime)))
                .build();
    }

    private JsonObject buildCaseHearingsWithHearingDaysAndMultipleCases(final String hearingDate, final String hearingStartTime,
                                                                        final String hearingEndTime, final UUID courtRoomId, final UUID judicialId,
                                                                        final String defendant1FirstName, final String defendant1lastName, final String case1Reference,
                                                                        final String defendant2FirstName, final String defendant2LastName, final String case2Reference) {
        return Json.createObjectBuilder()
                .add("courtRoomId", courtRoomId.toString())
                .add("judiciary", Json.createArrayBuilder().add(
                        Json.createObjectBuilder().add("judicialId", judicialId.toString()))
                )
                .add("listedCases", Json.createArrayBuilder()
                        .add(buildCaseDetailsJsonWithDefendant(defendant1FirstName, defendant1lastName, case1Reference))
                        .add(buildCaseDetailsJsonWithDefendant(defendant2FirstName, defendant2LastName, case2Reference)))
                .add("nonDefaultDays", Json.createArrayBuilder()
                        .add(buildNonDefaultDaysJson()))
                .add("hearingDays", Json.createArrayBuilder()
                        .add(buildHearingDaysJson(hearingDate, hearingStartTime, hearingEndTime)))
                .build();
    }

    private JsonObject buildCaseHearingsWithHearingDaysAndMultipleApplications(final String hearingDate, final String hearingStartTime,
                                                                               final String hearingEndTime, final UUID courtRoomId, final UUID judicialId,
                                                                               final String applicationReference1, final String applicationReference2) {

        return Json.createObjectBuilder()
                .add("courtRoomId", courtRoomId.toString())
                .add("judiciary", Json.createArrayBuilder().add(
                        Json.createObjectBuilder().add("judicialId", judicialId.toString()))
                )
                .add("courtApplications", Json.createArrayBuilder()
                        .add(buildApplicationJson(applicationReference1))
                        .add(buildApplicationJson(applicationReference2)))
                .add("nonDefaultDays", Json.createArrayBuilder()
                        .add(buildNonDefaultDaysJson()))
                .add("hearingDays", Json.createArrayBuilder()
                        .add(buildHearingDaysJson(hearingDate, hearingStartTime, hearingEndTime)))
                .build();

    }

    private JsonObject buildCaseHearingsWithHearingDaysAndApplicationWithSubject(final String hearingDate, final String hearingStartTime,
                                                                                final String hearingEndTime, final UUID courtRoomId, final UUID judicialId,
                                                                                final String applicationReference, final JsonObject applicant, final JsonObject subject) {

        final JsonArrayBuilder respondentsArrayBuilder = Json.createArrayBuilder()
                .add(Json.createObjectBuilder()
                        .add("id", randomUUID().toString())
                        .add("lastName", "TestRespondent")
                        .add("isRespondent", true)
                        .add("restrictFromCourtList", false)
                        .add("courtApplicationPartyType", "PERSON"));

        final JsonObjectBuilder applicationJson = Json.createObjectBuilder()
                .add("applicationReference", applicationReference)
                .add("restrictFromCourtList", false)
                .add("applicant", applicant)
                .add("respondents", respondentsArrayBuilder)
                .add("subject", subject);

        return Json.createObjectBuilder()
                .add("courtRoomId", courtRoomId.toString())
                .add("judiciary", Json.createArrayBuilder().add(
                        Json.createObjectBuilder().add("judicialId", judicialId.toString()))
                )
                .add("courtApplications", Json.createArrayBuilder()
                        .add(applicationJson))
                .add("nonDefaultDays", Json.createArrayBuilder()
                        .add(buildNonDefaultDaysJson()))
                .add("hearingDays", Json.createArrayBuilder()
                        .add(buildHearingDaysJson(hearingDate, hearingStartTime, hearingEndTime)))
                .build();

    }

    private JsonObject buildWeekCommencingCaseHearings(final String startDate, final UUID courtRoomId, final UUID judicialId) {
        return Json.createObjectBuilder()
                .add("weekCommencingStartDate", startDate)
                .add("weekCommencingEndDate", "2019-12-15")
                .add("courtRoomId", courtRoomId.toString())
                .add("judiciary", Json.createArrayBuilder().add(
                        Json.createObjectBuilder().add("judicialId", judicialId.toString()))
                )
                .add("listedCases", Json.createArrayBuilder()
                        .add(buildCaseDetailsJson()))
                .add("nonDefaultDays", Json.createArrayBuilder()
                        .add(buildNonDefaultDaysJson()))
                .build();
    }

    private JsonObject buildWeekCommencingCaseHearingsWithVideoLink(final String startDate, final UUID courtRoomId, final UUID judicialId, final boolean hasVideoLink, final String publicListNote) {

        final JsonObjectBuilder hearing = Json.createObjectBuilder()
                .add("weekCommencingStartDate", startDate)
                .add("weekCommencingEndDate", "2019-12-15")
                .add("courtRoomId", courtRoomId.toString())
                .add("judiciary", Json.createArrayBuilder().add(
                        Json.createObjectBuilder().add("judicialId", judicialId.toString()))
                )
                .add("listedCases", Json.createArrayBuilder()
                        .add(buildCaseDetailsJson()))
                .add("nonDefaultDays", Json.createArrayBuilder()
                        .add(buildNonDefaultDaysJson()))
                .add("hasVideoLink", hasVideoLink);
        if (nonNull(publicListNote)) {
            hearing.add("publicListNote", publicListNote);

        }
        return hearing.build();
    }

    private JsonObjectBuilder buildCaseDetailsJson() {
        final JsonObjectBuilder caseDetailsJson = Json.createObjectBuilder();

        caseDetailsJson.add("restrictFromCourtList", false);

        return caseDetailsJson;
    }

    private JsonObjectBuilder buildCaseDetailsJsonWithDefendant(final String defendantFirstName, final String defendantLastName, final String caseReference) {
        final JsonObjectBuilder caseDetailsJson = Json.createObjectBuilder();
        final JsonObjectBuilder caseIdentifierJson = Json.createObjectBuilder();
        final JsonArrayBuilder defendantsArrayBuilder = Json.createArrayBuilder();
        final JsonObjectBuilder defendantJson = Json.createObjectBuilder();
        caseDetailsJson.add("restrictFromCourtList", false);
        caseIdentifierJson.add("caseReference", caseReference);
        caseDetailsJson.add("caseIdentifier", caseIdentifierJson);
        defendantJson.add("firstName", defendantFirstName);
        defendantJson.add("lastName", defendantLastName);
        defendantsArrayBuilder.add(defendantJson);
        caseDetailsJson.add("defendants", defendantsArrayBuilder);

        return caseDetailsJson;
    }

    private JsonObjectBuilder buildApplicationJson(final String applicationReferene) {

        final JsonObjectBuilder applicantJson = Json.createObjectBuilder()
                .add("id", randomUUID().toString())
                .add("lastName", "TestApplicant")
                .add("isRespondent", false)
                .add("restrictFromCourtList", false)
                .add("courtApplicationPartyType", "PERSON");

        final JsonArrayBuilder respondentsArrayBuilder = Json.createArrayBuilder()
                .add(Json.createObjectBuilder()
                        .add("id", randomUUID().toString())
                        .add("lastName", "TestRespondent")
                        .add("isRespondent", true)
                        .add("restrictFromCourtList", false)
                        .add("courtApplicationPartyType", "PERSON"));

        final JsonObjectBuilder applicationDetailsJson = Json.createObjectBuilder();
        applicationDetailsJson.add("applicationReference", applicationReferene);
        applicationDetailsJson.add("restrictFromCourtList", false);
        applicationDetailsJson.add("applicant", applicantJson);
        applicationDetailsJson.add("respondents", respondentsArrayBuilder);

        return applicationDetailsJson;


    }


    private JsonObjectBuilder buildNonDefaultDaysJson() {
        final JsonObjectBuilder nonDefaultDaysJson = Json.createObjectBuilder();

        nonDefaultDaysJson.add("startTime", "2019-12-17T09:57:18.807Z");

        return nonDefaultDaysJson;
    }

    private JsonObjectBuilder buildHearingDaysJson(final String hearingDate, final String hearingStartTime, final String hearingEndTime) {
        final JsonObjectBuilder hearingDaysJson = Json.createObjectBuilder();

        hearingDaysJson.add("startTime", "2020-10-05T11:00:00.000Z");
        hearingDaysJson.add("endTime", "2020-10-05T12:00:00.000Z");
        hearingDaysJson.add("hearingDate", hearingDate);

        return hearingDaysJson;
    }

}
