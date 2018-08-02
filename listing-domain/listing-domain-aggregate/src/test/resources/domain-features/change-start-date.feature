Feature: Change the start date of the hearing

  Scenario: An unallocated hearing has been listed and changing the original start date to
            a different start date results in the hearing start date being changed

    Given hearing listed
    When you changeStartDate to a Hearing using a different start date
    Then start date changed for hearing


  Scenario: An unallocated hearing has been listed and requesting the original start date to
            be changed to the same start date does not result in any change

    Given hearing listed
    When you changeStartDate to a Hearing using a the same start date
    Then no events occured


  Scenario: A request to change the start date for a hearing that has not been listed
            does not result in any change

    Given no previous events
    When you changeStartDate to a Hearing using a start date
    Then no events occured
