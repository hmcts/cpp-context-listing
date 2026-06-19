package uk.gov.moj.cpp.listing.domain.aggregate;

import static java.util.Collections.singletonList;
import static java.util.Optional.of;
import static java.util.UUID.randomUUID;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static uk.gov.justice.services.test.utils.core.random.RandomGenerator.STRING;
import static uk.gov.moj.cpp.listing.domain.Address.address;
import static uk.gov.moj.cpp.listing.domain.CourtApplicationParty.courtApplicationParty;
import static uk.gov.moj.cpp.listing.domain.CourtApplication.courtApplication;
import static uk.gov.moj.cpp.listing.domain.CourtApplicationPartyType.PERSON;
import static uk.gov.moj.cpp.listing.domain.aggregate.NewDomainToEventConverter.buildCourtApplications;
import static org.junit.Assert.*;

import uk.gov.justice.listing.events.NewBaseDefendant;
import uk.gov.moj.cpp.listing.domain.Address;
import uk.gov.moj.cpp.listing.domain.CivilOffence;
import uk.gov.moj.cpp.listing.domain.CommittingCourt;
import uk.gov.moj.cpp.listing.domain.CourtApplication;
import uk.gov.moj.cpp.listing.domain.CourtHouseType;
import uk.gov.moj.cpp.listing.domain.JurisdictionType;
import uk.gov.moj.cpp.listing.domain.LaaReference;
import uk.gov.moj.cpp.listing.domain.Offence;
import uk.gov.moj.cpp.listing.domain.ReportingRestriction;
import uk.gov.moj.cpp.listing.domain.SeedingHearing;
import uk.gov.moj.cpp.listing.domain.StatementOfOffence;
import uk.gov.justice.listing.events.Defendant;
import uk.gov.justice.listing.events.NewBaseDefendant;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.UUID;

import org.junit.jupiter.api.Test;

public class NewDomainToEventConverterTest {

    @Test
    public void shouldBuildCourtApplications() {
        final CourtApplication courtApplication = createCourtApplication();

        uk.gov.justice.listing.events.CourtApplication courtApplicationBuilt = buildCourtApplications(courtApplication);

        assertThat(courtApplicationBuilt.getApplicationParticulars(), is(courtApplicationBuilt.getApplicationParticulars()));
        assertThat(courtApplicationBuilt.getApplicant().getAddress(), is(notNullValue()));
        checkAddress(courtApplication.getApplicant().getAddress(), courtApplicationBuilt.getApplicant().getAddress());
        assertThat(courtApplication.getRespondents().size(), equalTo(courtApplicationBuilt.getRespondents().size()));
        assertThat(courtApplicationBuilt.getRespondents().get(0).getAddress(), is(notNullValue()));
        checkAddress(courtApplication.getRespondents().get(0).getAddress(), courtApplicationBuilt.getRespondents().get(0).getAddress());
        assertThat(courtApplicationBuilt.getSubject().getAddress(), is(notNullValue()));
        checkAddress(courtApplication.getSubject().getAddress(), courtApplicationBuilt.getSubject().getAddress());

        assertThat(courtApplicationBuilt.getRespondents().get(0).getMasterDefendantId(), is(courtApplication.getRespondents().get(0).getMasterDefendantId().orElse(null)));
        assertThat(courtApplicationBuilt.getRespondents().get(0).getDateOfBirth(), is(courtApplication.getRespondents().get(0).getDateOfBirth().orElse(null)));
        assertThat(courtApplicationBuilt.getSubject().getMasterDefendantId(), is(courtApplication.getSubject().getMasterDefendantId().orElse(null)));
        assertThat(courtApplicationBuilt.getSubject().getDateOfBirth(), is(courtApplication.getSubject().getDateOfBirth().orElse(null)));
    }

