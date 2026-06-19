Feature: Mark as duplicate

  Scenario: An hearing has been listed. When hearing marked as duplicated then event raised is raised
    Given hearing listed
    When you markHearingAsDuplicate from a Hearing using a hearingId and caseIds
    Then hearing marked as duplicate

  Scenario: An hearing has been listed. When hearing marked as duplicated then event raised is raised

    Given hearing added to case
    When you markHearingAsDuplicate on a Case using a hearingId and caseId
    Then hearing marked as duplicate for case

  Scenario: Given an unallocated hearing, when marked as duplicated then event raised is raised
    Given hearing listed
    When you markUnallocatedHearingAsDuplicate from a Hearing using a hearing id
    Then unallocated hearing marked as duplicate

  Scenario: An hearing has been listed. When hearing marked as duplicated then no event raised is raised
    Given hearing allocated for listing
    When you markUnallocatedHearingAsDuplicate from a Hearing using a hearing id
    Then no events occurred