package uk.gov.moj.cpp.listing.persistence.repository;

import static java.util.UUID.randomUUID;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.test.utils.core.random.RandomGenerator.STRING;
import static uk.gov.moj.cpp.listing.persistence.repository.JsonEntityFinder.using;

import uk.gov.moj.cpp.listing.persistence.entity.Hearing;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.function.Function;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.vladmihalcea.hibernate.type.json.internal.JacksonUtil;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class JsonEntityFinderTest {

    private static final UUID HEARING_ID = randomUUID();
    private static final String JSON_PATH = "path";
    private static final String UUID_FIELD = "uuid";
    private static final String ARRAY_FIELD = "array";
    private static final String STRING_FIELD = "string";
    private static final String LOCAL_DATE_FIELD = "localDate";
    private static final String REMOVE_FIELD = "removeField";
    private static final String TEST_REMOVE_VALUE = "removeValue";
    private static final String BOOLEAN_FIELD = "boolean";
    private static final UUID TEST_UUID = randomUUID();
    private static final String TEST_STRING = STRING.next();
    private static final String TEST_ARRAY = "[\"Item1\", \"Item2\"]";
    private static final LocalDate TEST_LOCAL_DATE = LocalDate.now();
    private static final boolean TEST_BOOLEAN = true;
    private static final UUID EXPECTED_UUID = randomUUID();
    private static final String EXPECTED_STRING = STRING.next();
    private static final String EXPECTED_ARRAY = "[\"UpdatedItem1\", \"UpdatedItem2\"]";
    private static final LocalDate EXPECTED_LOCAL_DATE = LocalDate.now();
    private static final boolean EXPECTED_BOOLEAN = false;
    private static final List<String> TEST_LIST = Arrays.asList("UpdatedItem1", "UpdatedItem2");
    private static final String TEST_JSON_WITH_PATH =
            "{ \"path\": { " +
                    "\"" + ARRAY_FIELD + "\": " + TEST_ARRAY + "," +
                    "\"" + LOCAL_DATE_FIELD + "\": \"" + TEST_LOCAL_DATE + "\"," +
                    "\"" + UUID_FIELD + "\": \"" + TEST_UUID + "\"," +
                    "\"" + STRING_FIELD + "\": \"" + TEST_STRING + "\"," +
                    "\"" + BOOLEAN_FIELD + "\": " + TEST_BOOLEAN + "," +
                    "\"" + REMOVE_FIELD + "\": \"" + TEST_REMOVE_VALUE + "\"" +
                    "} " +
                    "}";
    private static final String EXPECTED_JSON_WITH_PATH =
            "{ \"path\": { " +
                    "\"" + ARRAY_FIELD + "\": " + EXPECTED_ARRAY + "," +
                    "\"" + LOCAL_DATE_FIELD + "\": \"" + EXPECTED_LOCAL_DATE + "\"," +
                    "\"" + UUID_FIELD + "\": \"" + EXPECTED_UUID + "\"," +
                    "\"" + STRING_FIELD + "\": \"" + EXPECTED_STRING + "\"," +
                    "\"" + BOOLEAN_FIELD + "\": " + EXPECTED_BOOLEAN +
                    "} " +
                    "}";
    private static final String TEST_JSON_WITHOUT_PATH =
            "{ " +
                    "\"" + ARRAY_FIELD + "\": " + TEST_ARRAY + "," +
                    "\"" + LOCAL_DATE_FIELD + "\": \"" + TEST_LOCAL_DATE + "\"," +
                    "\"" + UUID_FIELD + "\": \"" + TEST_UUID + "\"," +
                    "\"" + STRING_FIELD + "\": \"" + EXPECTED_STRING + "\"," +
                    "\"" + BOOLEAN_FIELD + "\": " + TEST_BOOLEAN + "," +
                    "\"" + REMOVE_FIELD + "\": \"" + TEST_REMOVE_VALUE + "\"" +
                    "} ";
    private static final String EXPECTED_JSON_WITHOUT_PATH =
            "{ " +
                    "\"" + ARRAY_FIELD + "\": " + EXPECTED_ARRAY + "," +
                    "\"" + LOCAL_DATE_FIELD + "\": \"" + EXPECTED_LOCAL_DATE + "\"," +
                    "\"" + UUID_FIELD + "\": \"" + EXPECTED_UUID + "\"," +
                    "\"" + STRING_FIELD + "\": \"" + EXPECTED_STRING + "\"," +
                    "\"" + BOOLEAN_FIELD + "\": " + EXPECTED_BOOLEAN +
                    "} ";
    private static final ObjectNode EXPECTED_JSON_NODE_WITH_PATH = (ObjectNode) JacksonUtil.toJsonNode(EXPECTED_JSON_WITH_PATH);
    private static final ObjectNode EXPECTED_JSON_NODE_WITHOUT_PATH = (ObjectNode) JacksonUtil.toJsonNode(EXPECTED_JSON_WITHOUT_PATH);

    @Mock
    HearingRepository hearingRepository;

    @Mock
    Hearing hearing;

    @Test
    public void shouldUpdateAndRemoveJsonPropertiesWithPath() {
        ObjectNode hearingProperties = (ObjectNode) JacksonUtil.toJsonNode(TEST_JSON_WITH_PATH);
        when(hearingRepository.findBy(HEARING_ID)).thenReturn(hearing);
        when(hearing.getProperties()).thenReturn(hearingProperties);

        final TypeReference<List<String>> typeRef = new TypeReference<List<String>>() {
        };

        using(hearingRepository)
                .find(HEARING_ID, JSON_PATH)
                .putSubList(ARRAY_FIELD, typeRef, getListListFunction())
                .put(UUID_FIELD, EXPECTED_UUID)
                .put(LOCAL_DATE_FIELD, EXPECTED_LOCAL_DATE)
                .put(BOOLEAN_FIELD, EXPECTED_BOOLEAN)
                .put(STRING_FIELD, EXPECTED_STRING)
                .remove(REMOVE_FIELD)
                .save();

        final ArgumentCaptor<ObjectNode> objectNodeCaptur =
                ArgumentCaptor.forClass(ObjectNode.class);

        verify(hearing).setProperties(objectNodeCaptur.capture());
        ObjectNode updatedProperties = objectNodeCaptur.getValue();
        assertThat(updatedProperties, equalTo(EXPECTED_JSON_NODE_WITH_PATH));
        verify(hearingRepository).save(hearing);
    }

    @Test
    public void shouldUpdateAndRemoveSubJsonPropertiesWithoutPath() {
        ObjectNode hearingProperties = (ObjectNode) JacksonUtil.toJsonNode(TEST_JSON_WITHOUT_PATH);
        when(hearingRepository.findBy(HEARING_ID)).thenReturn(hearing);
        when(hearing.getProperties()).thenReturn(hearingProperties);

        final TypeReference<List<String>> typeRef = new TypeReference<List<String>>() {
        };

        using(hearingRepository)
                .find(HEARING_ID)
                .putSubList(ARRAY_FIELD, typeRef, getListListFunction())
                .put(UUID_FIELD, EXPECTED_UUID)
                .put(LOCAL_DATE_FIELD, EXPECTED_LOCAL_DATE)
                .put(BOOLEAN_FIELD, EXPECTED_BOOLEAN)
                .put(STRING_FIELD, EXPECTED_STRING)
                .remove(REMOVE_FIELD)
                .save();


        final ArgumentCaptor<ObjectNode> objectNodeCaptur =
                ArgumentCaptor.forClass(ObjectNode.class);

        verify(hearing).setProperties(objectNodeCaptur.capture());
        ObjectNode updatedProperties = objectNodeCaptur.getValue();
        assertThat(updatedProperties, equalTo(EXPECTED_JSON_NODE_WITHOUT_PATH));
        verify(hearingRepository).save(hearing);
    }

    private Function<List<String>, List<String>> getListListFunction() {
        return o -> TEST_LIST;
    }
}