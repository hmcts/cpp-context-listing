package uk.gov.moj.cpp.listing.domain.aggregate;


import java.io.Serializable;
import java.util.UUID;

@SuppressWarnings({"squid:S00107", "squid:S1067", "PMD.BeanMembersShouldSerialize"})
public class BailStatus implements Serializable {
    private final String code;

    private final CustodyTimeLimit custodyTimeLimit;

    private final String description;

    private final UUID id;

    public BailStatus(final String code, final CustodyTimeLimit custodyTimeLimit, final String description, final UUID id) {
        this.code = code;
        this.custodyTimeLimit = custodyTimeLimit;
        this.description = description;
        this.id = id;
    }

    public String getCode() {
        return code;
    }

    public CustodyTimeLimit getCustodyTimeLimit() {
        return custodyTimeLimit;
    }

    public String getDescription() {
        return description;
    }

    public UUID getId() {
        return id;
    }

    public static Builder bailStatus() {
        return new Builder();
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }

        final BailStatus that = (BailStatus) obj;

        return java.util.Objects.equals(this.code, that.code) &&
                java.util.Objects.equals(this.custodyTimeLimit, that.custodyTimeLimit) &&
                java.util.Objects.equals(this.description, that.description) &&
                java.util.Objects.equals(this.id, that.id);
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(code, custodyTimeLimit, description, id);
    }

    @Override
    public String toString() {
        return "BailStatus{" +
                "code='" + code + "'," +
                "custodyTimeLimit='" + custodyTimeLimit + "'," +
                "description='" + description + "'," +
                "id='" + id + "'" +
                "}";
    }

    @SuppressWarnings({"PMD.BeanMembersShouldSerialize"})
    public static final class Builder {
        private String code;

        private CustodyTimeLimit custodyTimeLimit;

        private String description;

        private UUID id;

        public Builder withCode(final String code) {
            this.code = code;
            return this;
        }

        public Builder withCustodyTimeLimit(final CustodyTimeLimit custodyTimeLimit) {
            this.custodyTimeLimit = custodyTimeLimit;
            return this;
        }

        public Builder withDescription(final String description) {
            this.description = description;
            return this;
        }

        public Builder withId(final UUID id) {
            this.id = id;
            return this;
        }

        public BailStatus build() {
            return new BailStatus(code, custodyTimeLimit, description, id);
        }
    }
}
