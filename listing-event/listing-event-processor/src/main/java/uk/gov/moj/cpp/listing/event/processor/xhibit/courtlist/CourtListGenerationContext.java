package uk.gov.moj.cpp.listing.event.processor.xhibit.courtlist;

public class CourtListGenerationContext {

    private PublishCourtListRequestParameters parameters;
    private CourtListMetadata metadata;

    public CourtListGenerationContext(final PublishCourtListRequestParameters parameters, final CourtListMetadata metadata) {
        this.parameters = parameters;
        this.metadata = metadata;
    }

    public PublishCourtListRequestParameters getParameters() {
        return parameters;
    }

    public CourtListMetadata getMetadata() {
        return metadata;
    }
}