    @Test
    public void shouldBuildCourtApplicationsWithNullSubject() {
        final CourtApplication courtApplication = courtApplication()
                .withApplicationParticulars(of(STRING.next()))
                .withApplicant(courtApplicationParty()
                        .withCourtApplicationPartyType(PERSON)
                        .withAddress(address()
                                .withAddress1(STRING.next())
                                .withAddress2(of(STRING.next()))
                                .withAddress3(of(STRING.next()))
                                .withAddress4(of(STRING.next()))
                                .withAddress5(of(STRING.next()))
                                .withPostcode(of(STRING.next()))
                                .build())
                        .build())
                .withRespondents(singletonList(courtApplicationParty()
                        .withCourtApplicationPartyType(PERSON)
                        .withAddress(address()
                                .withAddress1(STRING.next())
                                .withAddress2(of(STRING.next()))
                                .withAddress3(of(STRING.next()))
                                .withAddress4(of(STRING.next()))
                                .withAddress5(of(STRING.next()))
                                .withPostcode(of(STRING.next()))
                                .build())
                        .build()))
                .withSubject(null)
                .build();

        uk.gov.justice.listing.events.CourtApplication courtApplicationBuilt = buildCourtApplications(courtApplication);

        assertThat(courtApplicationBuilt.getSubject(), is((uk.gov.justice.listing.events.ApplicantRespondent) null));
    }

