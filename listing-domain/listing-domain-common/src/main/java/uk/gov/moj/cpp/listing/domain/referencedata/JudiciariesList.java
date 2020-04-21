package uk.gov.moj.cpp.listing.domain.referencedata;

import java.util.List;

public class JudiciariesList {

    private List<Judiciary> judiciaries;

    public JudiciariesList(final List<Judiciary> judiciaries) {
        this.judiciaries = judiciaries;
    }

    public List<Judiciary> getJudiciaries() {
        return judiciaries;
    }

    public void setJudiciaries(final List<Judiciary> judiciaries) {
        this.judiciaries = judiciaries;
    }
}
