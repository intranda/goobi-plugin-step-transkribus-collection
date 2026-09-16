package de.intranda.goobi.plugins;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;

import org.junit.Test;

public class TranskribusHelperTest {

    @Test
    public void testExtractJobIdReturnsTrimmedId() throws IOException {
        assertEquals("1234567", TranskribusHelper.extractJobId(200, "1234567"));
        assertEquals("1234567", TranskribusHelper.extractJobId(200, " 1234567\n"));
    }

    @Test
    public void testExtractJobIdRejectsErrorStatus() {
        try {
            TranskribusHelper.extractJobId(503, "Service Temporarily Unavailable");
            fail("expected an IOException for a failed request");
        } catch (IOException e) {
            assertTrue(e.getMessage(), e.getMessage().contains("503"));
            assertTrue(e.getMessage(), e.getMessage().contains("Service Temporarily Unavailable"));
        }
    }

    @Test
    public void testExtractJobIdRejectsNonNumericBody() {
        try {
            TranskribusHelper.extractJobId(200, "<html><body>Session expired</body></html>");
            fail("expected an IOException for a body that is no job id");
        } catch (IOException e) {
            assertTrue(e.getMessage(), e.getMessage().contains("Session expired"));
        }
    }

    @Test
    public void testExtractJobIdRejectsEmptyBody() {
        try {
            TranskribusHelper.extractJobId(200, "");
            fail("expected an IOException for an empty body");
        } catch (IOException e) {
            assertTrue(e.getMessage(), e.getMessage().contains("job id"));
        }
    }

    @Test
    public void testParseJobReadsDocId() throws IOException {
        TranskribusJob job = TranskribusHelper.parseJob("42", 200, "{\"jobId\":\"42\",\"docId\":4711,\"state\":\"FINISHED\"}");
        assertEquals(Integer.valueOf(4711), job.getDocId());
    }

    /** This is the failure reported from production: Transkribus answers with a plain text message instead of JSON. */
    @Test
    public void testParseJobReportsPlainTextAnswerInsteadOfJsonParseException() {
        try {
            TranskribusHelper.parseJob("42", 200, "jobId not found: 42");
            fail("expected an IOException naming the unexpected answer");
        } catch (IOException e) {
            assertTrue(e.getMessage(), e.getMessage().contains("jobId not found: 42"));
            assertTrue(e.getMessage(), e.getMessage().contains("42"));
        }
    }

    @Test
    public void testParseJobRejectsErrorStatus() {
        try {
            TranskribusHelper.parseJob("42", 404, "jobId not found: 42");
            fail("expected an IOException for a failed request");
        } catch (IOException e) {
            assertTrue(e.getMessage(), e.getMessage().contains("404"));
        }
    }
}
