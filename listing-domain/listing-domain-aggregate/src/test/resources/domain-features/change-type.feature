Feature: Change the type of the hearing

  Scenario: An unallocated hearing has been listed and changing the original type to
            a different type results in the hearing type being changed

    Given unallocated-hearing-listed
    When you changeType to a Hearing using a different-type
    Then type-changed-for-hearing


  Scenario: An unallocated hearing has been listed and requesting the original type to
            be changed to the same type does not result in any change

    Given unallocated-hearing-listed
    When you changeType to a Hearing using a the-same-type
    Then no events occured


  Scenario: A request to change the type for a hearing that has not been listed does not result in any change

    Given no previous events
    When you changeType to a Hearing using a trial-type
    Then no events occured
