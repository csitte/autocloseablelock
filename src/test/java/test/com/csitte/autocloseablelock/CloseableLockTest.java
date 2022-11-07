package test.com.csitte.autocloseablelock;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import com.csitte.autocloseablelock.AutoCloseableLock;
import com.csitte.autocloseablelock.CloseableLock;
import com.csitte.autocloseablelock.LockCondition.BooleanLockCondition;
import com.csitte.autocloseablelock.LockException;

import test.com.csitte.autocloseablelock.CloseableLockTest.ThreadObject.MODE;


/**
 * Tests for CloseableLock class
 */
class CloseableLockTest
{
    private static final Logger LOG = LogManager.getLogger(CloseableLockTest.class);

    private static final Duration SEC10 = Duration.ofSeconds(10);
    private static final Duration SEC2 = Duration.ofSeconds(2);

    @ParameterizedTest
    @EnumSource(value = MODE.class, names = { "TRY_LOCK_ZERO", "TRY_LOCK_NULL"})
    void testTryLock(MODE mode)
    {
    	LOG.debug("-> testTryLock({})", mode);
        CloseableLock lock = new CloseableLock();
        CloseableLock waitLock = new CloseableLock();
        try (AutoCloseableLock acl = lock.lock())
        {
            ThreadObject thread = new ThreadObject(lock, mode, null); // no wait
            LOG.debug("start thread & try to aquire lock (w/o wait), which goes wrong");
            thread.start(); // start thread & try to aquire lock (w/o wait), which goes wrong
            boolean status = waitLock.waitForCondition(()->thread.isFinished(), SEC10);
            assertTrue(status);
            assertNotNull(thread.getException());

            ThreadObject thread3 = new ThreadObject(lock, mode, null); // no wait
            thread3.start(); // start thread & try to aquire lock (w/o wait)
            status = waitLock.waitForCondition(()->thread3.isFinished(), null);
            assertTrue(status);
            assertNotNull(thread3.getException());
        }
        ThreadObject thread2 = new ThreadObject(lock, mode, null); // no wait
        thread2.start(); // start thread & try to aquire lock (w/o wait)
        boolean status = waitLock.waitForCondition(()->thread2.isFinished(), SEC10);
        assertTrue(status);
        assertNull(thread2.getException());

        ThreadObject thread4 = new ThreadObject(lock, mode, SEC2); // no wait
        thread4.start(); // start thread & try to aquire lock (w/o wait)
        status = waitLock.waitForCondition(()->thread4.isFinished(), SEC10);
        assertTrue(status);
        assertNull(thread4.getException());
    }

    @Test
    void testTryLockNeg()
    {
        boolean status;
        ThreadObject thread;
        CloseableLock lock = new CloseableLock();
        try (AutoCloseableLock acl = lock.lock())
        {
            thread = new ThreadObject(lock, MODE.TRY_LOCK_NEG, null); // wait w/o timeout
            thread.start();
            //- Wait (max 10s) until thread is started
            status = lock.waitForCondition(()->thread.isStarted(), SEC10);
            assertTrue(status);
            assertFalse(thread.isFinished()); // thread should be waiting

            //- Wait (max 10s) until thread is finished
            status = lock.waitForCondition(()->thread.isFinished(), SEC10);
        }
        assertTrue(status); // thread-is-finished == true
        assertNull(thread.getException()); // no (timeout-)exception
    }

    @Test
    void testTryLockTimeout()
    {
        CloseableLock lock = new CloseableLock();
        try (AutoCloseableLock acl = lock.lock())
        {
            ThreadObject thread = new ThreadObject(lock, MODE.TRY_LOCK_3S_TIMEOUT, null); // wait w/o timeout
            thread.start();
            //- Wait (max 10s) until thread is started
            boolean status = lock.waitForCondition(()->thread.isStarted(), SEC10);
            assertTrue(status);

            new CloseableLock().wait(Duration.ofSeconds(6)); // block lock for 6 seconds, timeout in thread after 3 sec
            assertTrue(thread.isFinished());
            assertNotNull(thread.getException());
        }
    }

