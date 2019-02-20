Feature: Change defendant offences

  Scenario: Defendant offences are updated following a public event from the Progression context

    Given hearing added to case
    When you updateDefendantOffences to a Case with a updated defendant offences
    Then defendant offences to be updated for hearing

  Scenario: Defendant offences are updated for Hearing

    Given hearing listed
      And end date changed for hearing way in the future
    When you updateOffences on a Hearing with a updated offences
    Then offences updated

  Scenario: Defendant offences for Hearing are not updated due to the hearing end date being in the past

    Given hearing listed in the past
    When you updateOffences on a Hearing with a updated offences
    Then no events occured

  Scenario: Defendant offences are added for Hearing

    Given hearing listed
      And end date changed for hearing way in the future
    When you addOffences to a Hearing with a add offences
    Then offences added

  Scenario: Defendant offences for Hearing are not added due to the hearing end date being in the past

    Given hearing listed in the past
    When you addOffences to a Hearing with a add offences
    Then no events occured

  Scenario: Defendant offences are deleted for Hearing

    Given hearing listed
      And end date changed for hearing way in the future
    When you deleteOffences to a Hearing with a delete offences
    Then offences deleted

  Scenario: Defendant offences for Hearing are not deleted due to the hearing end date being in the past

    Given hearing listed in the past
    When you deleteOffences to a Hearing with a delete offences
    Then no events occured
