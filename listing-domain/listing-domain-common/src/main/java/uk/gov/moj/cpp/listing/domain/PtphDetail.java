package uk.gov.moj.cpp.listing.domain;

import java.io.Serializable;
import java.util.Objects;

public class PtphDetail implements Serializable {

    private static final long serialVersionUID = 1L;

    private final String tier;
    private final String listType;
    private final String keyReason;

    public PtphDetail(final String tier, final String listType, final String keyReason) {
        this.tier = tier;
        this.listType = listType;
        this.keyReason = keyReason;
    }

    public String getTier() {
        return tier;
    }

    public String getListType() {
        return listType;
    }

    public String getKeyReason() {
        return keyReason;
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        final PtphDetail that = (PtphDetail) obj;
        return Objects.equals(tier, that.tier)
                && Objects.equals(listType, that.listType)
                && Objects.equals(keyReason, that.keyReason);
    }

    @Override
    public int hashCode() {
        return Objects.hash(tier, listType, keyReason);
    }

    @Override
    public String toString() {
        return "PtphDetail{tier='" + tier + "', listType='" + listType + "', keyReason='" + keyReason + "'}";
    }
}
