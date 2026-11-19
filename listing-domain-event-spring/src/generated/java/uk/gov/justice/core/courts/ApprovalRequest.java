package uk.gov.justice.core.courts;

import com.fasterxml.jackson.annotation.JsonCreator;
import java.io.Serializable;
import java.lang.Object;
import java.lang.Override;
import java.time.ZonedDateTime;
import java.util.UUID;
import uk.gov.justice.listing.events.ApprovalType;

public class ApprovalRequest implements Serializable {
  private static final long serialVersionUID = 5569119786335285770L;

  private final ApprovalType approvalType;

  private final UUID hearingId;

  private final ZonedDateTime requestApprovalTime;

  private final UUID userId;

  @JsonCreator
  public ApprovalRequest(final ApprovalType approvalType, final UUID hearingId, final ZonedDateTime requestApprovalTime, final UUID userId) {
    this.approvalType = approvalType;
    this.hearingId = hearingId;
    this.requestApprovalTime = requestApprovalTime;
    this.userId = userId;
  }

  public ApprovalType getApprovalType() {
    return approvalType;
  }

  public UUID getHearingId() {
    return hearingId;
  }

  public ZonedDateTime getRequestApprovalTime() {
    return requestApprovalTime;
  }

  public UUID getUserId() {
    return userId;
  }

  public static Builder approvalRequest() {
    return new uk.gov.justice.core.courts.ApprovalRequest.Builder();
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) return true;
    if (obj == null || getClass() != obj.getClass()) return false;
    final uk.gov.justice.core.courts.ApprovalRequest that = (uk.gov.justice.core.courts.ApprovalRequest) obj;

    return java.util.Objects.equals(this.approvalType, that.approvalType) &&
    java.util.Objects.equals(this.hearingId, that.hearingId) &&
    java.util.Objects.equals(this.requestApprovalTime, that.requestApprovalTime) &&
    java.util.Objects.equals(this.userId, that.userId);
  }

  @Override
  public int hashCode() {
    return java.util.Objects.hash(approvalType, hearingId, requestApprovalTime, userId);}

  public static class Builder {
    private ApprovalType approvalType;

    private UUID hearingId;

    private ZonedDateTime requestApprovalTime;

    private UUID userId;

    public Builder withApprovalType(final ApprovalType approvalType) {
      this.approvalType = approvalType;
      return this;
    }

    public Builder withHearingId(final UUID hearingId) {
      this.hearingId = hearingId;
      return this;
    }

    public Builder withRequestApprovalTime(final ZonedDateTime requestApprovalTime) {
      this.requestApprovalTime = requestApprovalTime;
      return this;
    }

    public Builder withUserId(final UUID userId) {
      this.userId = userId;
      return this;
    }

    public Builder withValuesFrom(final ApprovalRequest approvalRequest) {
      this.approvalType = approvalRequest.getApprovalType();
      this.hearingId = approvalRequest.getHearingId();
      this.requestApprovalTime = approvalRequest.getRequestApprovalTime();
      this.userId = approvalRequest.getUserId();
      return this;
    }

    public ApprovalRequest build() {
      return new uk.gov.justice.core.courts.ApprovalRequest(approvalType, hearingId, requestApprovalTime, userId);
    }
  }
}
