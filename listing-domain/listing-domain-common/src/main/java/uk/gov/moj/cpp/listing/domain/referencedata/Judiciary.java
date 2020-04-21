package uk.gov.moj.cpp.listing.domain.referencedata;

import java.io.Serializable;
import java.util.UUID;

@SuppressWarnings("pmd:BeanMembersShouldSerialize")
public class Judiciary implements Serializable {

    private UUID id;

    private String titleSuffix;

    private String titlePrefix;

    private String titleJudicialPrefix;

    private String surname;

    private String forenames;

    public Judiciary(final UUID id, final String titleSuffix, final String titlePrefix, final String titleJudicialPrefix, final String surname, final String forenames) {
        this.id = id;
        this.titleSuffix = titleSuffix;
        this.titlePrefix = titlePrefix;
        this.titleJudicialPrefix = titleJudicialPrefix;
        this.surname = surname;
        this.forenames = forenames;
    }

    public Judiciary() {}

    public UUID getId() {
        return id;
    }

    public String getTitlePrefix() {
        return titlePrefix;
    }

    public String getTitleJudicialPrefix() {
        return titleJudicialPrefix;
    }

    public String getTitleSuffix() {
        return titleSuffix;
    }

    public String getSurname() {
        return surname;
    }

    public String getForenames() {
        return forenames;
    }

    public static class Builder {
        private UUID id;
        private String titleSuffix;
        private String titlePrefix;
        private String titleJudicialPrefix;
        private String surname;
        private String forenames;

        public Judiciary.Builder withId(final UUID id) {
            this.id = id;
            return this;
        }

        public Judiciary.Builder withTitlePrefix(final String titlePrefix) {
            this.titlePrefix = titlePrefix;
            return this;
        }

        public Judiciary.Builder withTitleJudicialPrefix(final String titleJudicialPrefix) {
            this.titleJudicialPrefix = titleJudicialPrefix;
            return this;
        }

        public Judiciary.Builder withTitleSuffix(final String titleSuffix) {
            this.titleSuffix = titleSuffix;
            return this;
        }

        public Judiciary.Builder withSurname(final String surname) {
            this.surname = surname;
            return this;
        }

        public Judiciary.Builder withForenames(final String forenames) {
            this.forenames = forenames;
            return this;
        }

        public Judiciary build() {
            return new Judiciary(id, titleSuffix, titlePrefix, titleJudicialPrefix, surname, forenames);
        }
    }
}