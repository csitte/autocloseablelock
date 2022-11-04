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

class ActivityTest
{
    private static final Logger LOG = LogManager.getLogger(ActivityTest.class);

	@Test
	void test()
	{
	    LOG.debug("new TestActivity()");
		DummyActivity activity = new DummyActivity();
		assertFalse(activity.isActive());
		assertNull(activity.getStartOfActivity());
		assertNotNull(activity.getLastActivity());
		assertNull(activity.getEndOfActivity());
		assertEquals("", activity.getStatus());

		activity.setShutdownRequest(false); // only for junit test

	    LOG.debug("start of test-activity");
		try (CloseableActivity ca = activity.startActivity("start of test-activity"))
		{
			assertTrue(activity.isActive());
			assertNull(activity.getEndOfActivity());

			assertThrows(ActivityRuntimeException.class, () -> activity.startActivity("invalid second start"));

			activity.updateStatus("activity in progress");
			assertEquals("activity in progress", activity.getStatus());

			activity.errorStatus("error-message-test");
			assertEquals("error-message-test", activity.getStatus());

		    LOG.debug("startActivityThread");
			activity.startActivityThread();

		    LOG.debug("Wait until activity is running");
			assertTrue(activity.getLock().waitForCondition(() -> activity.isThreadRunning(), null));

		    LOG.debug("Wait 1 1/2 second");
			activity.getLock().waitForCondition(() -> false, Duration.ofMillis(1500));

            Instant instant = activity.touch();

		    LOG.debug("Request shutdown");
			activity.setShutdownRequest(true);

		    LOG.debug("Wait until activity ended");
			assertTrue(activity.getLock().waitForCondition(() -> !activity.isThreadRunning(), Duration.ofSeconds(5)));

            LOG.debug("touch activity()");
            assertTrue(instant.isBefore(activity.touch()));
		}
	    LOG.debug("end of activity");
		assertFalse(activity.isActive());
		assertThrows(ActivityRuntimeException.class, () -> activity.close());
        assertThrows(ActivityRuntimeException.class, () -> activity.touch());
	}
}
