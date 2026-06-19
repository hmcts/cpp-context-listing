Feature: Cancel remaining hearing days from a multiday hearing and free unused slots when hearing status is set to Cracked or Ineffective

  Scenario: Should cancel remaining hearing days and free unused slots when a multiday hearing is cancelled in Magistrates jurisdiction

    Given hearing allocated for listing with mandatory data and multiple hearing days in magistrates jurisdiction
    When you cancelHearingDays to a Hearing with an indication to hearing days to be cancelled
    Then remaining hearing days are cancelled
    And hearing slots are updated to retain the used slots

  Scenario: Should cancel remaining hearing days and not attempt to free any slots when a multiday hearing is cancelled in Crown jurisdiction

    Given hearing allocated for listing with mandatory data and multiple hearing days in crown jurisdiction
    When you cancelHearingDays to a Hearing with an indication to hearing days to be cancelled
    Then remaining hearing days are cancelled

  Scenario: Should retain the cancelled state of a hearing day when hearing days are cancelled and then sequenced

    Given hearing allocated for listing with mandatory data and multiple hearing days in crown jurisdiction
    When you cancelHearingDays to a Hearing with an indication to hearing days to be cancelled
    And you sequenceHearingDays to a Hearing with a sequence hearing on non cancelled days
    Then remaining hearing days are cancelled
    And allocated hearing updated for listing with cancelled hearing days
    And hearing days sequenced for hearing with cancelled hearing days

  Scenario: Should retain the cancelled state of a hearing day when non cancelled hearing days are sequenced

    Given hearing allocated for listing with mandatory data and multiple hearing days in crown jurisdiction
    And hearing days were cancelled
    When you sequenceHearingDays to a Hearing with a sequence hearing on non cancelled days
    Then allocated hearing updated for listing with cancelled hearing days
    And hearing days sequenced for hearing with cancelled hearing days
