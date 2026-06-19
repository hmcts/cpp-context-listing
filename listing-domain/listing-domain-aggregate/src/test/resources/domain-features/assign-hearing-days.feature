Feature: Assign hearing days to a hearing or change the hearing days

  Scenario: An unallocated hearing has been listed and assigning hearing days results in the
            calendar being assigned to the hearing

    Given hearing listed
    When you assignHearingDays to a Hearing using a hearing days
    Then hearing days changed for hearing


  Scenario: An unallocated hearing has been listed and assigning hearing days results in the
    calendar being assigned to the hearing with no non default days

    Given hearing listed
    When you assignHearingDays to a Hearing using a start and end date with no non default days
    Then hearing days updated for hearing with default days


  Scenario: An unallocated hearing has been listed and assigning hearing days results in the
    calendar being assigned to the hearing with non default days

    Given hearing listed
    When you assignHearingDays to a Hearing using a start and end date with non default days
    Then hearing days updated for hearing with non default days


  Scenario: A request to change the hearing days for a hearing that has not been listed does not result in any change

    Given no previous events
    When you assignHearingDays to a Hearing using a hearing days
    Then no events occurred


  Scenario: An unallocated hearing has been listed and hearing days assigned. Requesting the original
            hearing days to be changed to the same hearing days does not result in any change

    Given no previous events
    When you assignHearingDays to a Hearing using a the same hearing days
    Then no events occurred


  Scenario: An unallocated hearing has been listed and hearing days assigned. A request to change
  the hearing days when the there is no end date, results in the hearing days being set to emmpty

    Given hearing listed
    And hearing days changed for hearing
    When you assignHearingDays to a Hearing using a empty hearing days
    Then hearing days changed to empty for hearing


  Scenario: An unallocated hearing has been listed and hearing days assigned & sequenced. A request to change
  the hearing days results in new hearing days and maintains the sequences of any existing hearing days

    Given hearing listed
    And hearing days changed for hearing
    And hearing days sequenced for hearing
    When you assignHearingDays to a Hearing using a new hearing days
    Then hearing days changed includes previous sequences


  Scenario: An unallocated hearing has been listed, hearing days assigned & sequenced, the court room has been changed for the hearing.
  A request to change the hearing days results in new hearing days and maintains the sequences of any existing hearing days

    Given hearing listed
    And hearing days changed for hearing
    And hearing days sequenced for hearing
    And court room changed for hearing
    When you assignHearingDays to a Hearing using a new hearing days
    Then hearing days changed has zero sequences


  Scenario: An unallocated hearing has been listed, hearing days assigned & sequenced, the court room has been removed for the hearing.
  A request to change the hearing days results in new hearing days and maintains the sequences of any existing hearing days

    Given hearing listed
    And hearing days changed for hearing
    And hearing days sequenced for hearing
    And court room removed from hearing
    When you assignHearingDays to a Hearing using a new hearing days
    Then hearing days changed has zero sequences
