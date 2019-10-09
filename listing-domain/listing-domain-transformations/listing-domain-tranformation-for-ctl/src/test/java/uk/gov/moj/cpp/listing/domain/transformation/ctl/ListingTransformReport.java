package uk.gov.moj.cpp.listing.domain.transformation.ctl;

import uk.gov.moj.cpp.coredomain.transform.TransformQuery;
import uk.gov.moj.cpp.coredomain.transform.TransformReport;

import java.io.IOException;
import java.util.Arrays;

public class ListingTransformReport {


    public final static String[] EVENT_PACKAGES =  new String [] {
            "uk.gov.justice.listing.events"
            /*"uk.gov.moj.cpp.listing.domain.event",
            "uk.gov.moj.cpp.listing.nows.events",
            "uk.gov.justice.listing.courts",
            "uk.gov.moj.cpp.listing.subscription.events"*/} ;
    public final static String MASTER_PACKAGE_PREFIX="listingmaster";

    public TransformReport query() throws IOException {
        return query("../../..");
    }
//listing.events.new-defendant-details-updated
    public TransformReport query(String projectRoot) throws IOException {
        TransformReport report = (new TransformQuery()).compare(projectRoot,
                Arrays.asList(
//                        "/listing-command/listing-command-handler/src/raml/listing-private-event.messaging.raml",
                        "/listing-event/listing-event-listener/src/raml/listing-event-listener.messaging.raml",
                         "/listing-event/listing-event-processor/src/raml/listing-event-processor.messaging.raml"),
                EVENT_PACKAGES, MASTER_PACKAGE_PREFIX);
        System.out.println("\r\n\r\n*****************results::");
        report.printOut();
        return report;

    }
    listingmaster.uk.gov.justice.listing.events.NewDefendantDetailsUpdated olnewDefendantIpdated;
    public static void main(String[] args) throws IOException{
        (new ListingTransformReport()).query(".");
    }

}
