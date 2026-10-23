package uk.gov.moj.cpp.listing.common.xhibit.courtlist.pdda;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.moj.cpp.listing.domain.xhibit.PublishCourtListType.WARN;
import static uk.gov.moj.cpp.listing.common.xhibit.courtlist.PublishCourtListRequestParametersBuilder.withDefaults;

import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.listing.common.xhibit.courtlist.CourtListFileGenerator;
import uk.gov.moj.cpp.listing.common.xhibit.courtlist.CourtListMetadata;
import uk.gov.moj.cpp.listing.common.xhibit.courtlist.CourtListMetadataGenerator;
import uk.gov.moj.cpp.listing.common.xhibit.courtlist.ListingService;
import uk.gov.moj.cpp.listing.common.xhibit.courtlist.PublishCourtListRequestParameters;
import uk.gov.moj.cpp.listing.common.xhibit.courtlist.XmlUtils;

import java.time.ZonedDateTime;

import javax.json.JsonObject;
import javax.xml.bind.JAXBElement;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.w3c.dom.Element;

import uk.gov.justice.services.messaging.JsonObjects;

@ExtendWith(MockitoExtension.class)
public class PddaListServiceTest {

    @Mock
    private ListingService listingService;

    @Mock
    private CourtListMetadataGenerator courtListMetadataGenerator;

    @Mock
    private CourtListFileGenerator courtListFileGenerator;

    @Mock
    private XmlUtils xmlUtils;

    @Mock
    private PddaListJsonConverter pddaListJsonConverter;

    @Mock
    private JAXBElement<?> courtListDocument;

    @Mock
    private Element courtListDom;

    @Mock
    private JsonEnvelope envelope;

    @InjectMocks
    private PddaListService pddaListService;

    @Test
    public void shouldConvertTheGeneratedXhibitDocumentDirectlyWithoutWritingXml() throws Exception {
        final PublishCourtListRequestParameters parameters = withDefaults().publishCourtListType(WARN).build();
        final JsonObject courtListJson = JsonObjects.createObjectBuilder().add("courtLists", JsonObjects.createArrayBuilder()).build();
        final CourtListMetadata metadata = new CourtListMetadata("WARN.xml", "UNIQUEID", ZonedDateTime.now());

        when(listingService.getUnpublishedCourtListForCourtCentre(envelope, parameters)).thenReturn(courtListJson);
        when(courtListMetadataGenerator.generate(parameters)).thenReturn(metadata);
        doReturn(courtListDocument).when(courtListFileGenerator).generateDocument(envelope, parameters, metadata, courtListJson);
        when(xmlUtils.convertToDom(courtListDocument)).thenReturn(courtListDom);
        when(pddaListJsonConverter.convert(courtListDom, WARN))
                .thenReturn(new ObjectMapper().readTree("{\"WarnedList\":{\"DocumentID\":{\"UniqueID\":\"UNIQUEID\"}}}"));

        final JsonObject result = pddaListService.generate(envelope, parameters);

        assertThat(result.getJsonObject("WarnedList").getJsonObject("DocumentID").getString("UniqueID"), is("UNIQUEID"));

        final InOrder inOrder = inOrder(listingService, courtListMetadataGenerator, courtListFileGenerator, xmlUtils, pddaListJsonConverter);
        inOrder.verify(listingService).getUnpublishedCourtListForCourtCentre(envelope, parameters);
        inOrder.verify(courtListMetadataGenerator).generate(parameters);
        inOrder.verify(courtListFileGenerator).generateDocument(envelope, parameters, metadata, courtListJson);
        inOrder.verify(xmlUtils).convertToDom(courtListDocument);
        inOrder.verify(pddaListJsonConverter).convert(courtListDom, WARN);
        verify(courtListFileGenerator, never()).generateXml(any(), any(), any(), any());
    }
}
