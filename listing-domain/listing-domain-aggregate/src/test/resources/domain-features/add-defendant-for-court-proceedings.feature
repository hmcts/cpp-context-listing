Feature: Add defendant details following public event from Progression

  Scenario: Defendants are added following a public event from the Progression context

    Given hearing added to case
    When you addedDefendantForCourtProceedings to a Case with a new defendant
    Then defendants to be added for court proceedings

  Scenario: Defendant is added to court proceedings for hearing

    Given hearing listed
      And end date changed for hearing way in the future
    When you addDefendantsForCourtProceedings to a Hearing with a new defendants
    Then defendants are added

  Scenario: Defendant details for a hearing are not updated due to the hearing end date being in the past

      Given hearing listed in the past
      When you addDefendantsForCourtProceedings to a Hearing with a new defendants
      Then no events occured