Feature: Change the end date of the hearing

  Scenario: An unallocated hearing has been listed and changing the original end date to
            a different end date results in the hearing end date being changed

    Given hearing listed
    When you changeEndDate to a Hearing using a different end date
    Then end date changed for hearing

  Scenario: An unallocated hearing has been listed and requesting the original end date to
            be changed to the same end date does not result in any change

    Given hearing listed
    When you changeEndDate to a Hearing using a the same end date
    Then no events occured


  Scenario: A request to change the end date for a hearing that has not been listed
            does not result in any change

    Given no previous events
    When you changeEndDate to a Hearing using an end date
    Then no events occured

