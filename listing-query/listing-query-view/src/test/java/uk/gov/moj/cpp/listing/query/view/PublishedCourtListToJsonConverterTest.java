package uk.gov.moj.cpp.listing.query.view;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static uk.gov.justice.listing.event.PublishCourtListType.WARN;

import uk.gov.justice.listing.event.PublishCourtListType;
import uk.gov.moj.cpp.listing.persistence.repository.courtlist.PublishedCourtList;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import javax.json.JsonObject;

import com.fasterxml.jackson.databind.JsonNode;
import com.vladmihalcea.hibernate.type.json.internal.JacksonUtil;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class PublishedCourtListToJsonConverterTest {

    @InjectMocks
    private PublishedCourtListToJsonConverter publishedCourtListToJsonConverter;

    @Test
    public void shouldConvertToJson() {

        final UUID courtCentreId = UUID.randomUUID();
        final PublishCourtListType publishCourtListType = WARN;
        final LocalDate startDate = LocalDate.now();
        final JsonNode courtListJson = JacksonUtil.toJsonNode("{}");
        final ZonedDateTime lastUpdated = ZonedDateTime.now();
        final ZonedDateTime lastExported = ZonedDateTime.now();

        final List<PublishedCourtList> publishedCourtLists = new ArrayList<>();

        publishedCourtLists.add(new PublishedCourtList(courtCentreId,publishCourtListType, startDate, courtListJson, lastUpdated, lastExported));

        final JsonObject jsonObject = publishedCourtListToJsonConverter.convert(publishedCourtLists);

        JsonObject courtList1=jsonObject.getJsonArray("publishedCourtLists").getJsonObject(0);

        assertThat(courtList1.getString("courtCentreId"), is(courtCentreId.toString()));
        assertThat(courtList1.getString("publishCourtListType"), is(WARN.name()));
        assertThat(courtList1.getString("startDate"), is(startDate.toString()));
        assertThat(courtList1.getString("startDate"), is(startDate.toString()));
        assertThat(courtList1.getString("lastExported"), is(lastExported.toString()));
    }
}
