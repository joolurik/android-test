package androidx.test.orchestrator.listeners;

import android.util.Log;

import androidx.test.orchestrator.junit.ParcelableDescription;
import androidx.test.orchestrator.junit.ParcelableFailure;
import androidx.test.orchestrator.listeners.result.TestResult;

import java.io.PrintStream;
import java.util.HashMap;
import java.util.Map;

public class OrchestratorRetryHelper extends OrchestrationRunListener  {

    private static final String TAG = "AndroidTestOrchestrator";
    private static final int MAX_RETRY_COUNT = 3;

    private int testCount = -1;
    private String currentTest = null;
    private TestResult.TestStatus currentTestResult = null;
    private Map<String, Integer> executionCounter = new HashMap<>();
    private Map<String, TestResult.TestStatus> resultTracker = new HashMap<>();

    @Override
    public void testStarted(ParcelableDescription description) {
        currentTest = testNameFromParcel(description);
        currentTestResult = TestResult.TestStatus.PASSED;
    }

    @Override
    public void testFinished(ParcelableDescription description) {
        resultTracker.put(currentTest, currentTestResult);
        if(executionCounter.containsKey(currentTest)) {
            executionCounter.put(currentTest, executionCounter.get(currentTest) + 1);
        } else {
            executionCounter.put(currentTest, 1);
        }
        currentTest = null;
    }

    @Override
    public void testFailure(ParcelableFailure failure) {
        currentTestResult = TestResult.TestStatus.FAILURE;
    }

    @Override
    public void testIgnored(ParcelableDescription description) {
        currentTestResult = TestResult.TestStatus.IGNORED;
    }

    @Override
    public void testAssumptionFailure(ParcelableFailure failure) {
        currentTestResult = TestResult.TestStatus.ASSUMPTION_FAILURE;
    }

    @Override
    public void orchestrationRunStarted(int testCount) {
        this.testCount = testCount;
    }

    public boolean isTestNeedsRetry(final String testDisplayName) {
        if(testDisplayName == null || testDisplayName.isEmpty()) {
            return false;
        }
        final ParcelableDescription description = new ParcelableDescription(testDisplayName);
        final String testName = testNameFromParcel(description);
        if(! (resultTracker.containsKey(testName) && executionCounter.containsKey(testName))) {
            return true;
        }
        return !resultTracker.get(testName).equals(TestResult.TestStatus.PASSED) &&
                executionCounter.get(testName) < MAX_RETRY_COUNT;
    }

    public void writeResults(final PrintStream resultStream) {
        StringBuffer buffer = new StringBuffer("OrchestratorRetryHelper result: ");
        if((testCount == resultTracker.size()) && isAllTestPassed()) {
            buffer.append("all Tests passed.");
        } else {
            buffer.append("Tests failed, please check failure details above.");
        }
        resultStream.println(buffer.toString());
    }

    private boolean isAllTestPassed() {
        return resultTracker.entrySet().stream()
                .allMatch(entry -> entry.getValue().equals(TestResult.TestStatus.PASSED));
    }

    private String testNameFromParcel(final ParcelableDescription description) {
        return description.getClassName() + "#" + description.getMethodName();
    }
}
