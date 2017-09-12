package uk.gov.moj.cpp.listing.steps.data;


public class JudgeData {

    private final String id;
    private final String title;
    private final String firstName;
    private final String lastName;

    public JudgeData(final String id,
                 final String title,
                 final String firstName,
                 final String lastName) {
        this.id = id;
        this.title = title;
        this.firstName = firstName;
        this.lastName = lastName;
    }

    public String getId() { return id; }

    public String getTitle() { return title; }

    public String getFirstName() { return firstName; }

    public String getLastName() { return lastName; }
}
