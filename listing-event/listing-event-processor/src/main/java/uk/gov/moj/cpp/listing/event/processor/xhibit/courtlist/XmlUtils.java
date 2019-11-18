package uk.gov.moj.cpp.listing.event.processor.xhibit.courtlist;

import static java.lang.String.format;
import static javax.xml.XMLConstants.W3C_XML_SCHEMA_NS_URI;
import static javax.xml.bind.JAXBContext.newInstance;
import static javax.xml.bind.Marshaller.JAXB_FORMATTED_OUTPUT;

import uk.gov.moj.cpp.listing.event.processor.xhibit.exception.GenerationFailedException;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.net.URL;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;

import com.google.common.annotations.VisibleForTesting;
import org.slf4j.Logger;
import org.xml.sax.SAXException;

public class XmlUtils {

    private static DatatypeFactory datatypeFactory;

    @Inject
    @SuppressWarnings("squid:S1312")
    private Logger logger;

    @PostConstruct
    public static void postConstruct() {
        try {
            datatypeFactory = DatatypeFactory.newInstance();
        } catch (DatatypeConfigurationException e) {
            throw new GenerationFailedException("Cannot get instance of DatatypeFactory: ", e);
        }
    }

    public static XMLGregorianCalendar convertDate(final String dateString) {
            return datatypeFactory.newXMLGregorianCalendar(dateString);
    }

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
    public void validate(final String inputXml, final String schemaFile) {

        try {
            final URL xsd = this.getClass().getClassLoader().getResource(schemaFile);
            final Schema schema = SchemaFactory.newInstance(W3C_XML_SCHEMA_NS_URI).newSchema(xsd);
            final Validator validator = schema.newValidator();

            final Source source = new StreamSource(new StringReader(inputXml));

            validator.validate(source);
        } catch (SAXException | IOException e) {
            logger.info(inputXml);
            throw new GenerationFailedException(format("Could not validate XML against schema %s : %s", schemaFile, e.getMessage()), e);
        }
    }

    private JAXBContext getJaxbContext() throws JAXBException {
        return newInstance("uk.gov.moj.cpp.listing.domain.xhibit.generated");
    }

    @VisibleForTesting
    public void setLogger(Logger logger) {
        this.logger = logger;
    }
}
