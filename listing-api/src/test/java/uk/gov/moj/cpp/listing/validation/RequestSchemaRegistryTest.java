package uk.gov.moj.cpp.listing.validation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.everit.json.schema.Schema;
import org.everit.json.schema.ValidationException;
import org.json.JSONObject;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class RequestSchemaRegistryTest {

    private static final String CREATE_NOTE = "application/vnd.listing.command.create-listing-note+json";
    private static RequestSchemaRegistry registry;

    @BeforeAll
    static void load() {
        // Compiles every schema named in the spec; fails here if a file or a $ref is missing.
        registry = new RequestSchemaRegistry("openapi/listing-api.openapi.yml");
    }

    @Test
    void everyRequestMediaTypeWithARamlSchemaIsMapped() {
        // 31 command media types with a RAML schema (duplicate-unallocated-hearing is !!null)
        // plus the 2 query POSTs.
        assertThat(registry.schemaFileByMediaType()).hasSize(33);
        assertThat(registry.schemaFor("application/vnd.listing.duplicate-unallocated-hearing+json")).isEmpty();
    }

    @Test
    void mediaTypesWhoseSchemaFileHasAnotherNameUseTheRamlInclude() {
        assertThat(registry.schemaFileByMediaType())
                .containsEntry("application/vnd.listing.command.edit-listing-note+json", "listing.command.edit-note-for-listing.json")
                .containsEntry("application/vnd.listing.next-hearings-v2+json", "listing.list-next-hearings-v2.json")
                .containsEntry("application/vnd.listing.related-hearing+json", "listing.update-related-hearing.json")
                .containsEntry("application/vnd.listing.delete-hearing+json", "listing.command.delete-hearing.json");
    }

    @Test
    void deleteHearingTakesAnEmptyBodyTheHearingIsInThePath() {
        // As the RAML schema (additionalProperties false, no properties): the JEE framework rejected a
        // body naming the hearingId too. The P2.3.2 Spring IT first sent {"hearingId":...} and got 400.
        final Schema schema = registry.schemaFor("application/vnd.listing.delete-hearing+json").orElseThrow();
        schema.validate(new JSONObject("{}"));
        assertThatThrownBy(() -> schema.validate(new JSONObject("""
                {"hearingId":"a299cf49-d14a-496f-895f-59c46859b4a9"}""")))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("extraneous key [hearingId]");
    }

    @Test
    void lookupIsCaseInsensitive() {
        assertThat(registry.schemaFor(CREATE_NOTE.toUpperCase())).isPresent();
    }

    @Test
    void remoteRefsResolveFromTheClasspathCatalog() {
        final Schema schema = registry.schemaFor(CREATE_NOTE).orElseThrow();
        // courtRoomId → http://justice.gov.uk/core/courts/courtsDefinitions.json#/definitions/uuid (criminal-court-public-model)
        schema.validate(new JSONObject("""
                {"hearingDate":"2020-01-09","courtRoomId":"a299cf49-d14a-496f-895f-59c46859b4a9","noteDescription":"n"}"""));
        assertThatThrownBy(() -> schema.validate(new JSONObject("""
                {"hearingDate":"2020-01-09","courtRoomId":"not-a-uuid","noteDescription":"n"}""")))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("courtRoomId");
    }

    @Test
    void catalogNeverFetchesUnknownIdsRemotely() {
        final LegacySchemaCatalog catalog = LegacySchemaCatalog.loadFromClasspath();
        assertThat(catalog.contains("http://justice.gov.uk/core/courts/courtsDefinitions.json#/definitions/uuid")).isTrue();
        assertThat(catalog.contains("http://justice.gov.uk/domain/core/common/definitions.json")).isTrue();
        assertThatThrownBy(() -> catalog.get("http://example.com/not-there.json"))
                .hasMessageContaining("not in the classpath schema catalog");
    }
}
