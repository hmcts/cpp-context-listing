Feature: Unallocate a hearing

  Scenario: A hearing has been allocated, all mandatory fields have been assigned.  The allocated
            hearing is then updated by removing the court-room. Applying allocation rules results
            in the hearing being unallocated.

    Given hearing allocated for listing with mandatory data
      And court room removed from hearing
    When you applyAllocationRules to a Hearing using a no args
    Then hearing unallocated for listing

