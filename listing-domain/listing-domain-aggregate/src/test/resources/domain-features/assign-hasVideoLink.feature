Feature: Assign a hasVideoLink to a hearing or change the hasVideoLink

  Scenario: An unallocated hearing has been listed and assigning the hasVideoLink being assigned to the hearing

    Given hearing listed
    When you assignVideoLink to a Hearing using a new hasVideoLink
    Then hasVideoLink assigned to hearing

  Scenario: An unallocated hearing has been listed and the hasVideoLink being assigned and then the hasVideoLink changed

    Given hearing listed
    And hasVideoLink assigned to hearing
    When you assignVideoLink to a Hearing using a remove hasVideoLink
    Then hasVideoLink changed to hearing

  Scenario: An unallocated hearing has been listed and the hasVideoLink being assigned and then same hasVideoLink received

    Given hearing listed
    And hasVideoLink assigned to hearing
    When you assignVideoLink to a Hearing using a new hasVideoLink
    Then no events occurred