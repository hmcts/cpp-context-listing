Feature: Publish Court list

  Scenario: Publish court list requested
    Given no previous events
    When you recordCourtListRequested to a PublishCourtListRequestAggregate using a record court list requested
    Then publish court list requested

  Scenario: Store published court list
    Given publish court list requested
    When you storePublishedCourtList to a PublishCourtListRequestAggregate using a store published court list
    Then published court list stored

  Scenario: Publish court list produced
    Given publish court list requested
    When you recordCourtListProduced to a PublishCourtListRequestAggregate with a record court list produced
    Then publish court list produced

  Scenario: Publish court list export was successful
    Given publish court list requested
    And publish court list produced
    When you recordCourtListExportSuccessful to a PublishCourtListRequestAggregate with a record court list export successful
    Then publish court list export successful

  Scenario: Publish court list export failed
    Given publish court list requested
    And publish court list produced
    When you recordCourtListExportFailed to a PublishCourtListRequestAggregate with a record court list export failed
    Then publish court list export failed
