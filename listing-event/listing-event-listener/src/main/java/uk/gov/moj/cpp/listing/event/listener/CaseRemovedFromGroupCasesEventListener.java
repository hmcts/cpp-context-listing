package uk.gov.moj.cpp.listing.event.listener;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static java.util.stream.Collectors.toList;
import static uk.gov.justice.services.core.annotation.Component.EVENT_LISTENER;

import uk.gov.justice.listing.events.CaseRemovedFromGroupCases;
import uk.gov.justice.listing.events.Defendant;
import uk.gov.justice.listing.events.ListedCase;
import uk.gov.justice.listing.events.Offence;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.listing.event.service.HearingSearchSyncService;
import uk.gov.moj.cpp.listing.persistence.entity.Hearing;
import uk.gov.moj.cpp.listing.persistence.repository.HearingRepository;

import java.util.List;
import java.util.Optional;

import javax.inject.Inject;
import javax.transaction.Transactional;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@ServiceComponent(EVENT_LISTENER)
public class CaseRemovedFromGroupCasesEventListener {

    private static final String LISTED_CASES_FIELD = "listedCases";
    private static final TypeReference<List<ListedCase>> LISTED_CASE_TYPE = new TypeReference<List<ListedCase>>() {
    };

    @Inject
    private JsonObjectToObjectConverter jsonObjectToObjectConverter;

    @Inject
    private HearingRepository hearingRepository;

    @Inject
    private ObjectMapper objectMapper;

    @Inject
    private HearingSearchSyncService hearingSearchSyncService;

    @Transactional
    @Handles("listing.events.case-removed-from-group-cases")
    public void caseRemovedFromGroupCases(final JsonEnvelope envelope) {
        final CaseRemovedFromGroupCases caseRemovedFromGroupCases =
                jsonObjectToObjectConverter.convert(envelope.payloadAsJsonObject(), CaseRemovedFromGroupCases.class);

        final Hearing hearing = hearingRepository.findBy(caseRemovedFromGroupCases.getHearingId());
        final ListedCase removedCase = caseRemovedFromGroupCases.getRemovedCase();
        final ListedCase newGroupMasterCase = nonNull(caseRemovedFromGroupCases.getNewGroupMaster()) ? caseRemovedFromGroupCases.getNewGroupMaster() : null;

        // The event cases are rebuilt from progression with every restriction flag false, and replace the
        // stored cases - so keep the court list restrictions already applied to any case that is stored
        final List<ListedCase> storedCases = getStoredListedCases(hearing);
        final List<ListedCase> casesToUpdate = (nonNull(newGroupMasterCase) ? asList(removedCase, newGroupMasterCase) : asList(removedCase)).stream()
                .map(listedCase -> keepCourtListRestrictions(listedCase, storedCases))
                .collect(toList());

        hearingSearchSyncService.syncEntity(hearing, casesToUpdate);
    }

    private List<ListedCase> getStoredListedCases(final Hearing hearing) {
        if (isNull(hearing) || isNull(hearing.getProperties()) || isNull(hearing.getProperties().get(LISTED_CASES_FIELD))) {
            return emptyList();
        }
        final JsonNode listedCases = hearing.getProperties().get(LISTED_CASES_FIELD);
        return objectMapper.convertValue(listedCases, LISTED_CASE_TYPE);
    }

    private ListedCase keepCourtListRestrictions(final ListedCase listedCase, final List<ListedCase> storedCases) {
        final Optional<ListedCase> storedCase = storedCases.stream()
                .filter(stored -> stored.getId().equals(listedCase.getId()))
                .findFirst();
        if (!storedCase.isPresent()) {
            return listedCase;
        }
        return ListedCase.listedCase()
                .withValuesFrom(listedCase)
                .withRestrictFromCourtList(isRestricted(listedCase.getRestrictFromCourtList(), storedCase.get().getRestrictFromCourtList()))
                .withDefendants(isNull(listedCase.getDefendants()) ? null : listedCase.getDefendants().stream()
                        .map(defendant -> keepDefendantRestrictions(defendant, storedCase.get().getDefendants()))
                        .collect(toList()))
                .build();
    }

    private Defendant keepDefendantRestrictions(final Defendant defendant, final List<Defendant> storedDefendants) {
        final Optional<Defendant> storedDefendant = ofNullableList(storedDefendants).stream()
                .filter(stored -> stored.getId().equals(defendant.getId()))
                .findFirst();
        if (!storedDefendant.isPresent()) {
            return defendant;
        }
        return Defendant.defendant()
                .withValuesFrom(defendant)
                .withRestrictFromCourtList(isRestricted(defendant.getRestrictFromCourtList(), storedDefendant.get().getRestrictFromCourtList()))
                .withOffences(isNull(defendant.getOffences()) ? null : defendant.getOffences().stream()
                        .map(offence -> keepOffenceRestriction(offence, storedDefendant.get().getOffences()))
                        .collect(toList()))
                .build();
    }

    private Offence keepOffenceRestriction(final Offence offence, final List<Offence> storedOffences) {
        final boolean storedOffenceRestricted = ofNullableList(storedOffences).stream()
                .filter(stored -> stored.getId().equals(offence.getId()))
                .anyMatch(stored -> Boolean.TRUE.equals(stored.getRestrictFromCourtList()));
        if (!storedOffenceRestricted) {
            return offence;
        }
        return Offence.offence()
                .withValuesFrom(offence)
                .withRestrictFromCourtList(true)
                .build();
    }

    private static Boolean isRestricted(final Boolean restrictFromCourtList, final Boolean storedRestrictFromCourtList) {
        return Boolean.TRUE.equals(storedRestrictFromCourtList) ? Boolean.TRUE : restrictFromCourtList;
    }

    private static <T> List<T> ofNullableList(final List<T> list) {
        return isNull(list) ? emptyList() : list;
    }
}