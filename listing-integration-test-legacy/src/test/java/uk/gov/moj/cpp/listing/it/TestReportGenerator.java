package uk.gov.moj.cpp.listing.it;

import org.junit.platform.launcher.TestExecutionListener;
import org.junit.platform.launcher.TestPlan;

public class TestReportGenerator implements TestExecutionListener {
    @Override
    public void testPlanExecutionFinished(TestPlan testPlan) {
        if (Boolean.getBoolean("generate.test.report")) {
            TestDurationListener.generateReport();
        }
    }
} 