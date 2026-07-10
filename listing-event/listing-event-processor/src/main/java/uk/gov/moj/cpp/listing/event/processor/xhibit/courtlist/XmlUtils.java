package uk.gov.moj.cpp.listing.event.processor.xhibit.courtlist;

import static java.lang.String.format;
import static javax.xml.XMLConstants.W3C_XML_SCHEMA_NS_URI;
import static jakarta.xml.bind.JAXBContext.newInstance;
import static jakarta.xml.bind.Marshaller.JAXB_FORMATTED_OUTPUT;

import uk.gov.moj.cpp.listing.event.processor.xhibit.exception.GenerationFailedException;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.net.URL;

import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBElement;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Marshaller;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;

import com.google.common.annotations.VisibleForTesting;
import org.glassfish.jaxb.runtime.marshaller.NamespacePrefixMapper;
import org.slf4j.Logger;
import org.xml.sax.SAXException;

public class XmlUtils {

    private static final String COURT_SERVICE_NS = "http://www.courtservice.gov.uk/schemas/courtservice";
    private static final String BS7666_NS = "http://www.govtalk.gov.uk/people/bs7666";
    private static final String ADDRESS_AND_PERSONAL_DETAILS_NS = "http://www.govtalk.gov.uk/people/AddressAndPersonalDetails";
    private static final String XSI_NS = "http://www.w3.org/2001/XMLSchema-instance";

    /**
     * Restores the fixed XHIBIT namespace prefixes (cs/p2/apd/xsi) that the generated JAXB classes emitted under the
     * pre-EE9 jaxb2-namespace-prefix add-on. Jakarta's JAXB (org.glassfish.jaxb) otherwise emits default ns2/ns3
     * prefixes, which the consuming XHIBIT systems (and the mapper contract tests) do not expect.
     */
    private static final NamespacePrefixMapper XHIBIT_PREFIX_MAPPER = new NamespacePrefixMapper() {
        @Override
        public String getPreferredPrefix(final String namespaceUri, final String suggestion, final boolean requirePrefix) {
            switch (namespaceUri) {
                case COURT_SERVICE_NS:
                    return "cs";
                case BS7666_NS:
                    return "p2";
                case ADDRESS_AND_PERSONAL_DETAILS_NS:
                    return "apd";
                case XSI_NS:
                    return "xsi";
                default:
                    return suggestion;
            }
        }

        @Override
        public String[] getPreDeclaredNamespaceUris() {
            return new String[]{BS7666_NS, ADDRESS_AND_PERSONAL_DETAILS_NS, XSI_NS};
        }
    };

    private static DatatypeFactory datatypeFactory;

    @Inject
    @SuppressWarnings("squid:S1312")
    private Logger logger;

    @SuppressWarnings("java:S2696")
    @PostConstruct
    public void postConstruct() {
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
            jaxbMarshaller.setProperty(JAXB_FORMATTED_OUTPUT, true);
            jaxbMarshaller.setProperty("org.glassfish.jaxb.namespacePrefixMapper", XHIBIT_PREFIX_MAPPER);
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
