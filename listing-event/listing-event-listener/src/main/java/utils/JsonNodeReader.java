package utils;

import com.fasterxml.jackson.databind.JsonNode;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.UUID;

@SuppressWarnings({"squid:S2447"})
public class JsonNodeReader {
    private JsonNode jsonNode;

    private JsonNodeReader(final JsonNode jsonNode) {
        this.jsonNode = jsonNode;
    }

    public String getText(final String name) {
        if (jsonNode.get(name) == null) {
            return null;
        }
        return jsonNode.get(name).asText();
    }

    public Boolean getBoolean(final String name) {
        if (jsonNode.get(name) == null) {
            return null;
        }
        return jsonNode.get(name).asBoolean();
    }

    public UUID getUUID(final String name) {
        if (jsonNode.get(name) == null) {
            return null;
        }
        return UUID.fromString(jsonNode.get(name).asText());
    }

    public LocalDate getDate(final String name) {
        if (jsonNode.get(name) == null) {
            return null;
        }
        return LocalDate.parse(jsonNode.get(name).asText());
    }

    public ZonedDateTime getZonedDateTime(final String name) {
        if (jsonNode.get(name) == null) {
            return null;
        }
        return ZonedDateTime.parse(jsonNode.get(name).asText());
    }

    public Integer getInteger(final String name) {
        if (jsonNode.get(name) == null) {
            return null;
        }
        return Integer.parseInt(jsonNode.get(name).asText());
    }

    public JsonNodeReader get(final String name) {
        if (jsonNode.get(name) == null) {
            return null;
        }
        return new JsonNodeReader(jsonNode.get(name));
    }

    public static JsonNodeReader read(final JsonNode jsonNode) {
        return new JsonNodeReader(jsonNode);
    }
}
