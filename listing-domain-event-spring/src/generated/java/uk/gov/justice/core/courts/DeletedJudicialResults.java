package uk.gov.justice.core.courts;

import com.fasterxml.jackson.annotation.JsonCreator;
import java.io.Serializable;
import java.lang.Object;
import java.lang.Override;
import java.util.List;
import uk.gov.justice.listing.events.ApplicationCaseResults;
import uk.gov.justice.listing.events.ApplicationCourtOrderResults;
import uk.gov.justice.listing.events.ApplicationResults;
import uk.gov.justice.listing.events.ProsecutionCaseResults;

public class DeletedJudicialResults implements Serializable {
  private static final long serialVersionUID = 5569119786335285770L;

  private final List<ApplicationCaseResults> applicationCaseResults;

  private final List<ApplicationCourtOrderResults> applicationCourtOrderResults;

  private final List<ApplicationResults> applicationResults;

  private final List<ProsecutionCaseResults> prosecutionCaseResults;

  @JsonCreator
  public DeletedJudicialResults(final List<ApplicationCaseResults> applicationCaseResults, final List<ApplicationCourtOrderResults> applicationCourtOrderResults, final List<ApplicationResults> applicationResults, final List<ProsecutionCaseResults> prosecutionCaseResults) {
    this.applicationCaseResults = applicationCaseResults;
    this.applicationCourtOrderResults = applicationCourtOrderResults;
    this.applicationResults = applicationResults;
    this.prosecutionCaseResults = prosecutionCaseResults;
  }

  public List<ApplicationCaseResults> getApplicationCaseResults() {
    return applicationCaseResults;
  }

  public List<ApplicationCourtOrderResults> getApplicationCourtOrderResults() {
    return applicationCourtOrderResults;
  }

  public List<ApplicationResults> getApplicationResults() {
    return applicationResults;
  }

  public List<ProsecutionCaseResults> getProsecutionCaseResults() {
    return prosecutionCaseResults;
  }

  public static Builder deletedJudicialResults() {
    return new uk.gov.justice.core.courts.DeletedJudicialResults.Builder();
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) return true;
    if (obj == null || getClass() != obj.getClass()) return false;
    final uk.gov.justice.core.courts.DeletedJudicialResults that = (uk.gov.justice.core.courts.DeletedJudicialResults) obj;

    return java.util.Objects.equals(this.applicationCaseResults, that.applicationCaseResults) &&
    java.util.Objects.equals(this.applicationCourtOrderResults, that.applicationCourtOrderResults) &&
    java.util.Objects.equals(this.applicationResults, that.applicationResults) &&
    java.util.Objects.equals(this.prosecutionCaseResults, that.prosecutionCaseResults);
  }

  @Override
  public int hashCode() {
    return java.util.Objects.hash(applicationCaseResults, applicationCourtOrderResults, applicationResults, prosecutionCaseResults);}

  public static class Builder {
    private List<ApplicationCaseResults> applicationCaseResults;

    private List<ApplicationCourtOrderResults> applicationCourtOrderResults;

    private List<ApplicationResults> applicationResults;

    private List<ProsecutionCaseResults> prosecutionCaseResults;

    public Builder withApplicationCaseResults(final List<ApplicationCaseResults> applicationCaseResults) {
      this.applicationCaseResults = applicationCaseResults;
      return this;
    }

    public Builder withApplicationCourtOrderResults(final List<ApplicationCourtOrderResults> applicationCourtOrderResults) {
      this.applicationCourtOrderResults = applicationCourtOrderResults;
      return this;
    }

    public Builder withApplicationResults(final List<ApplicationResults> applicationResults) {
      this.applicationResults = applicationResults;
      return this;
    }

    public Builder withProsecutionCaseResults(final List<ProsecutionCaseResults> prosecutionCaseResults) {
      this.prosecutionCaseResults = prosecutionCaseResults;
      return this;
    }

    public Builder withValuesFrom(final DeletedJudicialResults deletedJudicialResults) {
      this.applicationCaseResults = deletedJudicialResults.getApplicationCaseResults();
      this.applicationCourtOrderResults = deletedJudicialResults.getApplicationCourtOrderResults();
      this.applicationResults = deletedJudicialResults.getApplicationResults();
      this.prosecutionCaseResults = deletedJudicialResults.getProsecutionCaseResults();
      return this;
    }

    public DeletedJudicialResults build() {
      return new uk.gov.justice.core.courts.DeletedJudicialResults(applicationCaseResults, applicationCourtOrderResults, applicationResults, prosecutionCaseResults);
    }
  }
}
