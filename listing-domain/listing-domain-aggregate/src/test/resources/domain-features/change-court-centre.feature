Feature: Assign a court-room to a hearing or change the court-room

  Scenario: An unallocated hearing has been listed and assigning a court-centre results in the
            court-centre being assigned to the hearing

    Given hearing listed
    When you changeCourtCentre to a Hearing using a different court centre
    Then court centre changed for hearing

  Scenario: An unallocated hearing has been listed and requesting the original court centre to
            be changed to the same scourt centre does not result in any change

    Given hearing listed
    When you changeCourtCentre to a Hearing using a the same court centre
    Then no events occured

  Scenario: A request to change the court centre for a hearing that has not been listed
            does not result in any change

    Given no previous events
    When you changeCourtCentre to a Hearing using a court centre
    Then no events occured





    