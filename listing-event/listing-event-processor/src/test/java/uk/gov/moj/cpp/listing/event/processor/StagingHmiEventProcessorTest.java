package uk.gov.moj.cpp.listing.event.processor;

import static java.util.UUID.fromString;
import static java.util.UUID.randomUUID;
import static javax.json.Json.createArrayBuilder;
import static javax.json.Json.createObjectBuilder;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUIDAndName;
import static uk.gov.moj.cpp.listing.event.utils.FileUtil.getPayload;

import uk.gov.justice.core.courts.CourtCentre;
import uk.gov.justice.core.courts.HearingLanguage;
import uk.gov.justice.core.courts.JudicialRole;
import uk.gov.justice.core.courts.JudicialRoleType;
import uk.gov.justice.core.courts.JurisdictionType;
import uk.gov.justice.listing.commands.CourtCentreDetails;
import uk.gov.justice.listing.commands.UpdateHearingForListing;
import uk.gov.justice.listing.events.ApplicantRespondent;
import uk.gov.justice.listing.events.CaseIdentifier;
import uk.gov.justice.listing.events.CourtApplication;
import uk.gov.justice.listing.events.Defendant;
import uk.gov.justice.listing.events.DeletedHearingInStagingHmi;
import uk.gov.justice.listing.events.Hearing;
import uk.gov.justice.listing.events.HearingDay;
import uk.gov.justice.listing.events.ListedCase;
import uk.gov.justice.listing.events.Offence;
import uk.gov.justice.listing.events.Prosecutor;
import uk.gov.justice.listing.events.ReportingRestriction;
import uk.gov.justice.listing.events.RequestedHearingFromStagingHmi;
import uk.gov.justice.listing.events.StatementOfOffence;
import uk.gov.justice.listing.events.Type;
import uk.gov.justice.listing.events.UpdatedHearingInStagingHmi;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.ObjectToJsonValueConverter;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.test.utils.framework.api.JsonObjectConvertersFactory;
import uk.gov.justice.staginghmi.courts.HearingUpdatedFromHmi;
import uk.gov.justice.staginghmi.courts.NonDefaultDays;
import uk.gov.moj.cpp.listing.event.processor.courtcenter.CourtCentreFactory;
import uk.gov.moj.cpp.listing.event.processor.service.HearingService;
import uk.gov.moj.cpp.listing.event.processor.util.HearingListedToUpdateHearingForListingCommand;
import uk.gov.moj.cpp.listing.event.processor.util.HearingObjectsListingToCoreConverter;
import uk.gov.moj.cpp.staginghmi.common.StagingHmiService;

