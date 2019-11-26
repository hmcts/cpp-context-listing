package uk.gov.moj.cpp.listing.it.util;

import static java.lang.String.format;

public class XmlEditor {

    private String xmlString;

    private XmlEditor( final String xmlString) {
        this.xmlString = xmlString;
    }

    public static XmlEditor edit(final String xmlString) {
        return new XmlEditor(xmlString);
    }

    public XmlEditor replaceElementValue(final String elementName, final String newValue) {

        final String regex = format("%s\\>.*\\<", elementName);
        final String replacement = format("%s\\>%s\\<", elementName, newValue);

        xmlString = xmlString.replaceAll(regex, replacement);

        return this;
    }

    public XmlEditor replaceAttributeValue(final String elementName, final String attributeName, final String newValue) {

        final String regex = format("%s %s=.*\\>", elementName, attributeName);
        final String replacement = format("%s %s=\"%s\"\\>", elementName, attributeName, newValue);

        xmlString = xmlString.replaceAll(regex, replacement);

        return this;
    }

    public String save() {
        return xmlString;
    }
}