    @Test
    void testWaitInterrupted()
    {
        CloseableLock lock = new CloseableLock();
        try (AutoCloseableLock acl = lock.lock())
        {
            ThreadObject thread = new ThreadObject(lock, MODE.WAIT_5S, null); // wait 5sec
            thread.start();
            //- Wait (max 10s) until thread is started
            boolean status = lock.waitForCondition(()->thread.isStarted(), SEC10);
            assertTrue(status);

            thread.interrupt();

            acl.waitForCondition(() -> thread.isFinished(), SEC10);
            assertNotNull(thread.getException());
        }
    }

    @Test
    void testLockConstructor()
    {
        ReentrantLock baseLock = new ReentrantLock();
        new CloseableLock(baseLock);
    }

    @Test
    void testCondition()
    {
        CloseableLock lock = new CloseableLock();
        BooleanLockCondition condition = new BooleanLockCondition(lock);
        assertEquals(Boolean.FALSE, condition.getState());
    }

    @Test
    void testWait()
    {
        CloseableLock lock = new CloseableLock();
        try (AutoCloseableLock acl = lock.lock())
        {
            acl.wait(SEC2); // block lock for 2 seconds
            assertThrows(LockException.class, () -> acl.wait(null));
            assertThrows(LockException.class, () -> acl.wait(Duration.ofNanos(0L)));
        }
    }

    @Test
    void testSignal()
    {
        CloseableLock lock = new CloseableLock();
        lock.signal();
        lock.signalAll();
        try (AutoCloseableLock acl = lock.lock())
        {
            acl.signal();
            acl.signalAll();
            new BooleanLockCondition(lock); // create condition
            acl.signal();
            acl.signalAll();
        }
    }

    /**
     * Helper Object
     */
    public static class ThreadObject extends Thread
    {
        public static enum MODE
        {
            TRY_LOCK_NULL,
            TRY_LOCK_ZERO,
            TRY_LOCK_NEG,
            TRY_LOCK_3S_TIMEOUT,
            WAIT_5S;
        };

        private final MODE mode;

        private CloseableLock lock;
        private LockException exception;
        private boolean started;
        private boolean finished;
        private Duration waitAtEnd;

        public ThreadObject(CloseableLock lock, MODE mode, Duration waitAtEnd)
        {
            this.lock = lock;
            this.mode = mode;
            this.waitAtEnd = waitAtEnd;
        }

        public LockException getException()
        {
            return exception;
        }

        public boolean isFinished()
        {
            return finished;
        }

        public boolean isStarted()
        {
            return started;
        }

        @Override
        public void run()
        {
            started = true;
            try
            {
                switch (mode)
                {
                    case TRY_LOCK_NULL:
                        try (AutoCloseableLock acl = lock.tryLock(null)) // no wait
                        {
                        }
                        break;

                    case TRY_LOCK_ZERO:
                        try (AutoCloseableLock acl = lock.tryLock(Duration.ofSeconds(0))) // no wait
                        {
                        }
                        break;

                    case TRY_LOCK_NEG:
                        try (AutoCloseableLock acl = lock.tryLock(Duration.ofSeconds(-1))) // wait w/o timeout
                        {
                        }
                        break;

                    case TRY_LOCK_3S_TIMEOUT:
                        try (AutoCloseableLock acl = lock.tryLock(Duration.ofSeconds(3))) // wait with 3 sec timeout
                        {
                        }
                        break;

                    case WAIT_5S:
                        lock.wait(Duration.ofSeconds(5));

                    default:
                        break;
                }
            }
            catch (LockException x)
            {
                exception = x;
            }
            if (waitAtEnd != null)
            {
                new CloseableLock().wait(waitAtEnd);
            }
            finished = true;
        }
    }
}
