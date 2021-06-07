package uk.gov.moj.cpp.listing.persistence.entity;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

import javax.persistence.Column;
import javax.persistence.Embeddable;

@SuppressWarnings("squid:S1067")
@Embeddable
public class Prosecutor implements Serializable {

    private static final long serialVersionUID = 1L;

    @Column(name = "prosecutor_id")
    private UUID prosecutorId;

    @Column(name = "prosecutor_code")
    private String prosecutorCode;

    public UUID getProsecutorId() {
        return prosecutorId;
    }

    public void setProsecutorId(final UUID prosecutorId) {
        this.prosecutorId = prosecutorId;
    }

    public String getProsecutorCode() {
        return prosecutorCode;
    }

    public void setProsecutorCode(final String prosecutorCode) {
        this.prosecutorCode = prosecutorCode;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) { return true; }
        if (o == null || getClass() != o.getClass()) { return false; }
        final Prosecutor that = (Prosecutor) o;
        return Objects.equals(prosecutorId, that.prosecutorId) &&
                Objects.equals(prosecutorCode, that.prosecutorCode);
    }

    @Override
    public int hashCode() {
        return Objects.hash(prosecutorId, prosecutorCode);
    }
}