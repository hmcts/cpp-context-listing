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
import static uk.gov.moj.cpp.listing.domain.ApplicantRespondent.applicantRespondent;
import static uk.gov.moj.cpp.listing.domain.CourtApplication.courtApplication;
import static uk.gov.moj.cpp.listing.domain.CourtApplicationPartyType.PERSON;
import static uk.gov.moj.cpp.listing.domain.aggregate.NewDomainToEventConverter.buildCourtApplications;

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
//        assertThat(committingCourt.getCourtHouseType(), is(uk.gov.justice.listing.events.CourtHouseType.CROWN));

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

    private CourtApplication createCourtApplication() {
        return courtApplication()
                .withApplicationParticulars(of(STRING.next()))
                .withApplicant(applicantRespondent()
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
                .withRespondents(singletonList(applicantRespondent()
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
}