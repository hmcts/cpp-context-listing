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
    Then no events occurred

  Scenario: An unallocated hearing has been listed and removing the start date
            results in the hearing start date being removed

    Given hearing listed
    When you removeStartDate to a Hearing using a hearing id
    Then start date removed for hearing
