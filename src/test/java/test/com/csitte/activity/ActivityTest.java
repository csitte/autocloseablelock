package test.com.csitte.activity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.time.Instant;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Test;

import com.csitte.activity.ActivityRuntimeException;
import com.csitte.activity.CloseableActivity;

@SuppressWarnings("PMD")
class ActivityTest
{
    private static final Logger LOG = LogManager.getLogger(ActivityTest.class);

	@Test
	void test()
	{
	    LOG.debug("new TestActivity()");
		final DummyActivity activity = DummyActivity.createInstance();
		assertFalse(activity.isActive(), "is active");
		assertNull(activity.getStartOfActivity());
		assertNotNull(activity.getLastActivity());
		assertNull(activity.getEndOfActivity());
		assertNull(activity.getStatus());

		activity.setShutdownRequest(false); // only for junit test

	    LOG.debug("start of test-activity");
		try (CloseableActivity closeableActivity = activity.startActivity())
		{
		    assert closeableActivity != null;
			assertTrue(activity.isActive());
			assertNull(activity.getEndOfActivity());

			assertThrows(ActivityRuntimeException.class, activity::startActivity);

			activity.updateStatus("activity in progress");
			assertEquals("activity in progress", activity.getStatus());

		    LOG.debug("startActivityThread");
			activity.startActivityThread();

		    LOG.debug("Wait until activity is running");
			assertTrue(activity.getLock().waitForCondition(activity::isThreadRunning, null));

		    LOG.debug("Wait 1 1/2 second");
			activity.getLock().waitForCondition(() -> false, Duration.ofMillis(1500));

            final Instant instant = activity.touch();

		    LOG.debug("Request shutdown");
			activity.setShutdownRequest(true);

		    LOG.debug("Wait until activity ended");
			assertTrue(activity.getLock().waitForCondition(() -> !activity.isThreadRunning(), Duration.ofSeconds(5)));
            activity.getLock().waitForCondition(() -> false, Duration.ofMillis(100));

            LOG.debug("touch activity()");
            assertTrue(instant.isBefore(activity.touch()));
		}
	    LOG.debug("end of activity");
		assertFalse(activity.isActive());
		assertThrows(ActivityRuntimeException.class, activity::close);
        assertThrows(ActivityRuntimeException.class, activity::touch);
        LOG.debug(activity::toString);
	}

    @Test
    public void testToString()
    {
        DummyActivity activity = DummyActivity.createInstance();
        String initial = activity.toString();
        assertTrue(initial.contains("startOfActivity= "));
        assertTrue(initial.contains("lastActivity=1970-01-01T00:00:00Z"));
        assertTrue(initial.contains("endOfActivity= "));
        assertTrue(initial.endsWith("status=null"));

        try (CloseableActivity ignored = activity.startActivity())
        {
            String running = activity.toString();
            assertFalse(running.contains("startOfActivity= "));
            assertTrue(running.contains("endOfActivity= "));
            assertTrue(running.contains("status=null"));
        }

        String finished = activity.toString();
        assertFalse(finished.contains("startOfActivity= "));
        assertFalse(finished.contains("endOfActivity= "));
        assertTrue(finished.contains("status=null"));
    }
}
