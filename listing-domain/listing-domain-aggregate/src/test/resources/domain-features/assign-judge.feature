Feature: Assign a judge to a hearing or change the judge

  Scenario: An unallocated hearing has been listed and assigning a judge results in the
            judge being assigned to the hearing

    Given unallocated hearing listed
    When you assignJudge to a Hearing using a judge
    Then judge assigned to hearing


  Scenario: An unallocated hearing has been listed and a judge assigned. Changing the judge
            to a different judge results in the judge being changed for hearing

    Given unallocated hearing listed
      And judge assigned to hearing
    When you assignJudge to a Hearing using a different judge
    Then judge changed for hearing


  Scenario: An unallocated hearing has been listed and a judge assigned. Requesting the original
            judge to be changed to the same judge does not result in any change

    Given unallocated hearing listed
      And judge assigned to hearing
    When you assignJudge to a Hearing using a the same judge
    Then no events occured