Feature: Vacating a hearing initiated from hearing

  Scenario: Should vacate the hearing and free allocated slots when vacating reason is provided and jurisdiction is Magistrates

    Given hearing listed
    And you changeJurisdictionType to a Hearing using a different jurisdiction
    When you hearingVacateTrial to a Hearing with a vacate reason
    Then jurisdiction changed for hearing
    And hearing gets vacated from listing
    And available slots for hearing are freed

  Scenario: Should vacate the hearing and free allocated slots when vacating reason is provided and jurisdiction is Crown

    Given hearing listed
    When you hearingVacateTrial to a Hearing with a vacate reason
    Then hearing gets vacated from listing
    And available slots for hearing are freed

  Scenario: Should not attempt to free any slots when a hearing is vacated in Magistrates jurisdiction and vacate reason is missing

    Given hearing listed
    And you changeJurisdictionType to a Hearing using a different jurisdiction
    When you hearingVacateTrial to a Hearing with an empty vacate reason
    Then jurisdiction changed for hearing
    And hearing gets vacated with empty vacate reason



