Feature: List a hearing

  Scenario: A request to list a hearing results in an unallocated hearing being listed

    Given no previous events
    When you list to a Hearing using a new hearing details
    Then hearing listed


  Scenario: A request to list a hearing that has already been listed does not result in a new listing

    Given hearing listed
    When you list to a Hearing using a the same hearing details
    Then no events occured