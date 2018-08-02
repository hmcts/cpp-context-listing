Feature: Send Case for Listing

  Scenario: A request to send a case for listing results in the case being sent

    Given no previous events
    When you sendForListing to a Case using a listing details for ptp
    Then case sent for ptp listing

  Scenario: A previously listed case is sent for another listing

    Given case sent for ptp listing
    When you sendForListing to a Case using a listing details for trial
    Then  case sent for trial listing

