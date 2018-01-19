Feature: Assign a start time to a hearing or change the start time

  Scenario: An unallocated hearing has been listed and assigning a start time results in the
            start time being assigned to the hearing

    Given unallocated hearing listed
    When you assignStartTime to a Hearing using a start time
    Then start time assigned to hearing


  Scenario: An unallocated hearing has been listed and a start time assigned. Changing the start time
            to a different start time results in the start time being changed for hearing

    Given unallocated hearing listed
      And start time assigned to hearing
    When you assignStartTime to a Hearing using a different start time
    Then start time changed for hearing


  Scenario: An unallocated hearing has been listed and a start time assigned. Requesting the original
            start time to be changed to the same start time does not result in any change

    Given unallocated hearing listed
      And start time assigned to hearing
    When you assignStartTime to a Hearing using a the same start time
    Then no events occured