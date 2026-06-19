Feature: A listing officer should be able to add, edit, and delete notes per hearing date per court room id
  on allocated and hearing search pages

  Scenario: A listing officer should be able to edit a previously created note
    # GIVEN : listing-note-created.json
    Given listing note created
    # WHEN : ListingNote.editNote(..) aggregate method is called using edit-listing-note-requested.json input
    When you editNote to a ListingNote using a edit listing note requested
    # THEN : listing-note-edited.json event is raised
    Then listing note edited

  Scenario: A listing officer should not be able to edit a note which was not created
    Given no previous events
    When you editNote to a ListingNote using a edit listing note requested
    Then no events occurred