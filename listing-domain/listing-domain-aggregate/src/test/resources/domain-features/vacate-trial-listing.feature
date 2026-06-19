Feature: Vacating a hearing initiated from listing

  Scenario: Should vacate a hearing and free allocated slots when hearing is allocated and jurisdiction is Magistrates

    Given hearing allocated for listing with mandatory data and multiple hearing days
    And you changeJurisdictionType to a Hearing using a different jurisdiction
    When you vacateTrial to a Hearing with a vacate reason and hearing id
    Then jurisdiction changed for hearing
    And allocated hearing gets vacated from listing
    And available slots for hearing are freed

  Scenario: Should not attempt to free any slots when an allocated hearing is vacated and jurisdiction is Crown

    Given hearing allocated for listing with mandatory data and multiple hearing days
    When you vacateTrial to a Hearing with a vacate reason and hearing id
    Then allocated hearing gets vacated from listing

  Scenario: Should not attempt to free any slots when unallocated hearing is vacated and jurisdiction is Magistrates

    Given hearing listed
    And you changeJurisdictionType to a Hearing using a different jurisdiction
    When you vacateTrial to a Hearing with a vacate reason and hearing id
    Then jurisdiction changed for hearing
    And unallocated hearing gets vacated from listing