import java.io.StringReader;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import javax.json.Json;
import javax.json.JsonObject;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class StagingHmiEventProcessorTest {

    private static final String PUBLIC_REQUEST_HEARING_FROM_STAGING_HMI = "public.listing.requested-hearing-from-staging-hmi";

    private static final String PUBLIC_UPDATE_HEARING_IN_STAGING_HMI = "public.listing.updated-hearing-in-staging-hmi";

    @InjectMocks
    private StagingHmiEventProcessor stagingHmiEventProcessor;

    @Mock
    private Sender sender;

    private JsonEnvelope envelope;

    @Mock
    private JsonObjectToObjectConverter jsonObjectConverter;

    @Mock
    private HearingConfirmedFactory hearingConfirmedFactory;

    @Mock
    private StagingHmiService stagingHmiService;

    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();

    @Spy
    private HearingObjectsListingToCoreConverter hearingObjectsListingToCoreConverter;

    @Spy
    private final ObjectToJsonValueConverter objectToJsonValueConverter = new JsonObjectConvertersFactory().objectToJsonValueConverter();

    @Mock
    private CourtCentreFactory courtCentreFactory;

    @Mock
    private HearingService hearingService;

    @Captor
    private ArgumentCaptor<JsonEnvelope> senderJsonEnvelopeCaptor;

    @Spy
    private HearingListedToUpdateHearingForListingCommand hearingListedToUpdateHearingForListingCommand;

    @Test
    public void handlesRequestHearingFromStagingHmi() {
        final UUID hearingId = randomUUID();
        final UUID courtCentreId = randomUUID();
        final UUID courtRoomId = randomUUID();

        final JsonObject payLoad = createObjectBuilder()
                .build();
        envelope = JsonEnvelope.envelopeFrom(metadataWithRandomUUIDAndName(), payLoad);

        when(jsonObjectConverter.convert(payLoad, RequestedHearingFromStagingHmi.class)).thenReturn(getRequestedHearingFromStagingHmi(hearingId, courtCentreId, courtRoomId));
        when(hearingConfirmedFactory.buildCourtCentreWithAdmin(courtCentreId, courtRoomId, envelope)).thenReturn(CourtCentre.courtCentre().withCode("abc").build());
        when(stagingHmiService.isHmiListingEnabled(any())).thenReturn(true);
        stagingHmiEventProcessor.requestHearingFromStagingHmi(envelope);

        verify(sender).send(senderJsonEnvelopeCaptor.capture());
        assertThat(senderJsonEnvelopeCaptor.getValue().metadata().name(), is(PUBLIC_REQUEST_HEARING_FROM_STAGING_HMI));

        verifyPublicEventPayload(hearingId, courtCentreId);
    }

    @Test
    public void handlesUpdateHearingInStagingHmi() {
        final UUID hearingId = randomUUID();
        final UUID courtCentreId = randomUUID();
        final UUID courtRoomId = randomUUID();

        final JsonObject payLoad = createObjectBuilder()
                .build();
        envelope = JsonEnvelope.envelopeFrom(metadataWithRandomUUIDAndName(), payLoad);

        when(jsonObjectConverter.convert(payLoad, UpdatedHearingInStagingHmi.class)).thenReturn(getUpdatedExistingHearingRequested(hearingId, courtCentreId, courtRoomId));
        when(hearingConfirmedFactory.buildCourtCentreWithAdmin(courtCentreId, courtRoomId, envelope)).thenReturn(CourtCentre.courtCentre().withCode("abc").build());
        when(stagingHmiService.isHmiListingEnabled(any())).thenReturn(true);
        stagingHmiEventProcessor.updateHearingFromStagingHmi(envelope);

        verify(sender).send(senderJsonEnvelopeCaptor.capture());
        assertThat(senderJsonEnvelopeCaptor.getValue().metadata().name(), is(PUBLIC_UPDATE_HEARING_IN_STAGING_HMI));

        verifyPublicEventPayload(hearingId, courtCentreId);
    }

    private void verifyPublicEventPayload(final UUID hearingId, final UUID courtCentreId) {
        final String expectedPayload = getPayload("test-data/public.listing.requested-hearing-from-staging-hmi.json")
                .replaceAll("HEARING_ID", hearingId.toString())
                .replaceAll("COURT_CENTER_ID", courtCentreId.toString());
        assertThat(senderJsonEnvelopeCaptor.getValue().payload(), is(toJsonObject(expectedPayload)));
    }

    @Test
    public void handlesDeleteHearingInStagingHmi() {
        final UUID hearingId = randomUUID();
        final UUID courtCentreId = randomUUID();
        final UUID courtRoomId = randomUUID();

        final JsonObject payLoad = createObjectBuilder()
                .build();
        envelope = JsonEnvelope.envelopeFrom(metadataWithRandomUUIDAndName(), payLoad);

        when(jsonObjectConverter.convert(payLoad, DeletedHearingInStagingHmi.class)).thenReturn(getDeleteExistingHearingRequested(hearingId, courtCentreId, courtRoomId));
        when(hearingConfirmedFactory.buildCourtCentreWithAdmin(courtCentreId, courtRoomId, envelope)).thenReturn(CourtCentre.courtCentre().withCode("abc").build());
        when(stagingHmiService.isHmiListingEnabled(any())).thenReturn(true);
        stagingHmiEventProcessor.deletedHearingFromStagingHmi(envelope);

        verify(sender).send(senderJsonEnvelopeCaptor.capture());
        assertThat(senderJsonEnvelopeCaptor.getValue().metadata().name(), is("public.listing.deleted-hearing-in-staging-hmi"));
        assertThat(senderJsonEnvelopeCaptor.getValue().payloadAsJsonObject().keySet().size(), is(3));
        assertThat(senderJsonEnvelopeCaptor.getValue().payloadAsJsonObject().getString("hearingId"), is(hearingId.toString()));
        assertThat(senderJsonEnvelopeCaptor.getValue().payloadAsJsonObject().getString("cancellationReasonCode"), is("ABC"));
        assertThat(senderJsonEnvelopeCaptor.getValue().payloadAsJsonObject().getJsonArray("caseAndApplicationIds").getString(0), is("1"));
        assertThat(senderJsonEnvelopeCaptor.getValue().payloadAsJsonObject().getJsonArray("caseAndApplicationIds").getString(1), is("2"));
    }


    @Test
    public void handleUpdateHearingFromHmi(){
        final UUID courtCenterId = randomUUID();
        final UUID courtRoomId = randomUUID();
        final UUID hearingId = randomUUID();

        final JsonObject payLoad = createObjectBuilder()
                .add("hearingId",hearingId.toString())
                .add("courtCentreId", courtCenterId.toString())
                .add("courtRoomId", courtRoomId.toString())
                .add("nonDefaultDays", createArrayBuilder().add(createObjectBuilder()
                        .add("courtCentreId", courtCenterId.toString())
                        .add("roomId", courtRoomId.toString())))
                .add("judiciary", createArrayBuilder().add(createObjectBuilder()
                        .add("isBenchChairman", true)
                        .add("isDeputy", false)
                        .add("judicialRoleType", createObjectBuilder().add("judicialRoleTypeId", UUID.randomUUID().toString()).add("judiciaryType", "MAGISTRATES"))
                        .add("judicialId", UUID.randomUUID().toString())
                        .add("userId", UUID.randomUUID().toString())))
                .build();
        envelope = JsonEnvelope.envelopeFrom(metadataWithRandomUUIDAndName(), payLoad);

        when(jsonObjectConverter.convert(payLoad, HearingUpdatedFromHmi.class)).thenReturn(HearingUpdatedFromHmi.hearingUpdatedFromHmi()
                .withHearingId(hearingId)
                .withCourtCentreId(courtCenterId)
                .withCourtRoomId(courtRoomId)
                .withNonDefaultDays(Collections.singletonList(NonDefaultDays.nonDefaultDays()
                        .withCourtCentreId(courtCenterId.toString())
                        .withRoomId(courtRoomId.toString())
                        .build()))
                .withJudiciary(Collections.singletonList(JudicialRole.judicialRole()
                        .withIsBenchChairman(true)
                        .withIsDeputy(false)
                        .withJudicialId(randomUUID())
                        .withUserId(randomUUID())
                        .withJudicialRoleType(JudicialRoleType.judicialRoleType().withJudicialRoleTypeId(UUID.randomUUID()).withJudiciaryType("MAGISTRATES").build())
                        .build()))
                .build());
        when(courtCentreFactory.getCourtCentre(any(), any())).thenReturn(CourtCentreDetails.courtCentreDetails()
                .withDefaultDuration(20)
                .withId(courtCenterId)
                .build());
        when(hearingService.getHearing(hearingId, envelope)).thenReturn(uk.gov.justice.listing.events.Hearing.hearing().withId(hearingId)
                .withJurisdictionType(JurisdictionType.CROWN)
                .withNonDefaultDays(new ArrayList<>())
                .withType(Type.type().build())
                .build());

        stagingHmiEventProcessor.updateHearingFromHmi(envelope);

        verify(sender).send(senderJsonEnvelopeCaptor.capture());
        assertThat(senderJsonEnvelopeCaptor.getValue().metadata().name(), is("listing.command.update-hearing-for-listing-enriched"));
        assertThat(senderJsonEnvelopeCaptor.getValue().payloadAsJsonObject().getJsonObject("courtCentreDetails").getInt("defaultDuration"), is(20));
        assertThat(senderJsonEnvelopeCaptor.getValue().payloadAsJsonObject().getJsonObject("courtCentreDetails").getString("id"), is(courtCenterId.toString()));
        assertThat(senderJsonEnvelopeCaptor.getValue().payloadAsJsonObject().getJsonObject("updateHearingForListing").getString("hearingId"), is(hearingId.toString()));
        assertThat(senderJsonEnvelopeCaptor.getValue().payloadAsJsonObject().getJsonObject("updateHearingForListing").getString("source"), is("HMI"));
        assertThat(senderJsonEnvelopeCaptor.getValue().payloadAsJsonObject().getJsonObject("updateHearingForListing").getBoolean("startDateAndWeekCommencingOptional"), is(true));
        assertThat(senderJsonEnvelopeCaptor.getValue().payloadAsJsonObject().getJsonObject("updateHearingForListing").getJsonObject("selectedCourtCentre").getString("id"), is(courtCenterId.toString()));
        assertThat(senderJsonEnvelopeCaptor.getValue().payloadAsJsonObject().getJsonObject("updateHearingForListing").getJsonObject("selectedCourtCentre").getString("courtRoomId"), is(courtRoomId.toString()));
        assertThat(senderJsonEnvelopeCaptor.getValue().payloadAsJsonObject().getJsonObject("updateHearingForListing").getJsonArray("nonDefaultDays").getJsonObject(0).getString("courtCentreId"), is(courtCenterId.toString()));
        assertThat(senderJsonEnvelopeCaptor.getValue().payloadAsJsonObject().getJsonObject("updateHearingForListing").getJsonArray("nonDefaultDays").getJsonObject(0).getString("roomId"), is(courtRoomId.toString()));
    }

    public RequestedHearingFromStagingHmi getRequestedHearingFromStagingHmi(final UUID hearingId, final UUID courtCentreId, final UUID courtRoomId) {
        return RequestedHearingFromStagingHmi.requestedHearingFromStagingHmi()
                .withHearing(getHearing(hearingId, courtCentreId, courtRoomId))
                .build();
    }

    private Hearing getHearing(final UUID hearingId, final UUID courtCentreId, final UUID courtRoomId) {
        return Hearing.hearing()
                .withId(hearingId)
                .withCourtRoomId(courtRoomId)
                .withCourtCentreId(courtCentreId)
                .withType(Type.type()
                        .withWelshDescription("WelshDescription")
                        .withDescription("Description")
                        .withId(fromString("34bc50c2-53d6-4b3a-9b48-d26fda463777"))
                        .build())
                .withJurisdictionType(JurisdictionType.CROWN)
                .withHearingLanguage(HearingLanguage.ENGLISH)
                .withEstimatedMinutes(20)
                .withStartDate(LocalDate.MAX)
                .withCourtApplications(Collections.singletonList(CourtApplication.courtApplication()
                        .withId(fromString("d1dd9e00-c0d8-445c-9209-e99d62813730"))
                        .withApplicant(ApplicantRespondent.applicantRespondent()
                                .withId(fromString("11f0f67e-c79f-479e-90d7-56a0ef5d436c"))
                                .withFirstName("FirstName")
                                .withLastName("LastName")
                                .build())
                        .withRespondents(Collections.singletonList(ApplicantRespondent.applicantRespondent()
                                .withId(fromString("c80d5163-2804-4db5-9491-54de6957effa"))
                                .withFirstName("FirstName")
                                .withLastName("LastName")
                                .build()))
                        .withLinkedCaseIds(Collections.singletonList(fromString("59152c09-d2fd-44f5-86c5-cb47d709d577")))
                        .withOffences(getOffences("c1784218-10e2-472d-a308-93cf61202a3a"))
                        .withApplicationType("ApplicationType")
                        .build()))
                .withListedCases(Collections.singletonList(ListedCase.listedCase()
                        .withId(fromString("ff5e15d9-098c-41f2-9eb0-201a65f7268e"))
                        .withDefendants(Collections.singletonList(Defendant.defendant()
                                .withId(fromString("245cecac-df76-4da9-9433-49a7b80a005c"))
                                .withOrganisationName("OrganisationName")
                                .withOffences(getOffences("665deace-6480-49f6-81f6-63e5215f15a1"))
                                .withCourtProceedingsInitiated(ZonedDateTime.parse("2022-03-27T13:38:26.472Z"))
                                .build()))
                        .withProsecutor(Prosecutor.prosecutor()
                                .withProsecutorId(fromString("490c23eb-79de-4699-80fe-7565e4a3a3a4"))
                                .withProsecutorName("ProsecutorName")
                                .withProsecutorCode("ProsecutorCode")
                                .build())
                        .withCaseIdentifier(CaseIdentifier.caseIdentifier()
                                .withAuthorityCode("AuthorityCode")
                                .withAuthorityId(fromString("42310065-37c6-4527-903f-b12cc021d184"))
                                .withCaseReference("CaseReference")
                                .build())
                        .build()))
                .withHearingDays(Collections.singletonList(HearingDay.hearingDay()
                        .withCourtCentreId(fromString("5222093d-faa2-419b-8404-5f9f75bbef84"))
                        .withStartTime(ZonedDateTime.parse("2022-03-27T13:38:26.448Z"))
                        .withEndTime(ZonedDateTime.parse("2022-03-27T13:38:26.450Z"))
                        .withDurationMinutes(20)
                        .build()))
                .build();
    }

    private List<Offence> getOffences(final String s) {
        final DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd-MMM-yyyy");
        LocalDate date = LocalDate.parse("29-Mar-2019", dtf);

        uk.gov.justice.listing.events.ReportingRestriction reportingRestriction = new
                ReportingRestriction(fromString("34bc50c2-53d6-4b3a-9b48-d26fda463777"), fromString("34bc50c2-53d6-4b3a-9b48-d26fda463777"), "label", date);

        return Collections.singletonList(Offence.offence()
                .withOffenceWording("OffenceWording")
                .withId(fromString(s))
                .withStatementOfOffence(StatementOfOffence.statementOfOffence().withTitle("Title").withLegislation("Legislation").build())
                .withOffenceCode("OffenceCode")
                .withOrderIndex(0)
                .withCount(1)
                .withStartDate(LocalDate.MAX.toString())
                .withReportingRestrictions(Arrays.asList(reportingRestriction))
                .build());
    }

    public UpdatedHearingInStagingHmi getUpdatedExistingHearingRequested(final UUID hearingId, final UUID courtCentreId, final UUID courtRoomId) {
        return UpdatedHearingInStagingHmi.updatedHearingInStagingHmi()
                .withHearing(getHearing(hearingId, courtCentreId, courtRoomId))
                .build();
    }

    public DeletedHearingInStagingHmi getDeleteExistingHearingRequested(final UUID hearingId, final UUID courtCentreId, final UUID courtRoomId) {
        return DeletedHearingInStagingHmi.deletedHearingInStagingHmi()
                .withHearingId(hearingId)
                .withCourtRoomId(courtRoomId)
                .withCourtCentreId(courtCentreId)
                .withCancellationReasonCode("ABC")
                .withCaseAndApplicationIds(Arrays.asList("1","2"))
                .build();
    }

    private UpdateHearingForListing getUpdateHearingForListing(final UUID hearingId){
        return UpdateHearingForListing.updateHearingForListing().withHearingId(hearingId).build();
    }

    private JsonObject toJsonObject(final String value) {
        return Json.createReader(new StringReader(value)).readObject();
    }
}