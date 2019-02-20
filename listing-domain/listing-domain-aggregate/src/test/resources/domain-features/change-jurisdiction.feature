Feature: Change the jurisdiction of the hearing

  Scenario: An unallocated hearing has been listed and changing the original jurisdiction to
            a different jurisdiction results in the hearing type being changed

    Given hearing listed
    When you changeJurisdictionType to a Hearing using a different jurisdiction
    Then jurisdiction changed for hearing


  Scenario: An unallocated hearing has been listed and requesting the original jurisdiction to
            be changed to the same jurisdiction does not result in any change

    Given hearing listed
    When you changeJurisdictionType to a Hearing using a the same jurisdiction
    Then no events occured


  Scenario: A request to change the jurisdiction for a hearing that has not been listed does not result in any change

    Given no previous events
    When you changeJurisdictionType to a Hearing using a magistrates jurisdiction
    Then no events occured