    @Test
    public void shouldConvertOffence() {
        final UUID seedingHearingId = randomUUID();
        final String sittingDay = LocalDate.now().toString();
        final String startDate = LocalDate.now().toString();
        final String courtHouseCode = "ABCD-1234";
        final String courtHouseShortName = "ABC";
        final String courtHouseName = "court house name";
        final UUID courtCentreId = randomUUID();
        final String laidDate = LocalDate.now().plusDays(1).toString();
        final String endDate = LocalDate.now().plusDays(3).toString();
        final UUID offenceId = randomUUID();
        final String wording = "wording";
        final String offenceCode = "offence-code";
        final String welshTitle = "welsh-title";
        final String welshLegislation = "welsh legislation";
        final String title = "title";
        final String legislation = "legislation";
        final UUID statusId = randomUUID();
        final String statusDescription = "status-description";
        final String statusDate = LocalDate.now().toString();
        final String statusCode = "status-code";
        final String effectiveStartDate = LocalDate.now().plusDays(1).toString();
        final String effectiveEndDate = LocalDate.now().plusDays(3).toString();
        final String applicationReference = "application-reference";
        final UUID reportingRestrictionId = randomUUID();
        final UUID judicialResultId = randomUUID();
        final String label = "label";
        final LocalDate orderedDate = LocalDate.now().plusDays(-1);
        final CivilOffence civilOffence = CivilOffence.civilOffence()
                .withIsExParte(true)
                .build();
        final Offence offence = Offence.offence()
                .withLaidDate(of(laidDate))
                .withId(offenceId)
                .withOffenceWording(wording)
                .withEndDate(of(endDate))
                .withOffenceCode(offenceCode)
                .withStartDate(startDate)
                .withSeedingHearing(of(SeedingHearing.seedingHearing()
                        .withJurisdictionType(JurisdictionType.CROWN)
                        .withSeedingHearingId(seedingHearingId)
                        .withSittingDay(sittingDay)
                        .build()))
                .withCommittingCourt(of(CommittingCourt.committingCourt()
                        .withCourtHouseCode(courtHouseCode)
                        .withCourtHouseShortName(courtHouseShortName)
                        .withCourtHouseName(courtHouseName)
                        .withCourtCentreId(courtCentreId)
                        .withCourtHouseType(CourtHouseType.CROWN)
                        .build()))
                .withStatementOfOffence(StatementOfOffence.statementOfOffence()
                        .withWelshTitle(welshTitle)
                        .withWelshLegislation(of(welshLegislation))
                        .withTitle(title)
                        .withLegislation(of(legislation))
                        .build())
                .withLaaApplnReference(of(LaaReference.laaReference()
                        .withStatusId(statusId)
                        .withStatusDescription(statusDescription)
                        .withStatusDate(statusDate)
                        .withStatusCode(statusCode)
                        .withEffectiveStartDate(of(effectiveStartDate))
                        .withEffectiveEndDate(of(effectiveEndDate))
                        .withApplicationReference(applicationReference)
                        .build()))
                .withReportingRestrictions(Arrays.asList(ReportingRestriction.reportingRestriction()
                        .withId(reportingRestrictionId)
                        .withJudicialResultId(of(judicialResultId))
                        .withLabel(label)
                        .withOrderedDate(of(orderedDate))
                        .build()))
                .withCivilOffence(civilOffence)
                .build();

        final uk.gov.justice.listing.events.Offence eventOffence = NewDomainToEventConverter.buildOffence(offence);

        final uk.gov.justice.listing.events.SeedingHearing seedingHearing = eventOffence.getSeedingHearing();
        assertThat(seedingHearing.getJurisdictionType().toString(), is("CROWN"));
        assertThat(seedingHearing.getSeedingHearingId(), is(seedingHearingId));
        assertThat(seedingHearing.getSittingDay(), is(sittingDay));

        assertThat(eventOffence.getStartDate(), is(startDate));
        assertThat(eventOffence.getLaidDate(), is(laidDate));
        assertThat(eventOffence.getOffenceWording(), is(wording));
        assertThat(eventOffence.getId(), is(offenceId));
        assertThat(eventOffence.getEndDate(), is(endDate));
        assertThat(eventOffence.getOffenceCode(), is(offenceCode));

        final uk.gov.justice.listing.events.CommittingCourt committingCourt = eventOffence.getCommittingCourt();
        assertThat(committingCourt.getCourtCentreId(), is(courtCentreId));
        assertThat(committingCourt.getCourtHouseCode(), is(courtHouseCode));
        assertThat(committingCourt.getCourtHouseName(), is(courtHouseName));
        assertThat(committingCourt.getCourtHouseShortName(), is(courtHouseShortName));
        // justice listing events CommittingCourt does not expose courtHouseType on the current API

        final uk.gov.justice.listing.events.StatementOfOffence statementOfOffence = eventOffence.getStatementOfOffence();
        assertThat(statementOfOffence.getWelshLegislation(), is(welshLegislation));
        assertThat(statementOfOffence.getWelshTitle(), is(welshTitle));
        assertThat(statementOfOffence.getLegislation(), is(legislation));
        assertThat(statementOfOffence.getTitle(), is(title));

        final uk.gov.justice.listing.events.LaaReference laaReference = eventOffence.getLaaApplnReference();
        assertThat(laaReference.getStatusId(), is(statusId));
        assertThat(laaReference.getStatusDate(), is(statusDate));
        assertThat(laaReference.getStatusDescription(), is(statusDescription));
        assertThat(laaReference.getStatusCode(), is(statusCode));
        assertThat(laaReference.getEffectiveStartDate(), is(effectiveStartDate));
        assertThat(laaReference.getEffectiveEndDate(), is(effectiveEndDate));
        assertThat(laaReference.getApplicationReference(), is(applicationReference));

        assertThat(eventOffence.getReportingRestrictions().size(), is(1));
        final uk.gov.justice.listing.events.ReportingRestriction reportingRestriction = eventOffence.getReportingRestrictions().get(0);
        assertThat(reportingRestriction.getId(), is(reportingRestrictionId));
        assertThat(reportingRestriction.getJudicialResultId(), is(judicialResultId));
        assertThat(reportingRestriction.getLabel(), is(label));
        assertThat(reportingRestriction.getOrderedDate(), is(orderedDate));

        assertThat(civilOffence.getIsExParte(),is(eventOffence.getCivilOffence().getIsExParte()));

    }

