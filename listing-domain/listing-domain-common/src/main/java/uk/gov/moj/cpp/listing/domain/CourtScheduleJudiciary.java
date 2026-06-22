package uk.gov.moj.cpp.listing.domain;

import java.util.List;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
@SuppressWarnings("squid:S1067")
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CourtScheduleJudiciary {

    private String judiciaryId;

    private String rotaJudiciaryId;

    private String title;

    private String forenames;

    private String surname;

    private String emailAddress;

    private String courtScheduleId;

    private String courtListingProfileId;

    private String judiciaryType;

    private String position;

    private Boolean isBenchChairman;

    private Boolean isDeputy;

    private Integer seqId;
    private String titleJudicialPrefix;
    private String titleJudicialPrefixWelsh;
    private String personId;
    private List<String> specialisms;
    private String requestedName;

    @JsonProperty("id")
    public String getJudiciaryId() {
        return judiciaryId;
    }

    public void setJudiciaryId(final String judiciaryId) {
        this.judiciaryId = judiciaryId;
    }

    public String getRotaJudiciaryId() {
        return rotaJudiciaryId;
    }

    public void setRotaJudiciaryId(final String rotaJudiciaryId) {
        this.rotaJudiciaryId = rotaJudiciaryId;
    }

    @JsonProperty("titlePrefix")
    public String getTitle() {
        return title;
    }

    public void setTitle(final String title) {
        this.title = title;
    }

    public String getForenames() {
        return forenames;
    }

    public void setForenames(final String forenames) {
        this.forenames = forenames;
    }

    public String getSurname() {
        return surname;
    }

    public void setSurname(final String surname) {
        this.surname = surname;
    }

    public String getEmailAddress() {
        return emailAddress;
    }

    public void setEmailAddress(final String emailAddress) {
        this.emailAddress = emailAddress;
    }

    public String getCourtScheduleId() {
        return courtScheduleId;
    }

    public void setCourtScheduleId(final String courtScheduleId) {
        this.courtScheduleId = courtScheduleId;
    }

    public String getCourtListingProfileId() {
        return courtListingProfileId;
    }

    public void setCourtListingProfileId(final String courtListingProfileId) {
        this.courtListingProfileId = courtListingProfileId;
    }

    public String getJudiciaryType() {
        return judiciaryType;
    }

    public void setJudiciaryType(final String judiciaryType) {
        this.judiciaryType = judiciaryType;
    }

    public String getPosition() {
        return position;
    }

    public void setPosition(final String position) {
        this.position = position;
    }

    @JsonProperty("isBenchChairman")
    public Boolean getBenchChairman() {
        return isBenchChairman;
    }

    public void setBenchChairman(final Boolean benchChairman) {
        isBenchChairman = benchChairman;
    }

    @JsonProperty("isDeputy")
    public Boolean getDeputy() {
        return isDeputy;
    }

    public void setDeputy(final Boolean deputy) {
        isDeputy = deputy;
    }

    public Integer getSeqId() {
        return seqId;
    }

    public void setSeqId(final Integer seqId) {
        this.seqId = seqId;
    }

    public String getTitleJudicialPrefix() {
        return titleJudicialPrefix;
    }

    public void setTitleJudicialPrefix(final String titleJudicialPrefix) {
        this.titleJudicialPrefix = titleJudicialPrefix;
    }

    public String getTitleJudicialPrefixWelsh() {
        return titleJudicialPrefixWelsh;
    }

    public void setTitleJudicialPrefixWelsh(final String titleJudicialPrefixWelsh) {
        this.titleJudicialPrefixWelsh = titleJudicialPrefixWelsh;
    }

    public String getPersonId() {
        return personId;
    }

    public void setPersonId(final String personId) {
        this.personId = personId;
    }

    public List<String> getSpecialisms() {
        return specialisms;
    }

    public void setSpecialisms(final List<String> specialisms) {
        this.specialisms = specialisms;
    }

    public String getRequestedName() {
        return requestedName;
    }

    public void setRequestedName(final String requestedName) {
        this.requestedName = requestedName;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof CourtScheduleJudiciary)) {
            return false;
        }
        final CourtScheduleJudiciary courtScheduleJudiciary = (CourtScheduleJudiciary) o;
        return Objects.equals(judiciaryId, courtScheduleJudiciary.judiciaryId) &&
                Objects.equals(rotaJudiciaryId, courtScheduleJudiciary.rotaJudiciaryId) &&
                Objects.equals(title, courtScheduleJudiciary.title) &&
                Objects.equals(forenames, courtScheduleJudiciary.forenames) &&
                Objects.equals(surname, courtScheduleJudiciary.surname) &&
                Objects.equals(emailAddress, courtScheduleJudiciary.emailAddress) &&
                Objects.equals(courtListingProfileId, courtScheduleJudiciary.courtListingProfileId) &&
                Objects.equals(judiciaryType, courtScheduleJudiciary.judiciaryType) &&
                Objects.equals(position, courtScheduleJudiciary.position) &&
                Objects.equals(isBenchChairman, courtScheduleJudiciary.isBenchChairman) &&
                Objects.equals(isDeputy, courtScheduleJudiciary.isDeputy);
    }

    @Override
    public int hashCode() {
        return Objects.hash(judiciaryId, rotaJudiciaryId, title, forenames, surname, emailAddress, courtListingProfileId, judiciaryType, position, isBenchChairman, isDeputy);
    }

    @Override
    public String toString() {
        return "CourtScheduleJudiciary{" +
                "judiciaryId=" + judiciaryId +
                ", rotaJudiciaryId='" + rotaJudiciaryId + '\'' +
                ", title='" + title + '\'' +
                ", forenames='" + forenames + '\'' +
                ", surname='" + surname + '\'' +
                ", emailAddress='" + emailAddress + '\'' +
                ", courtListingProfileId='" + courtListingProfileId + '\'' +
                ", judiciaryType='" + judiciaryType + '\'' +
                ", position='" + position + '\'' +
                ", isBenchChairman=" + isBenchChairman +
                ", isDeputy=" + isDeputy +
                '}';
    }

}
