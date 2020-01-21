Feature: Eject case for Hearings

  Scenario: An unallocated hearing has been listed. When the case is ejected then event is raised.

    Given hearing listed
    When you ejectCaseForHearings on a Case using a case and reason for hearings
    Then case is ejected for hearings

  Scenario: A hearing has been listed with the necessary fields for allocation. When the case is ejected then event is raised.

    Given hearing listed with data for allocation
    And hearing days changed for hearing
    When you ejectCaseForHearings on a Case using a case and reason for hearings
    Then case is ejected for hearings

  Scenario: A case has multiple hearings and only one hearing is allocated. When the case is ejected then event is raised for all the hearings.

    Given hearing listed
    And hearing days changed for hearing
    And hearing added to case for allocated hearing
    And hearing added to case for unallocated hearing
    When you ejectCaseForHearings on a Case using a case and reason for hearings
    Then case is ejected for all hearings

  Scenario: An unallocated hearing has been listed. When the case is ejected and no hearingId passed then no event is raised

    Given no previous events
    When you ejectCaseForHearings on a Case using a case and reason without hearingId
    Then no events occurred