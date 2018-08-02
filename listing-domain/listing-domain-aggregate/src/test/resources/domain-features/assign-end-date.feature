Feature: Change the end date of the hearing

  Scenario: An unallocated hearing has been listed and assigning the end date results in the
  end date being assigned to the hearing

    Given hearing listed
    When you assignEndDate to a Hearing using a end date
    Then end date assigned to hearing


  Scenario: An unallocated hearing has been listed and end date is assigned, changing the
  end date results in the end date being changed for hearing

    Given hearing listed
    And end date assigned to hearing
    When you assignEndDate to a Hearing using a different end date
    Then end date changed for hearing

  Scenario: An unallocated hearing has been listed and end date is assigned, changing the
  end date to the same end date, does not result in any change

    Given hearing listed
    And end date assigned to hearing
    When you assignEndDate to a Hearing using a same end date
    Then no events occurred
