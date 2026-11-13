package uk.gov.moj.cpp.listing.persistence.entity.query;

/**
 * A judicial id sitting in a court session.
 *
 * @param judicialId the judicial id from {@code properties->'judiciary'}
 * @param session    AM, PM or AD (all day) from the hearing day start and end times
 */
public record JudiciarySession(String judicialId, String session) {
}