    @Test
    public void shouldConvertOffenceWhenOnlyMandatoryFieldsFilled() {

        final String startDate = LocalDate.now().toString();
        final String laidDate = LocalDate.now().plusDays(1).toString();
        final String endDate = LocalDate.now().plusDays(3).toString();
        final UUID offenceId = randomUUID();
        final String wording = "wording";
        final String offenceCode = "offence-code";
        final String welshTitle = "welsh-title";
        final String welshLegislation = "welsh legislation";
        final String title = "title";
        final String legislation = "legislation";
        final UUID statusId = randomUUID();
        final String statusDescription = "status-description";
        final String statusDate = LocalDate.now().toString();
        final String statusCode = "status-code";
        final String effectiveStartDate = LocalDate.now().plusDays(1).toString();
        final String effectiveEndDate = LocalDate.now().plusDays(3).toString();
        final String applicationReference = "application-reference";

        final Offence offence = Offence.offence()
                .withLaidDate(of(laidDate))
                .withId(offenceId)
                .withOffenceWording(wording)
                .withEndDate(of(endDate))
                .withOffenceCode(offenceCode)
                .withStartDate(startDate)
                .withLaaApplnReference(of(LaaReference.laaReference()
                        .withStatusId(statusId)
                        .withStatusDescription(statusDescription)
                        .withStatusDate(statusDate)
                        .withStatusCode(statusCode)
                        .withEffectiveStartDate(of(effectiveStartDate))
                        .withEffectiveEndDate(of(effectiveEndDate))
                        .withApplicationReference(applicationReference)
                        .build()))
                .withStatementOfOffence(StatementOfOffence.statementOfOffence()
                        .withWelshTitle(welshTitle)
                        .withWelshLegislation(of(welshLegislation))
                        .withTitle(title)
                        .withLegislation(of(legislation))
                        .build())
                .build();

        final uk.gov.justice.listing.events.Offence eventOffence = NewDomainToEventConverter.buildOffence(offence);

        final uk.gov.justice.listing.events.StatementOfOffence statementOfOffence = eventOffence.getStatementOfOffence();
        assertThat(statementOfOffence.getWelshLegislation(), is(welshLegislation));
        assertThat(statementOfOffence.getWelshTitle(), is(welshTitle));
        assertThat(statementOfOffence.getLegislation(), is(legislation));
        assertThat(statementOfOffence.getTitle(), is(title));

        assertThat(eventOffence.getStartDate(), is(startDate));
        assertThat(eventOffence.getLaidDate(), is(laidDate));
        assertThat(eventOffence.getOffenceWording(), is(wording));
        assertThat(eventOffence.getId(), is(offenceId));
        assertThat(eventOffence.getEndDate(), is(endDate));
        assertThat(eventOffence.getOffenceCode(), is(offenceCode));

        final uk.gov.justice.listing.events.LaaReference laaReference = eventOffence.getLaaApplnReference();
        assertThat(laaReference.getStatusId(), is(statusId));
        assertThat(laaReference.getStatusDate(), is(statusDate));
        assertThat(laaReference.getStatusDescription(), is(statusDescription));
        assertThat(laaReference.getStatusCode(), is(statusCode));
        assertThat(laaReference.getEffectiveStartDate(), is(effectiveStartDate));
        assertThat(laaReference.getEffectiveEndDate(), is(effectiveEndDate));
        assertThat(laaReference.getApplicationReference(), is(applicationReference));



    }

    @Test
    public void existingIsYouthTrue_newIsNull_retainsTrue() {
        final UUID defendantId = randomUUID();
        final UUID masterDefendantId = randomUUID();
        final Defendant existing = buildExistingDefendant(defendantId, "OldFirst", Boolean.TRUE, masterDefendantId);
        final NewBaseDefendant update = buildNewBaseDefendant(defendantId, null, null, null);

        final Defendant result = NewDomainToEventConverter.updateEventDefendant(update, existing);

        assertNotNull(result);
        assertTrue("isYouth should remain true when existing true and incoming null", Boolean.TRUE.equals(result.getIsYouth()));
        assertEquals("First name should be preserved", "OldFirst", result.getFirstName());
        assertEquals("Master id preserved", masterDefendantId, result.getMasterDefendantId());
    }

