Feature: Remove a hasVideoLink from a hearing

  Scenario: An unallocated hearing has been listed and the videoLink details assigned. A request to remove
            the videoLink details from the hearing

    Given hearing listed
    And hasVideoLink assigned to hearing
    When you removeVideoLinkDetails from a Hearing using a hearing-id
    Then hasVideoLink removed from hearing


  Scenario: An unallocated hearing has been listed and no videoLink details has been assigned. A request to remove
            the videoLink details from the hearing does not result in any change

    Given hearing listed
    When you removeVideoLinkDetails from a Hearing using a hearing id
    Then no events occurred