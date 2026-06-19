Feature: Eject case

  Scenario: An unallocated hearing has been listed. When the case is ejected then event is raised.

    Given hearing listed
    When you ejectCase on a Hearing using a case and reason
    Then case is ejected

  Scenario: A hearing has been listed with the necessary fields for allocation. When the case is ejected then event is raised.

    Given hearing listed with data for allocation
    And hearing days changed for hearing
    When you ejectCase on a Hearing using a case and reason
    Then case is ejected

  Scenario: An unallocated hearing has been listed. When the case is ejected and no hearingId passed then no event is raised

    Given no previous events
    When you ejectCase on a Hearing using a case and reason without hearingId
    Then no events occurred