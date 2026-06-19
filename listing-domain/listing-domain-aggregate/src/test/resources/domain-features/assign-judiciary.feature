Feature: Assign a judiciary to a hearing or change the judiciary

  Scenario: An unallocated hearing has been listed and assigning a judiciary results in the
            judiciary being assigned to the hearing

    Given hearing listed
    When you assignJudiciary to a Hearing using a judiciary
    Then judiciary assigned to hearing


  Scenario: An unallocated hearing has been listed and a judge assigned. Changing the judge
            to a different judge results in the judge being changed for hearing

    Given hearing listed
      And judiciary assigned to hearing
    When you assignJudiciary to a Hearing using a different judiciary
    Then judiciary changed for hearing


  Scenario: An unallocated hearing has been listed and a judiciary assigned. Requesting the original
            judiciary to be changed to the same judiciary does not result in any change

    Given hearing listed
      And judiciary assigned to hearing
    When you assignJudiciary to a Hearing using a the same judiciary
    Then no events occurred