    @Test
    public void existingIsYouthFalse_newIsTrue_becomesTrue() {
        final UUID defendantId = randomUUID();
        final Defendant existing = buildExistingDefendant(defendantId, "OldFirst", Boolean.FALSE, null);
        final NewBaseDefendant update = buildNewBaseDefendant(defendantId, null, Boolean.TRUE, null);

        final Defendant result = NewDomainToEventConverter.updateEventDefendant(update, existing);

        assertNotNull(result);
        assertTrue("isYouth should become true when incoming true", Boolean.TRUE.equals(result.getIsYouth()));
    }

    @Test
    public void existingIsYouthTrue_newIsFalse_retainsTrue() {
        final UUID defendantId = randomUUID();
        final Defendant existing = buildExistingDefendant(defendantId, "OldFirst", Boolean.TRUE, null);
        final NewBaseDefendant update = buildNewBaseDefendant(defendantId, null, Boolean.FALSE, null);

        final Defendant result = NewDomainToEventConverter.updateEventDefendant(update, existing);

        assertNotNull(result);
        assertTrue("isYouth should remain true even if incoming is false", Boolean.TRUE.equals(result.getIsYouth()));
    }

    @Test
    public void existingIsYouthFalse_newIsNull_remainsFalse() {
        final UUID defendantId = randomUUID();
        final Defendant existing = buildExistingDefendant(defendantId, "OldFirst", Boolean.FALSE, null);
        final NewBaseDefendant update = buildNewBaseDefendant(defendantId, null, null, null);

        final Defendant result = NewDomainToEventConverter.updateEventDefendant(update, existing);

        assertNotNull(result);
        assertEquals("isYouth remains false when no incoming value", Boolean.FALSE, result.getIsYouth());
    }

    @Test
    public void firstName_updateWhenProvided_or_preserveWhenNull() {
        final UUID defendantId = randomUUID();

        final Defendant existing = buildExistingDefendant(defendantId, "ExistingFirst", Boolean.FALSE, null);

        // incoming with null firstName -> preserve existing
        final NewBaseDefendant updateNull = buildNewBaseDefendant(defendantId, null, null, null);
        final Defendant resultPreserve = NewDomainToEventConverter.updateEventDefendant(updateNull, existing);
        assertEquals("Existing first name preserved when incoming null", "ExistingFirst", resultPreserve.getFirstName());

        // incoming with new firstName -> update to new
        final NewBaseDefendant updateNew = buildNewBaseDefendant(defendantId, "NewFirst", null, null);
        final Defendant resultUpdated = NewDomainToEventConverter.updateEventDefendant(updateNew, existing);
        assertEquals("First name updated when incoming non-null", "NewFirst", resultUpdated.getFirstName());
    }

    @Test
    public void masterDefendantId_updatesWhenProvided_or_preserveWhenNull() {
        final UUID defendantId = randomUUID();
        final UUID masterDefendantId = randomUUID();
        final UUID masterDefendantId1 = randomUUID();
        final Defendant existing = buildExistingDefendant(defendantId, "Name", Boolean.FALSE, masterDefendantId);

        // incoming sets null master id -> preserve existing
        final NewBaseDefendant updateNull = buildNewBaseDefendant(defendantId, null, null, null);
        final Defendant preserved = NewDomainToEventConverter.updateEventDefendant(updateNull, existing);
        assertEquals("Master id preserved when incoming null", masterDefendantId, preserved.getMasterDefendantId());

        // incoming provides new master id -> update
        final NewBaseDefendant updateNew = buildNewBaseDefendant(defendantId, null, null, masterDefendantId1);
        final Defendant updated = NewDomainToEventConverter.updateEventDefendant(updateNew, existing);
        assertEquals("Master id updated when incoming provided", masterDefendantId1, updated.getMasterDefendantId());
    }

    @Test
    public void combination_existingTrue_and_incomingChanges_retainsIsYouthTrue_and_updatesOtherFields() {
        final UUID defendantId = randomUUID();
        final UUID masterDefendantId = randomUUID();
        final Defendant existing = buildExistingDefendant(defendantId, "ExistFirst", Boolean.TRUE, masterDefendantId);
        final NewBaseDefendant update = buildNewBaseDefendant(defendantId, "UpdatedFirst", Boolean.FALSE, null);

        final Defendant result = NewDomainToEventConverter.updateEventDefendant(update, existing);

        // isYouth should still be true (retained once true)
        assertTrue("isYouth retained true", Boolean.TRUE.equals(result.getIsYouth()));
        // first name should be updated
        assertEquals("firstName updated", "UpdatedFirst", result.getFirstName());
        // master id should remain (incoming null -> preserve)
        assertEquals("master id preserved", masterDefendantId, result.getMasterDefendantId());
    }


