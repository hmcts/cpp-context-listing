package uk.gov.moj.cpp.listing.event.processor.xhibit.courtlist;

import static javax.xml.XMLConstants.FEATURE_SECURE_PROCESSING;
import static javax.xml.XMLConstants.W3C_XML_SCHEMA_NS_URI;
import static javax.xml.bind.JAXBContext.newInstance;

import uk.gov.moj.cpp.listing.domain.xhibit.generated.FirmListStructure;
import uk.gov.moj.cpp.listing.domain.xhibit.generated.ObjectFactory;

import java.io.IOException;
import java.io.StringWriter;
import java.net.URL;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.util.JAXBSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;

import org.xml.sax.SAXException;

public class XmlConverter {

    private final ObjectFactory objectFactory = new ObjectFactory();

    public String convertToXml(final FirmListStructure firmListStructure) throws JAXBException {

        final JAXBElement<FirmListStructure> firmListRoot = objectFactory.createFirmList(firmListStructure);

        //validate(firmListRoot, "xhibit/xsd/FirmList.xsd");    // TODO SCSL-167 Validate when full document generation is coded

        return convertToXml(firmListRoot);
    }

    private String convertToXml(final JAXBElement<?> rootElement) throws JAXBException {

        final StringWriter sw = new StringWriter();
        final Marshaller jaxbMarshaller = getJaxbContext().createMarshaller();
        jaxbMarshaller.setProperty("com.sun.xml.bind.namespacePrefixMapper", new XhibitNamespacePrefixMapper());

        jaxbMarshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
        jaxbMarshaller.marshal(rootElement, sw);

        return sw.toString();
    }

    @SuppressWarnings({"squid:S2755", "squid:S1160"})
    public void validate(final JAXBElement<?> rootElement, final String schemaFile) throws SAXException, JAXBException, IOException {

        final URL xsd = this.getClass().getClassLoader().getResource(schemaFile);
        final Schema schema = SchemaFactory.newInstance(W3C_XML_SCHEMA_NS_URI).newSchema(xsd);

        final JAXBSource snippet = new JAXBSource(getJaxbContext(), rootElement);
        final Validator validator = schema.newValidator();
        validator.setFeature(FEATURE_SECURE_PROCESSING, true);
        validator.validate(snippet);
    }

    private JAXBContext getJaxbContext() throws JAXBException {
        return newInstance("uk.gov.moj.cpp.listing.domain.xhibit.generated");
    }
}
