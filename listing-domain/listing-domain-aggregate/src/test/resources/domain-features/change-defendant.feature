Feature: Change defendant details following public event from Progression

  Scenario: Defendant details are changed following a public event from the Progression context

    Given hearing added to case
    When you updateDefendant to a Case with a changed defendant
    Then defendants to be updated for hearing

  Scenario: Defendant details for a hearing are updated

    Given hearing listed
      And end date changed for hearing way in the future
    When you updateDefendants to a Hearing with a changed defendants
    Then defendant details updated

  Scenario: Defendant details for a hearing are not updated due to the hearing end date being in the past

    Given hearing listed in the past
    When you updateDefendants to a Hearing with a changed defendants
    Then no events occurred

  Scenario: An hearing with multiple defendant listed. And only one of defendant detail is updated. When the case updated with the other defendant

    Given hearing listed with multiple defendant
    And defendant details updated
    When you updateDefendants to a Hearing with a not updated defendants
    Then no events occurred

  Scenario: An hearing with multiple defendant listed. And  allocation is partial. When not allocated case updated.

    Given hearing listed with multiple defendant
    And allocated hearing with partial
    When you updateDefendants to a Hearing with a not updated defendants
    Then no events occurred
