Feature: Publish Court list

  Scenario: Publish court list requested
    Given no previous events
    When you recordCourtListRequested to a PublishCourtListRequestAggregate using a record court list requested
    Then publish court list requested
