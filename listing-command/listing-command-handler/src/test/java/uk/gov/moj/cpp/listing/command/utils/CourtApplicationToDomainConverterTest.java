package uk.gov.moj.cpp.listing.command.utils;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

import uk.gov.justice.core.courts.CourtApplication;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.ObjectToJsonValueConverter;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.justice.services.test.utils.framework.api.JsonObjectConvertersFactory;
import uk.gov.moj.cpp.listing.domain.Address;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class CourtApplicationToDomainConverterTest {
    private final CourtApplicationToDomainConverter converter = new CourtApplicationToDomainConverter();

    @Spy
    private ObjectMapper objectMapper = new ObjectMapperProducer().objectMapper();

    @Spy
    private JsonObjectToObjectConverter jsonObjectToObjectConverter = new JsonObjectConvertersFactory().jsonObjectToObjectConverter();

    private ObjectToJsonValueConverter objectToJsonValueConverter = new JsonObjectConvertersFactory().objectToJsonValueConverter();

    @InjectMocks
    CommandBuilder commandBuilder;

    @Test
    public void shouldConvertCourtApplicationWithApplicantRespondent() {

        final CourtApplication courtApplication = commandBuilder.buildCourtApplication();
        final uk.gov.moj.cpp.listing.domain.CourtApplication actual = converter.convert(courtApplication);
        assertThat(actual.getId(), is(courtApplication.getId()));
        assertThat(actual.getApplicant().getFirstName().get(), is(courtApplication.getApplicant().getPersonDetails().getFirstName()));
        assertThat(actual.getApplicant().getLastName(), is(courtApplication.getApplicant().getPersonDetails().getLastName()));
        assertThat(actual.getApplicationType(), is(courtApplication.getType().getType()));
        assertThat(actual.getApplicationParticulars().get(), is(courtApplication.getApplicationParticulars()));
        assertThat(actual.getRespondents().get(0).getFirstName().get(), is(courtApplication.getRespondents().get(0).getPersonDetails().getFirstName()));
        assertThat(actual.getRespondents().get(0).getLastName(), is(courtApplication.getRespondents().get(0).getPersonDetails().getLastName()));
        assertThat(actual.getRespondents().get(1).getLastName(), is(courtApplication.getRespondents().get(1).getProsecutingAuthority().getProsecutionAuthorityCode()));
        assertThat(actual.getRespondents().get(2).getLastName(), is(courtApplication.getRespondents().get(2).getOrganisation().getName()));
        assertThat(actual.getRespondents().get(3).getFirstName().get(), is(courtApplication.getRespondents().get(3).getMasterDefendant().getPersonDefendant().getPersonDetails().getFirstName()));
        assertThat(actual.getRespondents().get(3).getLastName(), is(courtApplication.getRespondents().get(3).getMasterDefendant().getPersonDefendant().getPersonDetails().getLastName()));
        checkAddress(actual.getApplicant().getAddress(), courtApplication.getApplicant().getPersonDetails().getAddress());
        checkAddress(actual.getRespondents().get(0).getAddress(), courtApplication.getRespondents().get(0).getPersonDetails().getAddress());
        checkAddress(actual.getRespondents().get(1).getAddress(), courtApplication.getRespondents().get(1).getProsecutingAuthority().getAddress());
        checkAddress(actual.getRespondents().get(2).getAddress(), courtApplication.getRespondents().get(2).getOrganisation().getAddress());
        checkAddress(actual.getRespondents().get(3).getAddress(), courtApplication.getRespondents().get(3).getMasterDefendant().getPersonDefendant().getPersonDetails().getAddress());

    }

    @Test
    public void shouldConvertCourtApplicationWithLegalEntityDefendant() {
        final CourtApplication courtApplication = commandBuilder.buildCourtApplicationWithLegalEntity();
        final uk.gov.moj.cpp.listing.domain.CourtApplication actual = converter.convert(courtApplication);
        assertThat(actual.getId(), is(courtApplication.getId()));
        assertThat(actual.getApplicant().getLastName(), is(courtApplication.getApplicant().getMasterDefendant().getLegalEntityDefendant().getOrganisation().getName()));
        assertThat(actual.getApplicationType(), is(courtApplication.getType().getType()));
        assertThat(actual.getApplicationParticulars().get(), is(courtApplication.getApplicationParticulars()));
        checkAddress(actual.getApplicant().getAddress(), courtApplication.getApplicant().getMasterDefendant().getLegalEntityDefendant().getOrganisation().getAddress());
        checkAddress(actual.getRespondents().get(0).getAddress(), courtApplication.getRespondents().get(0).getPersonDetails().getAddress());
    }

    @Test
    public void shouldConvertCourtApplicationWithOrganisation() {
        final CourtApplication courtApplication = commandBuilder.buildCourtApplicationWithOrganisation();
        final uk.gov.moj.cpp.listing.domain.CourtApplication actual = converter.convert(courtApplication);
        assertThat(actual.getId(), is(courtApplication.getId()));
        assertThat(actual.getApplicant().getLastName(), is(courtApplication.getApplicant().getOrganisation().getName()));
        assertThat(actual.getApplicationType(), is(courtApplication.getType().getType()));
        assertThat(actual.getApplicationParticulars().get(), is(courtApplication.getApplicationParticulars()));
        checkAddress(actual.getApplicant().getAddress(), courtApplication.getApplicant().getOrganisation().getAddress());
        checkAddress(actual.getRespondents().get(0).getAddress(), courtApplication.getRespondents().get(0).getPersonDetails().getAddress());
    }

    private void checkAddress(final Address actualAddress, final uk.gov.justice.core.courts.Address address) {
        assertThat(actualAddress.getAddress1(), is(address.getAddress1()));
        assertThat(actualAddress.getAddress2().get(), is(address.getAddress2()));
        assertThat(actualAddress.getAddress3().get(), is(address.getAddress3()));
        assertThat(actualAddress.getAddress4().get(), is(address.getAddress4()));
        assertThat(actualAddress.getAddress5().orElse(null), is(address.getAddress5()));
        assertThat(actualAddress.getPostcode().orElse(null), is(address.getPostcode()));
    }
}
