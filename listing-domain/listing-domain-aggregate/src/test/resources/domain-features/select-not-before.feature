Feature: Select 'not before' start time for the hearing

  Scenario: An unallocated hearing has been listed and the start time has been assigned.
            Selecting 'not before' results in 'not before' being selected for hearing

    Given unallocated-hearing-listed,start-time-assigned-to-hearing
    When you selectNotBefore to a Hearing using a selected-true
    Then not-before-selected-for-hearing


  Scenario: An unallocated hearing has been listed, the start time has been assigned and 'not before' selected to true.
            Requesting 'not before' selected to be changed to false results in 'not before' being unselected for hearing

    Given unallocated-hearing-listed,start-time-assigned-to-hearing,not-before-selected-for-hearing
    When you selectNotBefore to a Hearing using a selected-false
    Then not-before-unselected-for-hearing


  Scenario: An unallocated hearing has been listed, the start time has been assigned and 'not before' selected.
            Requesting 'not before' selected to be changed to the same value does not result in any change

    Given unallocated-hearing-listed,start-time-assigned-to-hearing,not-before-selected-for-hearing
    When you selectNotBefore to a Hearing using a selected-true
    Then no events occured


  Scenario: An unallocated hearing has been listed and the start time has not been assigned.
            Selecting 'not before' does not result in any change

    Given unallocated-hearing-listed
    When you selectNotBefore to a Hearing using a selected-true
    Then no events occured