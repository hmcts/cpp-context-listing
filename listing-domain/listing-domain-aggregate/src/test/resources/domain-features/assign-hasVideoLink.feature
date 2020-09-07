Feature: Assign a hasVideoLink to a hearing or change the hasVideoLink

  Scenario: An unallocated hearing has been listed and assigning the videoLink details results in the
            the videoLink details being assigned to the hearing

    Given hearing listed
    When you assignOrUpdateVideoLinkDetails to a Hearing using a new hasVideoLink
    Then hasVideoLink assigned to hearing


  Scenario: An unallocated hearing has been listed and a hasVideoLink and videoLinkDetails assigned. Changing the videoLink details
            to a different videoLink details results in the videoLink details being changed for hearing

    Given hearing listed
    And hasVideoLink assigned to hearing
    When you assignOrUpdateVideoLinkDetails to a Hearing using a different hasVideoLink
    Then hasVideoLink changed for hearing