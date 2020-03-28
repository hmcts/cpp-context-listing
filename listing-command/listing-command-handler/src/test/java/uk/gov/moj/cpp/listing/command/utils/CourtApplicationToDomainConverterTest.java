package uk.gov.moj.cpp.listing.command.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;

import uk.gov.justice.core.courts.CourtApplication;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.ObjectToJsonValueConverter;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

@RunWith(MockitoJUnitRunner.class)
public class CourtApplicationToDomainConverterTest {
    private final CourtApplicationToDomainConverter converter = new CourtApplicationToDomainConverter();

    @Spy
    ObjectMapper objectMapper = new ObjectMapperProducer().objectMapper();

    @Spy
    @InjectMocks
    JsonObjectToObjectConverter jsonObjectToObjectConverter = new JsonObjectToObjectConverter();

    ObjectToJsonValueConverter objectToJsonValueConverter = new ObjectToJsonValueConverter(objectMapper);

    @InjectMocks
    CommandBuilder commandBuilder;

    @Test
    public void shouldConvertCourtApplicationWithApplicantRespondent() {

        final CourtApplication courtApplication = commandBuilder.buildCourtApplication();
        final uk.gov.moj.cpp.listing.domain.CourtApplication actual = converter.convert(courtApplication);
        assertThat(actual.getId(), is(courtApplication.getId()));
        assertThat(actual.getApplicant().getFirstName().get(), is(courtApplication.getApplicant().getPersonDetails().get().getFirstName().get()));
        assertThat(actual.getApplicant().getLastName(), is(courtApplication.getApplicant().getPersonDetails().get().getLastName()));
        assertThat(actual.getApplicationType(), is(courtApplication.getType().getApplicationType()));
        assertThat(actual.getRespondents().get(0).getFirstName().get(), is(courtApplication.getRespondents().get(0).getPartyDetails().getPersonDetails().get().getFirstName().get()));
        assertThat(actual.getRespondents().get(0).getLastName(), is(courtApplication.getRespondents().get(0).getPartyDetails().getPersonDetails().get().getLastName()));
        assertThat(actual.getRespondents().get(1).getLastName(), is(courtApplication.getRespondents().get(1).getPartyDetails().getProsecutingAuthority().get().getProsecutionAuthorityCode()));
        assertThat(actual.getRespondents().get(2).getLastName(), is(courtApplication.getRespondents().get(2).getPartyDetails().getOrganisation().get().getName()));
        assertThat(actual.getRespondents().get(3).getFirstName().get(), is(courtApplication.getRespondents().get(3).getPartyDetails().getDefendant().get().getPersonDefendant().get().getPersonDetails().getFirstName().get()));
        assertThat(actual.getRespondents().get(3).getLastName(), is(courtApplication.getRespondents().get(3).getPartyDetails().getDefendant().get().getPersonDefendant().get().getPersonDetails().getLastName()));
    }

    @Test
    public void shouldConvertCourtApplicationWithLegalEntityDefendant() {
        final CourtApplication courtApplication = commandBuilder.buildCourtApplicationWithLegalEntity();
        final uk.gov.moj.cpp.listing.domain.CourtApplication actual = converter.convert(courtApplication);
        assertThat(actual.getId(), is(courtApplication.getId()));
        assertThat(actual.getApplicant().getLastName(), is(courtApplication.getApplicant().getDefendant().get().getLegalEntityDefendant().get().getOrganisation().getName()));
        assertThat(actual.getApplicationType(), is(courtApplication.getType().getApplicationType()));
    }

    @Test
    public void shouldConvertCourtApplicationWithOrganisation() {
        final CourtApplication courtApplication = commandBuilder.buildCourtApplicationWithOrganisation();
        final uk.gov.moj.cpp.listing.domain.CourtApplication actual = converter.convert(courtApplication);
        assertThat(actual.getId(), is(courtApplication.getId()));
        assertThat(actual.getApplicant().getLastName(), is(courtApplication.getApplicant().getOrganisation().get().getName()));
        assertThat(actual.getApplicationType(), is(courtApplication.getType().getApplicationType()));
    }
}
