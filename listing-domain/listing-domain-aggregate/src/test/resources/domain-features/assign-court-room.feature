Feature: Assign a court-room to a hearing or change the court-room

  Scenario: An unallocated hearing has been listed and assigning a court-room results in the
            court-room being assigned to the hearing

    Given unallocated-hearing-listed
    When you assignCourtRoom to a Hearing using a court-room
    Then court-room-assigned-to-hearing


  Scenario: An unallocated hearing has been listed and a court-room assigned. Changing the court-room
            to a different court-room results in the court-room being changed for hearing

    Given unallocated-hearing-listed,court-room-assigned-to-hearing
    When you assignCourtRoom to a Hearing using a different-court-room
    Then court-room-changed-for-hearing


  Scenario: An unallocated hearing has been listed and a court-room assigned. Requesting the original
            court-room to be changed to the same court-room does not result in any change

    Given unallocated-hearing-listed,court-room-assigned-to-hearing
    When you assignCourtRoom to a Hearing using a the-same-court-room
    Then no events occured