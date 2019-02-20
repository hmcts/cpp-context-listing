Feature: Sequence hearing days

  Scenario: A hearing has been allocated, all mandatory fields have been assigned.
          A request to sequence HearingDays results in sequenced Hearing Days

    Given hearing allocated for listing with mandatory data and multiple hearing days
    When you sequenceHearingDays from a Hearing with a sequence hearing
    Then hearing days sequenced for hearing

  Scenario:  A hearing has been allocated, all mandatory fields have been assigned.
            A request to sequence HearingDays results for a hearing without hearingDays
            does not result in any change.

    Given no previous events
    When you sequenceHearingDays from a Hearing with a sequence hearing
    Then no events occured


  Scenario: A hearing has been allocated, all mandatory fields have been assigned and
            one hearing day sequenced. A request to sequence the second HearingDay results in
            both Hearing Days having the correct sequences

    Given hearing allocated for listing with mandatory data and multiple hearing days
    And   hearing days sequenced for hearing
    When you sequenceHearingDays from a Hearing with a new sequence hearing
    Then hearing days sequences includes previous sequences