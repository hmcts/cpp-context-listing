package uk.gov.moj.cpp.listing.command.hearing.state;

import uk.gov.moj.cpp.listing.command.hearing.rules.HearingDay;
import uk.gov.moj.cpp.listing.command.hearing.rules.HearingRulesState;
import uk.gov.moj.cpp.listing.command.hearing.rules.ListedCase;
import uk.gov.moj.cpp.listing.domain.JudicialRole;
import uk.gov.moj.cpp.listing.domain.NonDefaultDay;
import uk.gov.moj.cpp.listing.domain.ProsecutionCaseDefendantOffenceIds;
import uk.gov.moj.cpp.listing.domain.Type;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.BeanDescription;
import com.fasterxml.jackson.databind.DeserializationConfig;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.deser.ValueInstantiator;
import com.fasterxml.jackson.databind.deser.ValueInstantiators;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.objenesis.SpringObjenesis;

/**
 * JSON for {@link HearingRulesState}, the {@code hearing_rules_state.state} column (MBD refactor
 * P2.3.2, ADR 015). The state is made of the rules' own value classes (the JEE aggregate's,
 * {@code Serializable} but not Jackson beans: final fields, builders, {@code Optional} fields, no
 * default constructors), so the mapping is by <em>field</em>, with annotations ignored and objects
 * created without a constructor (as Java serialisation of the JEE aggregate snapshot did): every
 * field round-trips, whatever the class's accessors are. Only this column uses it.
 */
public final class HearingRulesStateCodec {

    private static final SpringObjenesis OBJENESIS = new SpringObjenesis();

    private final ObjectMapper mapper;

    public HearingRulesStateCodec() {
        final SimpleModule fieldInstantiation = new SimpleModule("hearing-rules-state") {
            @Override
            public void setupModule(final SetupContext context) {
                super.setupModule(context);
                context.addValueInstantiators(new ValueInstantiators.Base() {
                    @Override
                    public ValueInstantiator findValueInstantiator(final DeserializationConfig config, final BeanDescription beanDesc,
                                                                   final ValueInstantiator defaultInstantiator) {
                        final Class<?> type = beanDesc.getBeanClass();
                        if (type.isEnum() || type.isInterface() || type.getName().startsWith("java.")) {
                            return defaultInstantiator;
                        }
                        return new ObjenesisInstantiator(type);
                    }
                });
            }
        };
        this.mapper = JsonMapper.builder()
                .disable(MapperFeature.USE_ANNOTATIONS)
                .disable(MapperFeature.AUTO_DETECT_CREATORS)
                .visibility(PropertyAccessor.ALL, Visibility.NONE)
                .visibility(PropertyAccessor.FIELD, Visibility.ANY)
                .enable(MapperFeature.ALLOW_FINAL_FIELDS_AS_MUTATORS)
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                // Keep each ZonedDateTime's zone across a save/load (the JEE aggregate kept it, Java
                // serialisation): commands deserialise to the region zone UTC (ObjectMapperProducer),
                // so the rules' equality checks must see UTC again, not the offset Z.
                .enable(SerializationFeature.WRITE_DATES_WITH_ZONE_ID)
                .disable(SerializationFeature.FAIL_ON_EMPTY_BEANS)
                .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                .disable(DeserializationFeature.ADJUST_DATES_TO_CONTEXT_TIME_ZONE)
                .serializationInclusion(JsonInclude.Include.NON_NULL)
                .addModule(new JavaTimeModule().disable(com.fasterxml.jackson.datatype.jsr310.JavaTimeFeature.NORMALIZE_DESERIALIZED_ZONE_ID))
                .addModule(new Jdk8Module())
                .addModule(fieldInstantiation)
                .build();
    }

    public String toJson(final HearingRulesState state) {
        try {
            return mapper.writeValueAsString(Document.of(state));
        } catch (final IOException e) {
            throw new UncheckedIOException("Cannot write the hearing rules state of " + state.hearingId(), e);
        }
    }

    public HearingRulesState fromJson(final String json) {
        try {
            return mapper.readValue(json, Document.class).toState();
        } catch (final IOException e) {
            throw new UncheckedIOException("Cannot read a hearing rules state", e);
        }
    }

    /** Any other rules state document (Case, Application, SeedHearing; P2.3.3), mapped by field as above. */
    public String writeDocument(final Object document) {
        try {
            return mapper.writeValueAsString(document);
        } catch (final IOException e) {
            throw new UncheckedIOException("Cannot write a rules state document " + document.getClass().getSimpleName(), e);
        }
    }

    /** Reads a document written by {@link #writeDocument}. */
    public <T> T readDocument(final String json, final Class<T> type) {
        try {
            return mapper.readValue(json, type);
        } catch (final IOException e) {
            throw new UncheckedIOException("Cannot read a rules state document " + type.getSimpleName(), e);
        }
    }

    /** The stored document: the state's fields by name. */
    @SuppressWarnings({"java:S1104", "PMD.BeanMembersShouldSerialize"})
    static final class Document {
        UUID hearingId;
        boolean duplicate;
        boolean deleted;
        Type type;
        Integer estimatedMinutes;
        UUID courtRoomId;
        List<JudicialRole> judiciary;
        List<NonDefaultDay> nonDefaultDays;
        List<HearingDay> hearingDays;
        List<ProsecutionCaseDefendantOffenceIds> prosecutionCaseDefendantOffenceIds;
        Map<UUID, List<UUID>> prosecutionCaseDefendants;
        Map<UUID, List<UUID>> applicationOffenceIds;
        List<UUID> confirmedCourtApplicationIds;
        boolean updateSlot;
        boolean hasAdjournmentDate;
        List<ListedCase> unAllocatedListedCases;

        static Document of(final HearingRulesState s) {
            final Document d = new Document();
            d.hearingId = s.hearingId();
            d.duplicate = s.duplicate();
            d.deleted = s.deleted();
            d.type = s.type();
            d.estimatedMinutes = s.estimatedMinutes();
            d.courtRoomId = s.courtRoomId();
            d.judiciary = s.judiciary();
            d.nonDefaultDays = s.nonDefaultDays();
            d.hearingDays = s.hearingDays();
            d.prosecutionCaseDefendantOffenceIds = s.prosecutionCaseDefendantOffenceIds();
            d.prosecutionCaseDefendants = s.prosecutionCaseDefendants();
            d.applicationOffenceIds = s.applicationOffenceIds();
            d.confirmedCourtApplicationIds = s.confirmedCourtApplicationIds();
            d.updateSlot = s.updateSlot();
            d.hasAdjournmentDate = s.hasAdjournmentDate();
            d.unAllocatedListedCases = s.unAllocatedListedCases();
            return d;
        }

        HearingRulesState toState() {
            return new HearingRulesState(hearingId, duplicate, deleted, type, estimatedMinutes, courtRoomId, judiciary, nonDefaultDays,
                    hearingDays, prosecutionCaseDefendantOffenceIds, prosecutionCaseDefendants, applicationOffenceIds,
                    confirmedCourtApplicationIds, updateSlot, hasAdjournmentDate, unAllocatedListedCases);
        }
    }

    /** Creates beans without calling a constructor (their fields are then set one by one). */
    private static final class ObjenesisInstantiator extends ValueInstantiator.Base {

        ObjenesisInstantiator(final Class<?> type) {
            super(type);
        }

        @Override
        public boolean canCreateUsingDefault() {
            return true;
        }

        @Override
        public boolean canInstantiate() {
            return true;
        }

        @Override
        public Object createUsingDefault(final DeserializationContext ctxt) {
            return OBJENESIS.newInstance(getValueClass());
        }
    }
}
