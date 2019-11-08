package uk.gov.moj.cpp.listing.event.processor.xhibit.courtlist;

import java.util.HashMap;
import java.util.Map;

import com.sun.xml.bind.marshaller.NamespacePrefixMapper; //NOSONAR

public class XhibitNamespacePrefixMapper extends NamespacePrefixMapper {

    private Map<String, String> namespaceMap = new HashMap<>();

    public XhibitNamespacePrefixMapper() {
        namespaceMap.put("http://www.w3.org/2001/XMLSchema-instance", "xsi");
        namespaceMap.put("http://www.courtservice.gov.uk/schemas/courtservice", "cs");
        namespaceMap.put("http://www.govtalk.gov.uk/people/AddressAndPersonalDetails", "apd");
        namespaceMap.put("http://www.govtalk.gov.uk/people/bs7666", "p2");
    }

    @Override
    public String getPreferredPrefix(final String namespaceUri, final String suggestion, boolean requirePrefix) {
        return namespaceMap.getOrDefault(namespaceUri, suggestion);
    }
}
