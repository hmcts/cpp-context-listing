package uk.gov.moj.cpp.listing.persistence.entity;

import java.util.Objects;
import java.util.UUID;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name = "offence_listing_numbers")
public class ListingNumbers {

    @Id
    @Column(name = "offence_id")
    private UUID offenceId;

    @Column(name = "listing_number")
    private int listingNumber;

    public ListingNumbers() {
    }

    public ListingNumbers(UUID offenceId, int listingNumber) {
        this.offenceId = offenceId;
        this.listingNumber = listingNumber;
    }

    public UUID getOffenceId() {
        return offenceId;
    }

    public void setOffenceId(final UUID offenceId) {
        this.offenceId = offenceId;
    }

    public int getListingNumber() {
        return listingNumber;
    }

    public void setListingNumber(final int listingNumber) {
        this.listingNumber = listingNumber;
    }

    public void increaseListingNumber() {
        this.listingNumber ++;
    }


    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        final ListingNumbers notes = (ListingNumbers) o;
        return offenceId.equals(notes.offenceId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(offenceId);
    }
}
