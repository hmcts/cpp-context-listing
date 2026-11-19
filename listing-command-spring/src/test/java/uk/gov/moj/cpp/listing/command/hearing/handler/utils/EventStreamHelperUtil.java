package uk.gov.moj.cpp.listing.command.hearing.handler.utils;

import static org.mockito.Mockito.times;

import uk.gov.moj.cpp.listing.command.hearing.handler.testsupport.EventStream;
import uk.gov.moj.cpp.listing.messaging.envelope.Envelope;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

public class EventStreamHelperUtil {

    private EventStreamHelperUtil() {
    }

    public static List<Envelope> verifyAndGetEvents(final EventStream eventStream, int times) {
        ArgumentCaptor<Stream> argumentCaptor = ArgumentCaptor.forClass(Stream.class);
        ((EventStream) Mockito.verify(eventStream, times(times))).append((Stream) argumentCaptor.capture());

        List<Envelope> events = new ArrayList<>();
        for (Stream stream : argumentCaptor.getAllValues()) {
            events.addAll((List<Envelope>) stream.collect(Collectors.toList()));
        }

        return events;
    }
}
