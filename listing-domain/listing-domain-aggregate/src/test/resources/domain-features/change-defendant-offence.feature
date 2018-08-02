Feature: Change defendant offences

  Scenario: Defendant offences are updated following a public event from the Progression context

    Given case sent for ptp listing
    When you updateDefendantOffences to a Case with a updated defendant offences
    Then defendant offences to be updated for hearing

  Scenario: Defendant offences are updated

    Given hearing listed
      And end date assigned to hearing way in the future
    When you updateOffences on a Hearing with a updated offences
    Then offences updated

  Scenario: Defendant offences are not updated due to the hearing end date being in the past

    Given hearing listed in the past
    When you updateOffences on a Hearing with a updated offences
    Then no events occured

  Scenario: Defendant offences are added

    Given hearing listed
      And end date assigned to hearing way in the future
    When you addOffences to a Hearing with a add offences
    Then offences added

  Scenario: Defendant offences are not added due to the hearing end date being in the past

    Given hearing listed in the past
    When you addOffences to a Hearing with a add offences
    Then no events occured

  Scenario: Defendant offences are deleted

    Given hearing listed
      And end date assigned to hearing way in the future
    When you deleteOffences on a Hearing with a delete offences
    Then offences deleted

  Scenario: Defendant offences are not deleted due to the hearing end date being in the past

    Given hearing listed in the past
    When you deleteOffences on a Hearing with a delete offences
    Then no events occured
