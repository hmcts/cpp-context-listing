package uk.gov.moj.cpp.listing.utils;

import static com.google.common.base.Charsets.UTF_8;
import static com.google.common.io.Resources.getResource;
import static java.lang.String.format;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.List;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;

public class CSVFileReader {

    public List<CSVRecord> readData(final String filePath) {
        try {
            return CSVParser.parse(getResource(filePath), UTF_8,
                    CSVFormat.EXCEL.withHeader().withSkipHeaderRecord().withIgnoreEmptyLines().withTrim())
                    .getRecords();
        } catch (final IOException e) {
            throw new UncheckedIOException(format("Error reading file contents %s", filePath), e);
        }
    }
}
