package uk.gov.moj.cpp.listing.event.processor.xhibit.courtlist;

import uk.gov.justice.services.messaging.JsonEnvelope;

public class CourtListGenerationContext {

    private JsonEnvelope envelope;
    private PublishCourtListRequestParameters parameters;
    private CourtListMetadata metadata;

    public CourtListGenerationContext(final JsonEnvelope envelope, final PublishCourtListRequestParameters parameters,
                                      final CourtListMetadata metadata) {
        this.envelope = envelope;
        this.parameters = parameters;
        this.metadata = metadata;
    }

    public JsonEnvelope getEnvelope() {
        return envelope;
    }

    public PublishCourtListRequestParameters getParameters() {
        return parameters;
    }

    public CourtListMetadata getMetadata() {
        return metadata;
    }
}
