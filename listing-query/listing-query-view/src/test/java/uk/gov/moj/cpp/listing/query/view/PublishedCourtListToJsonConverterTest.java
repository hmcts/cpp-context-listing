package uk.gov.moj.cpp.listing.query.view;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static uk.gov.justice.listing.event.PublishCourtListType.WARN;

import uk.gov.justice.listing.event.PublishCourtListType;
import uk.gov.moj.cpp.listing.persistence.repository.courtlist.PublishedCourtList;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import javax.json.Json;
import javax.json.JsonObject;

import com.fasterxml.jackson.databind.JsonNode;
import com.vladmihalcea.hibernate.type.json.internal.JacksonUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class PublishedCourtListToJsonConverterTest {

    private final UUID courtCentreId = UUID.randomUUID();
    private final PublishCourtListType publishCourtListType = WARN;
    private final LocalDate startDate = LocalDate.now();
    private final JsonNode courtListJson = JacksonUtil.toJsonNode("{}");
    private final ZonedDateTime lastUpdated = ZonedDateTime.now();
    private final ZonedDateTime lastExported = ZonedDateTime.now();

    private PublishedCourtList publishedCourtList;

    @InjectMocks
    private PublishedCourtListToJsonConverter publishedCourtListToJsonConverter;

    @BeforeEach
    public void before() {
        publishedCourtList = new PublishedCourtList(courtCentreId, publishCourtListType, startDate, courtListJson, lastUpdated, lastExported, null);
    }

    @Test
    public void shouldConvertListToJson() {

        final List<PublishedCourtList> publishedCourtLists = new ArrayList<>();

        publishedCourtLists.add(publishedCourtList);

        final JsonObject jsonObject = publishedCourtListToJsonConverter.convert(publishedCourtLists);

        final JsonObject courtList1 = jsonObject.getJsonArray("publishedCourtLists").getJsonObject(0);

        assertCourtList(courtList1);
    }

    @Test
    public void shouldConvertObjectToJson() {

        final JsonObject courtListJson = publishedCourtListToJsonConverter.convert(publishedCourtList);

        assertCourtList(courtListJson);
    }

    private void assertCourtList(final JsonObject courtListJson) {
        assertThat(courtListJson.getString("courtCentreId"), is(courtCentreId.toString()));
        assertThat(courtListJson.getString("publishCourtListType"), is(WARN.name()));
        assertThat(courtListJson.getString("startDate"), is(startDate.toString()));
        assertThat(courtListJson.getJsonObject("courtListJson"), is(Json.createObjectBuilder().build()));
        assertThat(courtListJson.getString("lastUpdated"), is(lastUpdated.toString()));
        assertThat(courtListJson.getString("lastExported"), is(lastExported.toString()));
    }

}
