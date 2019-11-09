package uk.gov.moj.cpp.listing.event.processor.xhibit.courtlist;

import static javax.xml.XMLConstants.FEATURE_SECURE_PROCESSING;
import static javax.xml.XMLConstants.W3C_XML_SCHEMA_NS_URI;
import static javax.xml.bind.JAXBContext.newInstance;
import static javax.xml.bind.Marshaller.JAXB_FORMATTED_OUTPUT;

import uk.gov.moj.cpp.listing.event.processor.xhibit.exception.GenerationFailedException;

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

public class XmlUtils {

    public String convertToXml(final JAXBElement<?> documentRoot) {

        final StringWriter sw = new StringWriter();

        try {
            final Marshaller jaxbMarshaller = getJaxbContext().createMarshaller();
            jaxbMarshaller.setProperty("com.sun.xml.bind.namespacePrefixMapper", new XhibitNamespacePrefixMapper());

            jaxbMarshaller.setProperty(JAXB_FORMATTED_OUTPUT, true);
            jaxbMarshaller.marshal(documentRoot, sw);
        } catch (final JAXBException e) {
            throw new GenerationFailedException("Could not marshal XML", e);
        }

        return sw.toString();
    }

    @SuppressWarnings({"squid:S2755", "squid:S1160"})
    public void validate(final JAXBElement<?> rootElement, final String schemaFile) {

        try {
            final URL xsd = this.getClass().getClassLoader().getResource(schemaFile);
            final Schema schema = SchemaFactory.newInstance(W3C_XML_SCHEMA_NS_URI).newSchema(xsd);

            final JAXBSource snippet = new JAXBSource(getJaxbContext(), rootElement);
            final Validator validator = schema.newValidator();
            validator.setFeature(FEATURE_SECURE_PROCESSING, true);
            validator.validate(snippet);
        } catch (final JAXBException | SAXException | IOException e) {
            throw new GenerationFailedException("Could not validate XML", e);
        }
    }

    private JAXBContext getJaxbContext() throws JAXBException {
        return newInstance("uk.gov.moj.cpp.listing.domain.xhibit.generated");
    }
}
