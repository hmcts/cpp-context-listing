Feature: Change the non sitting days of the hearing

  Scenario: An unallocated hearing has been listed and assigning the non sitting days results in the
  non sitting days being assigned to the hearing

    Given hearing listed
    When you assignNonSittingDays to a Hearing using a non sitting days
    Then non sitting days assigned to hearing


  Scenario: An unallocated hearing has been listed and non sitting days is assigned, changing the
  non sitting days results in the non sitting days being changed for hearing

    Given hearing listed
    And non sitting days assigned to hearing
    When you assignNonSittingDays to a Hearing using a different non sitting days
    Then non sitting days changed for hearing

  Scenario: An unallocated hearing has been listed and non sitting days is assigned, changing the
  non sitting days to the same non sitting days, does not result in any change

    Given hearing listed
    And non sitting days assigned to hearing
    When you assignNonSittingDays to a Hearing using a same non sitting days
    Then no events occurred
