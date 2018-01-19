Feature: Remove a start time from a hearing

  Scenario: An unallocated hearing has been listed and a start time assigned. A request to remove the
            start time results in the start time being removed from the hearing

    Given unallocated hearing listed
      And start time assigned to hearing
    When you removeStartTime from a Hearing using a hearing id
    Then start time removed from hearing


  Scenario: An unallocated hearing has been listed and no start time has been assigned. A request to remove
            a start time from the hearing does not result in any change

    Given unallocated hearing listed
    When you removeStartTime from a Hearing using a hearing id
    Then no events occured