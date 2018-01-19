Feature: Allocate a hearing

  Scenario: An unallocated hearing has been listed and a courtroom has been assigned. However, a judge
            has not been assigned which is mandatory for allocation. Applying allocation rules does
            not result in any change.

    Given unallocated hearing listed
      And court room assigned to hearing
    When you applyAllocationRules to a Hearing
    Then no events occured


  Scenario: An unallocated hearing has been listed and a judge has been assigned. However, a court
            room not been assigned which is mandatory for allocation. Applying allocation rules does
            not result in any change.

    Given unallocated hearing listed
      And judge assigned to hearing
    When you applyAllocationRules to a Hearing
    Then no events occured


  Scenario: An unallocated hearing has been listed, a courtroom has been assigned, a judge has been assigned and
            a start time has been assigned. Applying allocation rules results in hearing being allocated

    Given unallocated hearing listed
      And court room assigned to hearing
      And judge assigned to hearing
      And start time assigned to hearing
    When you applyAllocationRules to a Hearing using a no-args
    Then hearing allocated for listing


  Scenario: An unallocated hearing has been listed, a courtroom has been assigned, a judge has been assigned
            but a start time has NOT been assigned. Applying allocation rules results in start time being
            assigned to default value of 10.30am and the hearing being allocated at 10.30am.

    Given unallocated hearing listed
      And court room assigned to hearing
      And judge assigned to hearing
    When you applyAllocationRules to a Hearing using a no-args
    Then default 1030 start time assigned to hearing, hearing allocated for listing at 1030


  Scenario: An unallocated hearing has been listed, all mandatory fields have been assigned, start time has been set to
            09:00am and the hearing has been allocated. The allocated hearing is then updated by removing the start time.
            Applying allocation rules results in the default start time of 10.30am being reassigned and the hearing is
            still allocated at 10.30am.

    Given unallocated hearing listed
      And court room assigned to hearing
      And judge assigned to hearing
      And start time assigned to hearing at 0900
      And hearing allocated for listing,
      And start time removed from hearing
    When you applyAllocationRules to a Hearing using a no-args
    Then default 1030 start time assigned to hearing, allocated hearing updated for listing at 1030


  Scenario: An unallocated hearing has been listed, a courtroom has been assigned and a judge has been assigned which
            are all the mandatory requirements for allocation. However, the judge is then removed. Applying allocation
            rules results does not result in any change.

    Given unallocated hearing listed
      And court room assigned to hearing
      And judge assigned to hearing
      And court room removed from hearing
    When you applyAllocationRules to a Hearing using a no-args
    Then no events occured