Feature: Remove a court-room from a hearing

  Scenario: An unallocated hearing has been listed and a court-room assigned. A request to remove the
            court-room results in the court-room being removed from the hearing

    Given hearing listed
      And court room assigned to hearing
    When you removeCourtRoom from a Hearing using a hearing id
    Then court room removed from hearing


  Scenario: An unallocated hearing has been listed and no court-room has been assigned. A request to remove
            a court-room from the hearing does not result in any change

    Given hearing listed
    When you removeCourtRoom from a Hearing using a hearing id
    Then no events occurred