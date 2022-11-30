package test.com.csitte.autocloseablelock;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.time.Duration;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Test;

import com.csitte.autocloseablelock.AutoCloseableLock;
import com.csitte.autocloseablelock.CloseableLock;
import com.csitte.autocloseablelock.LockCondition;

public class LockConditionTest
{
    private static final Logger LOG = LogManager.getLogger(LockConditionTest.class);

    @Test
    public void testStringCondition()
    {
        CloseableLock lock = new CloseableLock();
        LockCondition<String> stringCondition = new LockCondition<>(lock, null);
        assertNull(stringCondition.getState());
        assertEquals("", stringCondition.toString());
        stringCondition.setState("123");
        assertEquals("123", stringCondition.getState());
        assertEquals("123", stringCondition.toString());
    }

    @Test
    public void testEnumCondition()
    {
        LOG.debug("INIT");
        CloseableLock lock = new CloseableLock();
        StateCondition stateCondition = new StateCondition(lock);
        assertEquals(State.INIT, stateCondition.getState());
        assertEquals("INIT", stateCondition.toString());
        try (AutoCloseableLock acl = lock.lock())
        {
            new Thread(new Runnable() {

                @Override
                public void run()
                {
                    try (AutoCloseableLock acl2 = lock.lock())
                    {
                        LOG.debug("RUNNING");
                        assertEquals(State.INIT, stateCondition.getState());
                        lock.wait(Duration.ofSeconds(1L));
                        stateCondition.setRunning();
                        LOG.debug("wait for FINISHED");
                        lock.waitForCondition(() -> stateCondition.isFinished(), null);
                        LOG.debug("FINISHED");
                    }
                }
            }).start();
            LOG.debug("wait for RUNNING");
            lock.waitForCondition(() -> stateCondition.isRunning(), null);
            LOG.debug("RUNNING");
        }
        finally
        {
            assertEquals(State.RUNNING, stateCondition.getState());
            LOG.debug("FINISHED");
            stateCondition.setFinished();
        }
    }

    enum State
    {
        INIT,
        RUNNING,
        FINISHED;
    }

    public static class StateCondition extends LockCondition<State>
    {
        public StateCondition(CloseableLock lock)
        {
            super(lock, State.INIT);
        }

        void setRunning()
        {
            setState(State.RUNNING);
        }

        void setFinished()
        {
            setState(State.FINISHED);
        }

        public boolean isRunning()
        {
            return getState() == State.RUNNING;
        }

        public boolean isFinished()
        {
            return getState() == State.FINISHED;
        }
    }

}
