Feature: A listing officer should be able to add, edit, and delete notes per hearing date per court room id
  on allocated and hearing search pages

  Scenario: A listing officer should be able to create a new listing note
    Given no previous events
    # WHEN : ListingNote.createNote(..) aggregate method is called using edit-listing-note-requested.json input
    When you createNote to a ListingNote using a create listing note requested
    # THEN : create-listing-note-created.json event is raised
    Then create listing note created

  Scenario: A listing officer should not be able to create a note
    # GIVEN : listing-note-created.json
    Given listing-note-created
    When you createNote to a ListingNote using a create listing note requested
    Then no events occurred