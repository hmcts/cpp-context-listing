package uk.gov.moj.cpp.listing.event.processor.xhibit.courtlist.mapper;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

import uk.gov.moj.cpp.listing.domain.xhibit.generated.CourtHouseStructure;
import uk.gov.moj.cpp.listing.domain.xhibit.generated.DocumentIDstructure;
import uk.gov.moj.cpp.listing.domain.xhibit.generated.ListHeaderStructure;

import java.time.LocalDate;
import java.util.UUID;

import org.junit.Before;
import org.junit.Test;

public class CourtServicesMapperTest extends BaseMapperTest {

    private CourtServicesMapper courtServicesMapper;

    @Before
    public void before() {

        courtServicesMapper = new CourtServicesMapper(context, xhibitReferenceDataService);
    }

    @Test
    public void generateDocumentID() {

        DocumentIDstructure documentIDstructure = courtServicesMapper.generateDocumentID();

        assertThat(documentIDstructure.getUniqueID(), is("UNIQUEID"));
        assertThat(documentIDstructure.getDocumentType(), is("FL"));
        assertThat(documentIDstructure.getDocumentName(), is("FIRM-FILENAME"));
    }

    @Test
    public void generateListHeader() {

        ListHeaderStructure listHeaderStructure = courtServicesMapper.generateListHeader();

        assertThat(listHeaderStructure.getVersion(), is("NOT VERSIONED"));
        assertThat(listHeaderStructure.getListCategory(), is("Criminal"));
        assertThat(listHeaderStructure.getStartDate(), is(LocalDate.parse("2019-11-04")));
        assertThat(listHeaderStructure.getEndDate(), is(LocalDate.parse("2019-11-05")));
        assertThat(listHeaderStructure.getPublishedTime().toString(), is("2018-01-02T13:04:05Z"));
    }

    @Test
    public void generateCourtHouseStructure() {

        UUID courtCentreId = UUID.fromString("973961d2-ae3f-44d1-8926-c9b66edc2df2");

        CourtHouseStructure courtHouseStructure = courtServicesMapper.generateCourtHouseStructure(courtCentreId);

        assertThat(courtHouseStructure.getCourtHouseType().value(), is("Crown Court"));
        assertThat(courtHouseStructure.getCourtHouseName(), is("MOCKCOURTNAME"));
        assertThat(courtHouseStructure.getCourtHouseCode().getValue(), is("001"));
    }
}
