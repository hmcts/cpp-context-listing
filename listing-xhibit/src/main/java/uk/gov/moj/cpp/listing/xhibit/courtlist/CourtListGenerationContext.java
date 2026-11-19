package uk.gov.moj.cpp.listing.xhibit.courtlist;

/**
 * What the court-list mappers need about the export being generated. Port of the JEE
 * {@code CourtListGenerationContext} without the framework envelope (no mapper used it).
 */
public class CourtListGenerationContext {

    private final PublishCourtListRequestParameters parameters;
    private final CourtListMetadata metadata;

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
