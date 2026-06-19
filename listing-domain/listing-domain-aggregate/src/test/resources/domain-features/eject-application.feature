Feature: Eject application for Hearings

  Scenario: An unallocated hearing has been listed. When the application is ejected then event is raised.

    Given hearing listed
    When you ejectApplicationForHearings on a Application using a application and reason for hearings
    Then application is ejected for hearings

  Scenario: A hearing has been listed with the necessary fields for allocation. When the application is ejected then event is raised.

    Given hearing listed with data for allocation
    And hearing days changed for hearing
    When you ejectApplicationForHearings on a Application using a application and reason for hearings
    Then application is ejected for hearings

  Scenario: An Application has multiple hearings and only one hearing is allocated. When the application is ejected then event is raised for all the hearings.

    Given hearing listed
    And hearing days changed for hearing
    And hearing added to application for allocated hearing
    And hearing added to application for unallocated hearing
    When you ejectApplicationForHearings on a Application using a application and reason for hearings
    Then application is ejected for all hearings

  Scenario: An unallocated hearing has been listed. When the application is ejected and no hearingId passed then no event is raised

    Given no previous events
    When you ejectApplicationForHearings on a Application using a application and reason without hearingId
    Then no events occurred