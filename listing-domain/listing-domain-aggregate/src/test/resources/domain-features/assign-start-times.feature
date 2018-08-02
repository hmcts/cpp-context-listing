Feature: Assign start times to a hearing or change the start times

  Scenario: An unallocated hearing has been listed and assigning a start times results in the
            start times being assigned to the hearing

    Given hearing listed
    When you assignStartTimes to a Hearing using a start times
    Then start times assigned to hearing


  Scenario: An unallocated hearing has been listed and a start times assigned. Changing the start time
            to a different start times results in the start times being changed for hearing

    Given hearing listed
      And start times assigned to hearing
    When you assignStartTimes to a Hearing using a different start times
    Then start times changed for hearing


  Scenario: An unallocated hearing has been listed and start times assigned. Requesting the original
            start times to be changed to the same start times does not result in any change

    Given hearing listed
      And start times assigned to hearing
    When you assignStartTimes to a Hearing using a the same start times
    Then no events occured