    private CourtApplication createCourtApplication() {
        return courtApplication()
                .withApplicationParticulars(of(STRING.next()))
                .withApplicant(courtApplicationParty()
                        .withCourtApplicationPartyType(PERSON)
                        .withAddress(address()
                                .withAddress1(STRING.next())
                                .withAddress2(of(STRING.next()))
                                .withAddress3(of(STRING.next()))
                                .withAddress4(of(STRING.next()))
                                .withAddress5(of(STRING.next()))
                                .withPostcode(of(STRING.next()))
                                .build())
                        .build())
                .withRespondents(singletonList(courtApplicationParty()
                        .withCourtApplicationPartyType(PERSON)
                        .withMasterDefendantId(randomUUID())
                        .withDateOfBirth("1990-05-15")
                        .withAddress(address()
                                .withAddress1(STRING.next())
                                .withAddress2(of(STRING.next()))
                                .withAddress3(of(STRING.next()))
                                .withAddress4(of(STRING.next()))
                                .withAddress5(of(STRING.next()))
                                .withPostcode(of(STRING.next()))
                                .build())
                        .build()))
                .withSubject(courtApplicationParty()
                        .withCourtApplicationPartyType(PERSON)
                        .withMasterDefendantId(randomUUID())
                        .withDateOfBirth("1985-12-01")
                        .withAddress(address()
                                .withAddress1(STRING.next())
                                .withAddress2(of(STRING.next()))
                                .withAddress3(of(STRING.next()))
                                .withAddress4(of(STRING.next()))
                                .withAddress5(of(STRING.next()))
                                .withPostcode(of(STRING.next()))
                                .build())
                        .build())
                .build();
    }

    private void checkAddress(final Address address, final uk.gov.justice.core.courts.Address addressBuilt) {
        assertThat(addressBuilt.getAddress1(), is(address.getAddress1()));
        assertThat(addressBuilt.getAddress2(), is(notNullValue()));
        assertThat(addressBuilt.getAddress2(), is(address.getAddress2().get()));
        assertThat(addressBuilt.getAddress3(), is(notNullValue()));
        assertThat(addressBuilt.getAddress3(), is(address.getAddress3().get()));
        assertThat(addressBuilt.getAddress4(), is(notNullValue()));
        assertThat(addressBuilt.getAddress4(), is(address.getAddress4().get()));
        assertThat(addressBuilt.getAddress5(), is(notNullValue()));
        assertThat(addressBuilt.getAddress5(), is(address.getAddress5().get()));
        assertThat(addressBuilt.getPostcode(), is(notNullValue()));
        assertThat(addressBuilt.getPostcode(), is(address.getPostcode().get()));
    }

    // Helper: build a NewBaseDefendant (incoming update) with fields we care about
    private NewBaseDefendant buildNewBaseDefendant(final UUID id,
                                                   final String firstName,
                                                   final Boolean isYouth,
                                                   final UUID masterDefendantId) {
        return NewBaseDefendant.newBaseDefendant()
                .withId(id)
                .withFirstName(firstName)
                .withMasterDefendantId(masterDefendantId)
                .withIsYouth(isYouth != null ? isYouth : null)
                .build();
    }

    // Helper: build an event Defendant with fields we care about
    private Defendant buildExistingDefendant(final UUID id,
                                             final String firstName,
                                             final Boolean isYouth,
                                             final UUID masterDefendantId) {
        return Defendant.defendant()
                .withId(id)
                .withFirstName(firstName)
                .withMasterDefendantId(masterDefendantId)
                .withIsYouth(isYouth != null ? isYouth : null)
                .build();
    }

}