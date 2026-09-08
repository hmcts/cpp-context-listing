package uk.gov.moj.cpp.listing.persistence.entity;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

@SuppressWarnings({"squid:S1948", "pmd:BeanMembersShouldSerialize", "squid:S1067"})
@Entity
@Table(name = "cache_refdata_courtroom")
public class CacheRefDataCourtroom implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @Column(name = "id", unique = true, nullable = false)
    private UUID id;

    @Column(name = "courtroom_name")
    private String courtroomName;

    public CacheRefDataCourtroom() {
        // for JPA
    }

    public CacheRefDataCourtroom(final UUID id, final String courtroomName) {
        this.id = id;
        this.courtroomName = courtroomName;
    }

    public UUID getId() {
        return id;
    }

    public void setId(final UUID id) {
        this.id = id;
    }

    public String getCourtroomName() {
        return courtroomName;
    }

    public void setCourtroomName(final String courtroomName) {
        this.courtroomName = courtroomName;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof CacheRefDataCourtroom)) {
            return false;
        }
        final CacheRefDataCourtroom that = (CacheRefDataCourtroom) o;
        return Objects.equals(id, that.id) &&
                Objects.equals(courtroomName, that.courtroomName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, courtroomName);
    }

    public static Builder builder() {
        return new Builder();
    }

    @SuppressWarnings({"pmd:BeanMembersShouldSerialize"})
    public static class Builder {
        private UUID id;
        private String courtroomName;


        public Builder withId(UUID id) {
            this.id = id;
            return this;
        }

        public Builder withSequence(String courtroomName) {
            this.courtroomName = courtroomName;
            return this;
        }


        public CacheRefDataCourtroom build() {
            return new CacheRefDataCourtroom(id, courtroomName);
        }
    }
}
