Feature: Allocate an updated hearing

  Scenario: A hearing has been listed with the necessary fields for allocation. When a courtroom assigned is changed,
            then the hearing is updated and listed for allocation

    Given hearing listed with changed data for allocation
    And hearing days changed for hearing
    And hearing allocated for listing with is update slot false
    And you applyAllocationRules to a Hearing using a defendants offences
    Then allocated hearing updated for listing
