Feature: Unallocate a hearing

  # TODO: Think about what tests go in here
  # The allocate-hearing event only sets allocate=true. Rather than having to list each event
  # separately is it possible to specify a file name which contains multiple events that can
  # be applied so as to ensure clarity in the test specification, eg.
  # hearing-allocated-for-listing-with-mandatory-data = a file containing 4/5 events,
  # unallocated-hearing-listed, judge-assigned-to-hearing, court-room-assigned-to-hearing, hearing-allocated-for-listing

  Scenario: An unallocated hearing has been listed and a courtroom has been assigned. However, a judge
            has not been assigned which is mandatory for allocation. Applying allocation rules does
            not result in any change.

    Given there are previous events unallocated-hearing-listed,court-room-assigned-to-hearing
    When applyAllocationRules to a uk.gov.moj.cpp.listing.domain.aggregate.Hearing using no-args
    #Then the no events occurred


  Scenario: An unallocated hearing has been listed and a judge has been assigned. However, a court
            room not been assigned which is mandatory for allocation. Applying allocation rules does
            not result in any change.

    Given there are previous events unallocated-hearing-listed,judge-assigned-to-hearing
    When applyAllocationRules to a uk.gov.moj.cpp.listing.domain.aggregate.Hearing using no-args
    #Then the no events occurred


  Scenario: An unallocated hearing has been listed, a courtroom has been assigned, a judge has been assigned and
            a start time has been assigned. Applying allocation rules results in hearing being allocated

    Given there are previous events unallocated-hearing-listed,court-room-assigned-to-hearing,judge-assigned-to-hearing,start-time-assigned-to-hearing
    When applyAllocationRules to a uk.gov.moj.cpp.listing.domain.aggregate.Hearing using no-args
    Then the hearing-allocated-for-listing


  Scenario: An unallocated hearing has been listed, a courtroom has been assigned, a judge has been assigned
            but a start time has NOT been assigned. Applying allocation rules results in start time being
            assigned to default value of 10.30am and the hearing being allocated at 10.30am.

    Given there are previous events unallocated-hearing-listed,court-room-assigned-to-hearing,judge-assigned-to-hearing
    When applyAllocationRules to a uk.gov.moj.cpp.listing.domain.aggregate.Hearing using no-args
    Then the default-1030-start-time-assigned-to-hearing,hearing-allocated-for-listing-at-1030