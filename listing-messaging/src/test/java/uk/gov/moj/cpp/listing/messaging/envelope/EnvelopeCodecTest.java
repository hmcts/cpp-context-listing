package uk.gov.moj.cpp.listing.messaging.envelope;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;

import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.json.JsonValue;
import org.junit.jupiter.api.Test;

class EnvelopeCodecTest {

    private final EnvelopeCodec codec = new EnvelopeCodec();

    @Test
    void parsesTheCapturedLegacyMessage() {
        final Envelope envelope = codec.parse(legacyMessage());

        final Metadata metadata = envelope.metadata();
        assertThat(metadata.id()).isEqualTo(UUID.fromString("d41e6c89-449d-4c96-828e-0a472b18763a"));
        assertThat(metadata.name()).isEqualTo("public.listing.created-listing-note");
        assertThat(metadata.userId()).contains("e8e5bd19-e02c-46be-bc20-d4037012052d");
        assertThat(metadata.createdAt()).contains(ZonedDateTime.parse("2026-11-19T12:20:54.880Z"));
        assertThat(metadata.causation()).containsExactly(UUID.fromString("f97bce3f-afa9-48fe-a0b5-736d6781b319"));
        assertThat(metadata.streamId()).contains(UUID.fromString("72979ff7-7957-3499-8732-deabcec55b65"));
        assertThat(metadata.version()).isEmpty();
        assertThat(envelope.payload().keySet()).containsExactly("id", "date", "courtRoomId", "note");
        assertThat(envelope.payload().getString("note")).isEqualTo("p22 capture");
    }

    @Test
    void reserialisesTheCapturedLegacyMessageByteForByte() {
        final String legacy = legacyMessage();

        assertThat(codec.toJson(codec.parse(legacy))).isEqualTo(legacy);
    }

    @Test
    void writesMetadataFirstThenPayloadFieldsAtTopLevelAndDropsNullMembers() {
        final Metadata metadata = Metadata.builder()
                .withId(UUID.fromString("11111111-1111-1111-1111-111111111111"))
                .withName("public.listing.x")
                .build();
        final JsonObject payload = Json.createObjectBuilder()
                .add("b", 1)
                .add("a", Json.createArrayBuilder().add("x"))
                .add("gone", JsonValue.NULL)
                .add("decimal", 1.5)
                .build();

        assertThat(codec.toJson(new Envelope(metadata, payload))).isEqualTo(
                "{\"_metadata\":{\"id\":\"11111111-1111-1111-1111-111111111111\",\"name\":\"public.listing.x\"},"
                        + "\"b\":1,\"a\":[\"x\"],\"decimal\":1.5}");
    }

    @Test
    void anEnvelopeWithoutPayloadFieldsHasAnEmptyPayload() {
        final Envelope envelope = codec.parse("{\"_metadata\":{\"id\":\"11111111-1111-1111-1111-111111111111\",\"name\":\"n\"}}");

        assertThat(envelope.payload()).isEmpty();
    }

    @Test
    void rejectsBodiesThatAreNotFrameworkEnvelopes() {
        assertThatThrownBy(() -> codec.parse("not json")).isInstanceOf(EnvelopeFormatException.class);
        assertThatThrownBy(() -> codec.parse("[1]")).isInstanceOf(EnvelopeFormatException.class);
        assertThatThrownBy(() -> codec.parse("{\"id\":1}")).isInstanceOf(EnvelopeFormatException.class)
                .hasMessageContaining("_metadata");
        assertThatThrownBy(() -> codec.parse("{\"_metadata\":{\"name\":\"n\"}}")).isInstanceOf(EnvelopeFormatException.class)
                .hasMessage("Missing id field");
        assertThatThrownBy(() -> codec.parse("{\"_metadata\":{\"id\":\"11111111-1111-1111-1111-111111111111\"}}"))
                .isInstanceOf(EnvelopeFormatException.class).hasMessage("Missing name field");
        assertThatThrownBy(() -> codec.parse("{\"_metadata\":{\"id\":\"11111111-1111-1111-1111-111111111111\",\"name\":\"\"}}"))
                .isInstanceOf(EnvelopeFormatException.class).hasMessage("Name field cannot be empty");
        assertThatThrownBy(() -> codec.parse("{\"_metadata\":{\"id\":\"x\",\"name\":\"n\"}}"))
                .isInstanceOf(EnvelopeFormatException.class).hasMessageContaining("not a UUID");
    }

    @Test
    void metadataBuilderWritesTheFrameworkPathsAndKeepsOtherFields() {
        final Metadata source = codec.parse(legacyMessage()).metadata();

        final Metadata built = Metadata.from(source)
                .withSessionId("s")
                .withClientCorrelationId("c")
                .withVersion(7)
                .withSource("listing")
                .withCausation(List.of(UUID.fromString("22222222-2222-2222-2222-222222222222")))
                .build();

        assertThat(built.asJsonObject().keySet())
                .containsExactly("name", "context", "id", "createdAt", "causation", "stream", "correlation", "source");
        assertThat(built.userId()).contains("e8e5bd19-e02c-46be-bc20-d4037012052d");
        assertThat(built.sessionId()).contains("s");
        assertThat(built.clientCorrelationId()).contains("c");
        assertThat(built.asJsonObject().getJsonObject("correlation").getString("client")).isEqualTo("c");
        assertThat(built.streamId()).isEqualTo(source.streamId());
        assertThat(built.version()).contains(7L);
        assertThat(built.source()).contains("listing");
        assertThat(built.causation()).containsExactly(UUID.fromString("22222222-2222-2222-2222-222222222222"));
        assertThat(built.createdAt()).isEqualTo(source.createdAt());
    }

    @Test
    void createdAtIsWrittenInTheFrameworkFormatInUtc() {
        final Metadata metadata = Metadata.builder()
                .withId(UUID.randomUUID())
                .withName("n")
                .createdAt(ZonedDateTime.parse("2026-07-01T10:15:30.123+01:00[Europe/London]"))
                .build();

        assertThat(metadata.asJsonObject().getString("createdAt")).isEqualTo("2026-07-01T09:15:30.123Z");
    }

    static String legacyMessage() {
        try (InputStream in = EnvelopeCodecTest.class.getResourceAsStream("/legacy/public.listing.created-listing-note.json")) {
            return new String(in.readAllBytes(), StandardCharsets.UTF_8).trim();
        } catch (final IOException e) {
            throw new IllegalStateException(e);
        }
    }
}
