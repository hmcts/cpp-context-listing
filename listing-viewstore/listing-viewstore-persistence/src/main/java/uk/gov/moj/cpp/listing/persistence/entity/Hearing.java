package uk.gov.moj.cpp.listing.persistence.entity;

import static java.util.Objects.isNull;
import static org.apache.commons.collections.CollectionUtils.isEmpty;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.Table;

import com.fasterxml.jackson.databind.JsonNode;
import com.vladmihalcea.hibernate.type.json.JsonNodeBinaryType;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;
import org.hibernate.annotations.TypeDefs;
import org.hibernate.type.PostgresUUIDType;


@SuppressWarnings({"squid:S00107","squid:S2384","pmd:BeanMembersShouldSerialize"})
@Entity
@Table(name = "hearing")
@TypeDefs({
        @TypeDef(
                name = "jsonb-node",
                typeClass = JsonNodeBinaryType.class
        ),
        @TypeDef(
                name = "pg-uuid",
                typeClass = PostgresUUIDType.class,
                defaultForType = UUID.class
        )
})
public class Hearing implements JsonEntity {

    @Id
    @Column(name = "id", unique = true, nullable = false)
    private UUID id;

    @Column(name = "court_centre_id")
    private UUID courtCentreId;

    @Column(name = "court_room_id")
    private UUID courtRoomId;

    @Column(name = "is_vacated_trial")
    private Boolean isVacatedTrial;

    @Column(name = "unscheduled")
    private Boolean unscheduled;

    @Column(name = "type_id")
    private UUID typeId;

    @Column(name = "jurisdiction_type")
    private String jurisdictionType;

    @Column(name = "week_commencing_start_date")
    private LocalDate weekCommencingStartDate;

