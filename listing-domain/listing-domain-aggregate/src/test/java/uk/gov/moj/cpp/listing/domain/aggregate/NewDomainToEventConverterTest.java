package uk.gov.moj.cpp.listing.domain.aggregate;

import static java.util.Collections.singletonList;
import static java.util.Optional.of;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static uk.gov.justice.services.test.utils.core.random.RandomGenerator.STRING;
import static uk.gov.moj.cpp.listing.domain.Address.address;
import static uk.gov.moj.cpp.listing.domain.ApplicantRespondent.applicantRespondent;
import static uk.gov.moj.cpp.listing.domain.CourtApplication.courtApplication;
import static uk.gov.moj.cpp.listing.domain.CourtApplicationPartyType.PERSON;
import static uk.gov.moj.cpp.listing.domain.aggregate.NewDomainToEventConverter.buildCourtApplications;

import uk.gov.moj.cpp.listing.domain.Address;
import uk.gov.moj.cpp.listing.domain.CourtApplication;

import org.junit.Test;

public class NewDomainToEventConverterTest {

    @Test
    public void shouldBuildCourtApplications() {
        final CourtApplication courtApplication = createCourtApplication();

        uk.gov.justice.listing.events.CourtApplication courtApplicationBuilt = buildCourtApplications(courtApplication);

        assertThat(courtApplicationBuilt.getApplicationParticulars().get(), is(courtApplicationBuilt.getApplicationParticulars().get()));
        assertThat(courtApplicationBuilt.getApplicant().getAddress().isPresent(), is(true));
        checkAddress(courtApplication.getApplicant().getAddress(), courtApplicationBuilt.getApplicant().getAddress().get());
        assertThat(courtApplication.getRespondents().size(), equalTo(courtApplicationBuilt.getRespondents().size()));
        assertThat(courtApplicationBuilt.getRespondents().get(0).getAddress().isPresent(), is(true));
        checkAddress(courtApplication.getRespondents().get(0).getAddress(), courtApplicationBuilt.getRespondents().get(0).getAddress().get());
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
        assertThat(addressBuilt.getAddress2().isPresent(), is(true));
        assertThat(addressBuilt.getAddress2().get(), is(address.getAddress2().get()));
        assertThat(addressBuilt.getAddress3().isPresent(), is(true));
        assertThat(addressBuilt.getAddress3().get(), is(address.getAddress3().get()));
        assertThat(addressBuilt.getAddress4().isPresent(), is(true));
        assertThat(addressBuilt.getAddress4().get(), is(address.getAddress4().get()));
        assertThat(addressBuilt.getAddress5().isPresent(), is(true));
        assertThat(addressBuilt.getAddress5().get(), is(address.getAddress5().get()));
        assertThat(addressBuilt.getPostcode().isPresent(), is(true));
        assertThat(addressBuilt.getPostcode().get(), is(address.getPostcode().get()));
    }
}