package uk.gov.moj.cpp.listing.domain.transformation;

import static com.google.common.collect.Lists.newArrayList;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class EventMapper {

    private EventMapper() {
    }

    private static Map<String, List<String>> EVENT_MAP = new HashMap();

    static {
        EVENT_MAP.put("listing.events.case-update-defendant-proceedings-updated", newArrayList(
                "$.hearingId"));

        EVENT_MAP.put("listing.events.case-resulted-defendant-proceedings-updated", newArrayList(
                "$.hearingIds[-1:]"));

    }

    public static Collection getEventNames() {
        return EVENT_MAP.keySet();
    }

    public static List<String> getMappedJsonPaths(String eventName) {
        return EVENT_MAP.get(eventName);
    }

}
