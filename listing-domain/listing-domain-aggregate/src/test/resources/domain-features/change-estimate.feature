Feature: Change the estimate for the hearing

  Scenario: An unallocated hearing has been listed and changing the original estimate to a
            different estimate results in the hearing estimate being changed

    Given unallocated hearing listed
    When you changeEstimate to a Hearing using a different estimate
    Then estimate minutes changed for hearing


  Scenario: An unallocated hearing has been listed and requesting the original estimate to
            be changed to the same estimate does not result in any change

    Given unallocated hearing listed
    When you changeEstimate to a Hearing using a the same estimate
    Then no events occured


  Scenario: A request to change the estimate for a hearing that has not been listed does not result in any change

    Given no previous events
    When you changeEstimate to a Hearing using a ten minute estimate
    Then no events occured
