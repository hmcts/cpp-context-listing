package uk.gov.moj.cpp.listing.domain.xhibit;

import java.time.LocalDate;

import jakarta.xml.bind.annotation.adapters.XmlAdapter;

/**
 * {@code xs:date} ↔ {@link LocalDate} for the generated XHIBIT classes (binding.xjb). The Jakarta
 * copy of {@code listing-domain-xhibit}'s {@code LocalDateAdapter} (same package and behaviour).
 */
public class LocalDateAdapter extends XmlAdapter<String, LocalDate> {

    @Override
    public LocalDate unmarshal(final String inputDate) {
        if (inputDate == null) {
            return null;
        }
        return LocalDate.parse(inputDate);
    }

    @Override
    public String marshal(final LocalDate inputDate) {
        if (inputDate == null) {
            return null;
        }
        return inputDate.toString();
    }
}
