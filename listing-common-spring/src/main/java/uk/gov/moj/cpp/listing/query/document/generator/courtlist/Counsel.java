package uk.gov.moj.cpp.listing.query.document.generator.courtlist;

public class Counsel {

    private  String title;
    private  String firstName;
    private  String middleName;
    private  String lastName;

    public static Counsel.Builder counsel() {
        return new Counsel.Builder();
    }

    public static final class Builder {

        private  String title;
        private  String firstName;
        private  String middleName;
        private  String lastName;


        private Builder() {
        }

        public Counsel.Builder withTitle(String title) {
            this.title = title;
            return this;
        }


        public Counsel.Builder withFirstName(String firstName) {
            this.firstName = firstName;
            return this;
        }

        public Counsel.Builder withMiddleName(String middleName) {
            this.middleName = middleName;
            return this;
        }

        public Counsel.Builder withLastName(String lastName) {
            this.lastName = lastName;
            return this;
        }
        public Counsel build() {
            final Counsel counsel = new Counsel();
            counsel.title = title;
            counsel.firstName = firstName;
            counsel.lastName = lastName;
            counsel.middleName = middleName;
            return counsel;
        }

    }

    public String getTitle() {
        return title;
    }

    public String getFirstName() {
        return firstName;
    }

    public String getMiddleName() {
        return middleName;
    }

    public String getLastName() {
        return lastName;
    }
}
