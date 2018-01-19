Feature: Send Case for Listing

  Scenario: A request to send a case for listing results in the case being sent

    Given no previous events
    When you sendForListing to a Case using a new case details
    Then case sent for listing