    @Column(name = "week_commencing_end_date")
    private LocalDate weekCommencingEndDate;

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY, mappedBy = "hearing", orphanRemoval = true)
    private Set<HearingDays> hearingDays = new HashSet<>();

    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY, mappedBy = "hearing", orphanRemoval = true)
    private Set<ListedCases> listedCases = new HashSet<>();

    @Column(name = "allocated")
    private Boolean allocated;

    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY, mappedBy = "hearing", orphanRemoval = true)
    private Set<CourtApplications> courtApplications = new HashSet<>();

    @Column(name = "type_of_list_id")
    private UUID typeOfListId;

    @Column(name = "properties", columnDefinition = "jsonb")
    @Type( type = "jsonb-node" )
    private JsonNode properties;

    @Column(updatable = false, insertable = false)
    private Long totalCount;

    @Column(name = "is_possible_disqualification")
    private Boolean isPossibleDisqualification;

    /** flattened hearing-days virtual col: number of hearing days in for the parent hearingId**/
    @Column(name = "hearing_day_count", updatable = false, insertable = false)
    private Long hearingDayCount;

    /** flattened hearing-days virtual col: position of the current hearing-day in the list of hearingDays belonging to this hearingId **/
    @Column(name = "hearing_day_position",updatable = false, insertable = false)
    private Long hearingDayPosition;

    /** flattened hearing virtual col: The date matching this current flat-hearing. **/
    @Column(name = "hearing_date",updatable = false, insertable = false)
    private LocalDate hearingDate;

    public Hearing() {
        // for JPA

    }

    public Hearing(final UUID id, final JsonNode properties) {
        this.id = id;
        this.properties = properties;

    }

    public Hearing(final UUID id,
                   final UUID courtCentreId,
                   final UUID courtRoomId,
                   final Boolean isVacatedTrial,
                   final Boolean unscheduled,
                   final UUID typeId,
                   final String jurisdictionType,
                   final LocalDate weekCommencingStartDate,
                   final LocalDate weekCommencingEndDate,
                   final LocalDate startDate,
                   final LocalDate endDate,
                   final Set<HearingDays> hearingDays,
                   final Set<ListedCases> listedCases,
                   final Boolean allocated,
                   final Set<CourtApplications> courtApplications,
                   final UUID typeOfListId,
                   final JsonNode properties,
                   final Boolean isPossibleDisqualification,
                   final Long hearingDayCount,
                   final Long hearingDayPosition,
                   final LocalDate hearingDate) {
        this.id = id;
        this.courtCentreId = courtCentreId;
        this.courtRoomId = courtRoomId;
        this.isVacatedTrial = isVacatedTrial;
        this.unscheduled = unscheduled;
        this.typeId = typeId;
        this.jurisdictionType = jurisdictionType;
        this.weekCommencingStartDate = weekCommencingStartDate;
        this.weekCommencingEndDate = weekCommencingEndDate;
        this.startDate = startDate;
        this.endDate = endDate;
        this.setHearingDays(hearingDays);
        this.setListedCases(listedCases);
        this.allocated = allocated;
        this.setCourtApplications(courtApplications);
        this.typeOfListId = typeOfListId;
        this.properties = properties;
        this.isPossibleDisqualification = isPossibleDisqualification;
        this.hearingDayCount = hearingDayCount;
        this.hearingDayPosition = hearingDayPosition;
        this.hearingDate = hearingDate;
    }

    public static HearingBuilder builder() {
        return new HearingBuilder();
    }

    public UUID getId() {
        return id;
    }

    public JsonNode getProperties() {
        return properties;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public void setProperties(JsonNode properties) {
        this.properties = properties;
    }

    public UUID getCourtCentreId() {
        return courtCentreId;
    }

    public void setCourtCentreId(final UUID courtCentreId) {
        this.courtCentreId = courtCentreId;
    }

    public UUID getCourtRoomId() {
        return courtRoomId;
    }

    public void setCourtRoomId(final UUID courtRoomId) {
        this.courtRoomId = courtRoomId;
    }

    public Boolean getIsVacatedTrial() {
        return isVacatedTrial;
    }

    public void setIsVacatedTrial(final Boolean vacatedTrial) {
        isVacatedTrial = vacatedTrial;
    }

    public Boolean getUnscheduled() {
        return unscheduled;
    }

    public void setUnscheduled(final Boolean unscheduled) {
        this.unscheduled = unscheduled;
    }

    public UUID getTypeId() {
        return typeId;
    }

    public void setTypeId(final UUID typeId) {
        this.typeId = typeId;
    }

    public String getJurisdictionType() {
        return jurisdictionType;
    }

    public void setJurisdictionType(final String jurisdictionType) {
        this.jurisdictionType = jurisdictionType;
    }

    public LocalDate getWeekCommencingStartDate() {
        return weekCommencingStartDate;
    }

    public void setWeekCommencingStartDate(final LocalDate weekCommencingStartDate) {
        this.weekCommencingStartDate = weekCommencingStartDate;
    }

    public LocalDate getWeekCommencingEndDate() {
        return weekCommencingEndDate;
    }

    public void setWeekCommencingEndDate(final LocalDate weekCommencingEndDate) {
        this.weekCommencingEndDate = weekCommencingEndDate;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public void setStartDate(final LocalDate startDate) {
        this.startDate = startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public void setEndDate(final LocalDate endDate) {
        this.endDate = endDate;
    }

    public Set<HearingDays> getHearingDays() {
        return hearingDays;
    }

    public Long getHearingDayCount() {
        return hearingDayCount;
    }

    public void setHearingDayCount(final Long hearingDayCount) {
        this.hearingDayCount = hearingDayCount;
    }

    public Long getHearingDayPosition() {
        return hearingDayPosition;
    }

    public void setHearingDayPosition(final Long hearingDayPosition) {
        this.hearingDayPosition = hearingDayPosition;
    }

    public LocalDate getHearingDate() {
        return hearingDate;
    }

    public void setHearingDate(final LocalDate hearingDate) {
        this.hearingDate = hearingDate;
    }

    public void setHearingDays(final Set<HearingDays> hearingDays) {
        if(isNull(hearingDays)){
            return;
        }

        this.hearingDays.clear();
        hearingDays.forEach(hearingDay -> {
            hearingDay.setHearing(this);
            this.hearingDays.add(hearingDay);
        });
    }

    public Set<ListedCases> getListedCases() {
        return listedCases;
    }

    public void setListedCases(final Set<ListedCases> listedCases) {
        if(isNull(listedCases)){
            return;
        }

        this.listedCases.clear();
        listedCases.forEach(listedCase -> {
            listedCase.setHearing(this);
            this.listedCases.add(listedCase);
        });
    }

    public Boolean getAllocated() {
        return allocated;
    }

    public void setAllocated(final Boolean allocated) {
        this.allocated = allocated;
    }

    public Set<CourtApplications> getCourtApplications() {
        return courtApplications;
    }


    public void setCourtApplications(final Set<CourtApplications> courtApplications) {
        if(isNull(courtApplications)){
            return;
        }

        this.courtApplications.clear();
        courtApplications.forEach(courtApplication -> {
            courtApplication.setHearing(this);
            this.courtApplications.add(courtApplication);
        });
    }

    public UUID getTypeOfListId() {
        return typeOfListId;
    }

    public void setTypeOfListId(final UUID typeOfListId) {
        this.typeOfListId = typeOfListId;
    }

    public Long getTotalCount() {
        return totalCount;
    }

    public void setTotalCount(final Long totalCount) {
        this.totalCount = totalCount;
    }

    public Boolean getPossibleDisqualification() {
        return isPossibleDisqualification;
    }

    public void setPossibleDisqualification(Boolean possibleDisqualification) {
        isPossibleDisqualification = possibleDisqualification;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        final Hearing hearing = (Hearing) o;

        return id.equals(hearing.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @SuppressWarnings({"PMD.BeanMembersShouldSerialize"})
    public static final class HearingBuilder {
        private UUID id;
        private UUID courtCentreId;
        private UUID courtRoomId;
        private Boolean isVacatedTrial;
        private Boolean unscheduled;
        private UUID typeId;
        private String jurisdictionType;
        private LocalDate weekCommencingStartDate;
        private LocalDate weekCommencingEndDate;
        private LocalDate startDate;
        private LocalDate endDate;
        private Set<HearingDays> hearingDays = new HashSet<>();
        private Set<ListedCases> listedCases = new HashSet<>();
        private Boolean allocated;
        private Set<CourtApplications> courtApplications = new HashSet<>();
        private UUID typeOfListId;
        private JsonNode properties;
        private Boolean isPossibleDisqualification;
        private Long hearingDayCount;
        private Long hearingDayPosition;
        private LocalDate hearingDate;

        private HearingBuilder() {
        }

        public static HearingBuilder aHearing() {
            return new HearingBuilder();
        }

        public HearingBuilder withId(UUID id) {
            this.id = id;
            return this;
        }

        public HearingBuilder withCourtCentreId(UUID courtCentreId) {
            this.courtCentreId = courtCentreId;
            return this;
        }

        public HearingBuilder withCourtRoomId(UUID courtRoomId) {
            this.courtRoomId = courtRoomId;
            return this;
        }

        public HearingBuilder withIsVacatedTrial(Boolean isVacatedTrial) {
            this.isVacatedTrial = isVacatedTrial;
            return this;
        }

        public HearingBuilder withUnscheduled(Boolean unscheduled) {
            this.unscheduled = unscheduled;
            return this;
        }

        public HearingBuilder withTypeId(UUID typeId) {
            this.typeId = typeId;
            return this;
        }

        public HearingBuilder withJurisdictionType(String jurisdictionType) {
            this.jurisdictionType = jurisdictionType;
            return this;
        }

        public HearingBuilder withWeekCommencingStartDate(LocalDate weekCommencingStartDate) {
            this.weekCommencingStartDate = weekCommencingStartDate;
            return this;
        }

        public HearingBuilder withWeekCommencingEndDate(LocalDate weekCommencingEndDate) {
            this.weekCommencingEndDate = weekCommencingEndDate;
            return this;
        }

        public HearingBuilder withStartDate(LocalDate startDate) {
            this.startDate = startDate;
            return this;
        }

        public HearingBuilder withEndDate(LocalDate endDate) {
            this.endDate = endDate;
            return this;
        }

        public HearingBuilder withHearingDays(Set<HearingDays> hearingDays) {
            if (!isEmpty(hearingDays)) {
                this.hearingDays = new HashSet<>(hearingDays);
            } else {
                this.hearingDays = new HashSet<>();
            }
            return this;
        }

        public HearingBuilder withListedCases(Set<ListedCases> listedCases) {
            if (!isEmpty(listedCases)) {
                this.listedCases = new HashSet<>(listedCases);
            } else {
                this.listedCases = new HashSet<>();
            }
            return this;
        }

        public HearingBuilder withAllocated(Boolean allocated) {
            this.allocated = allocated;
            return this;
        }

        public HearingBuilder withCourtApplications(Set<CourtApplications> courtApplications) {
            if (!isEmpty(courtApplications)) {
                this.courtApplications = new HashSet<>(courtApplications);
            } else {
                this.courtApplications = new HashSet<>();
            }
            return this;
        }

        public HearingBuilder withTypeOfListId(UUID typeOfListId) {
            this.typeOfListId = typeOfListId;
            return this;
        }

        public HearingBuilder withProperties(JsonNode properties) {
            this.properties = properties;
            return this;
        }

        public HearingBuilder withIsPossibleDisqualification(Boolean isPossibleDisqualification) {
            this.isPossibleDisqualification = isPossibleDisqualification;
            return this;
        }

        public HearingBuilder withHearingDayCount(Long hearingDayCount) {
            this.hearingDayCount = hearingDayCount;
            return this;
        }

        public HearingBuilder withHearingDayPosition(Long hearingDayPosition) {
            this.hearingDayPosition = hearingDayPosition;
            return this;
        }

        public HearingBuilder withHearingDate(LocalDate hearingDate) {
            this.hearingDate = hearingDate;
            return this;
        }

        public Hearing build() {
            return new Hearing(id, courtCentreId, courtRoomId, isVacatedTrial, unscheduled, typeId, jurisdictionType,
                    weekCommencingStartDate, weekCommencingEndDate, startDate, endDate, hearingDays, listedCases, allocated,
                    courtApplications, typeOfListId, properties, isPossibleDisqualification, hearingDayCount, hearingDayPosition, hearingDate);
        }
    }
}
