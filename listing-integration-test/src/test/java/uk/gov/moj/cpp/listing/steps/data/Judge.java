package uk.gov.moj.cpp.listing.steps.data;

import java.util.UUID;

public class Judge {

    private UUID judgeId;
    private String judgeTitle;
    private String judgeFirstName;
    private String judgeLastName;

    public Judge(final UUID judgeId, final String judgeTitle, final String judgeFirstName, final String judgeLastName) {
        this.judgeId = judgeId;
        this.judgeTitle = judgeTitle;
        this.judgeFirstName = judgeFirstName;
        this.judgeLastName = judgeLastName;
    }

    public UUID getJudgeId() {
        return judgeId;
    }

    public String getJudgeTitle() {
        return judgeTitle;
    }

    public String getJudgeFirstName() {
        return judgeFirstName;
    }

    public String getJudgeLastName() {
        return judgeLastName;
    }
}
