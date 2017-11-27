Feature: Allocate a hearing

  Scenario: An unallocated hearing has been listed and a courtroom has been assigned,
            then assigning a judge results in hearing being allocated

    # Issue that have to add 'start-time'assigned-to-hearing' even though not required because won't generate event members with null values

    Given there are previous events unallocated-hearing-listed,court-room-assigned-to-hearing,start-time-assigned-to-hearing
    When assignJudge to a uk.gov.moj.cpp.listing.domain.aggregate.HearingAggregate using a-judge
    Then the judge-assigned-to-hearing,hearing-allocated-for-listing

  Scenario: An unallocated hearing has been listed and a judge has been assigned,
            then assigning a courtroom results in hearing being allocated

    Given there are previous events unallocated-hearing-listed,judge-assigned-to-hearing,start-time-assigned-to-hearing
    When assignCourtRoom to a uk.gov.moj.cpp.listing.domain.aggregate.HearingAggregate using a-court-room
    Then the court-room-assigned-to-hearing,hearing-allocated-for-listing

  Scenario: When a hearing is allocated and the start-time is not set, then it is defaulted to 10.30am