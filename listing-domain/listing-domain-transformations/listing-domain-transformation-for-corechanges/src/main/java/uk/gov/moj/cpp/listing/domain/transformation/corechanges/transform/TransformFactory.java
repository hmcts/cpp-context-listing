package uk.gov.moj.cpp.listing.domain.transformation.corechanges.transform;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class TransformFactory {

    private Map<String, List<ListingEventTransformer>> transformEventMap;


    public TransformFactory() {
        transformEventMap = new HashMap<>();

        addInstance(MasterDefendantTransformer.getEventAndJsonPaths().keySet(), new MasterDefendantTransformer());
        addInstance(CourtProceedingsInitiatedEventTransformer.getEventAndJsonPaths().keySet(), new CourtProceedingsInitiatedEventTransformer());

    }

    private void addInstance(final Set<String> keySet, final ListingEventTransformer eventTransformer) {
        keySet.forEach(key -> transformEventMap.compute(key, (s, listingEventTransformers) -> {
                    if (listingEventTransformers == null) {
                        listingEventTransformers = new ArrayList<>();
                    }
                    listingEventTransformers.add(eventTransformer);
                    return listingEventTransformers;
                }
        ));
    }

    public List<ListingEventTransformer> getEventTransformer(String eventName) {
        return transformEventMap.get(eventName);
    }
}
