package test.com.csitte.autocloseablelock;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicBoolean;
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
import com.csitte.autocloseablelock.LockTimeoutException;

import test.com.csitte.autocloseablelock.CloseableLockTest.ThreadObject.MODE;


/**
 * Tests for CloseableLock class
 */
@SuppressWarnings("PMD")
public class CloseableLockTest
{
    private static final Logger LOG = LogManager.getLogger(CloseableLockTest.class);

    private static final Duration MS500 = Duration.ofMillis(500);
    private static final Duration MS200 = Duration.ofMillis(200);
    private static final Duration MS100 = Duration.ofMillis(100);

    /**
     * Tests for lock() and tryLock() methods
     * @throws InterruptedException
     */
    @Test
    public void testLock() throws InterruptedException
    {
        CloseableLock lock = new CloseableLock();
        AtomicBoolean locked = new AtomicBoolean(false);

        Thread thread = new Thread(() -> {
            try (AutoCloseableLock autoCloseableLock = lock.lock())
            {
                locked.set(true);
                Thread.sleep(100);
            }
            catch (InterruptedException e)
            {
                e.printStackTrace();
            }
        });
        thread.start();
        assertTrue(lock.waitForCondition(locked::get, MS500));

        thread.join();
    }

    /**
     * Tests for tryLock() method with timeout
     * @throws InterruptedException
     */
    @Test
    public void testTryLock() throws InterruptedException
    {
        CloseableLock lock = new CloseableLock();
        AtomicBoolean locked = new AtomicBoolean(false);
        Thread thread = new Thread(() -> {
            try (AutoCloseableLock autoCloseableLock = lock.tryLock(MS200))
            {
                locked.set(true);
                Thread.sleep(100);
            }
            catch (InterruptedException e)
            {
                e.printStackTrace();
            }
        });

        thread.start();
        assertTrue(lock.waitForCondition(locked::get, MS500));

        thread.join();
    }

    /**
     * Tests for lockInterruptibly() method
     * @throws InterruptedException
     */
    @Test
    public void testLockInterruptibly() throws InterruptedException
    {
        CloseableLock lock = new CloseableLock();
        AtomicBoolean locked = new AtomicBoolean(false);

        Thread thread = new Thread(() -> {
            try (AutoCloseableLock autoCloseableLock = lock.lockInterruptibly())
            {
                locked.set(true);
                Thread.sleep(100);
            }
            catch (InterruptedException e)
            {
                e.printStackTrace();
            }
        });
        thread.start();
        assertTrue(lock.waitForCondition(locked::get, MS500));
        thread.interrupt();
        thread.join();
        assertTrue(locked.get());   // "Lock acquired"
    }

    @ParameterizedTest
    @EnumSource(value = MODE.class, names = { "TRY_LOCK_ZERO", "TRY_LOCK_NULL"})
    void testTryLock(MODE mode) throws InterruptedException
    {
        LOG.debug("-> testTryLock({})", mode);
        CloseableLock lock = new CloseableLock();

        try (AutoCloseableLock acl = lock.lock())
        {
            ThreadObject thread = new ThreadObject(lock, mode, null);
            thread.start();
            thread.join(500); // Wait max 500ms for thread to finish
            assertTrue(thread.isFinished(), "Thread should finish quickly");
            assertNotNull(thread.getException(), "Should throw exception when lock unavailable");
        }
    }

    /**
     * tryLock must succeed once the lock is released by the holding thread.
     */
    @ParameterizedTest
    @EnumSource(value = MODE.class, names = { "TRY_LOCK_ZERO", "TRY_LOCK_NULL"})
    void testTryLockSuccessAfterRelease(MODE mode) throws InterruptedException
    {
        CloseableLock lock = new CloseableLock();
        ThreadObject thread = new ThreadObject(lock, mode, null);
        thread.start();
        thread.join(500);
        assertTrue(thread.isFinished(), "Thread should finish quickly");
        assertNull(thread.getException(), "Should acquire lock after release");
    }

    /**
     * Multi-second tryLock timeouts must use total nanoseconds, not only the nanosecond component.
     */
    @Test
    void testTryLockMultiSecondTimeout() throws InterruptedException
    {
        CloseableLock lock = new CloseableLock();
        try (AutoCloseableLock acl = lock.lock())
        {
            final long startNanos = System.nanoTime();
            ThreadObject thread = new ThreadObject(lock, MODE.TRY_LOCK_1S_TIMEOUT, null);
            thread.start();

            //- Keep the lock held; do not use lock.wait() because that releases the lock.
            Thread.sleep(1500);
            thread.join(500);

            assertTrue(thread.isFinished(), "Thread should finish after timeout elapses");
            assertNotNull(thread.getException(), "Should throw when lock stays unavailable");
            assertTrue(thread.getException() instanceof LockTimeoutException,
                    "Expected LockTimeoutException but got " + thread.getException());

            final long elapsedMillis = (System.nanoTime() - startNanos) / 1_000_000L;
            assertTrue(elapsedMillis >= 900,
                    "Timeout should wait roughly one second, but elapsed=" + elapsedMillis + "ms");
        }
    }

    /**
     * Tests for tryLock() method with negative timeout, which should wait indefinitely until lock is acquired
     */
    @Test
    public void testTryLockNeg() throws InterruptedException
    {
        boolean status;
        ThreadObject thread;
        CloseableLock lock = new CloseableLock();
        try (AutoCloseableLock acl = lock.lock())
        {
            thread = new ThreadObject(lock, MODE.TRY_LOCK_NEG, null); // wait w/o timeout
            thread.start();
            //- Poll without waitForCondition: awaiting would release the lock.
            waitUntilStarted(thread, MS500);
            assertFalse(thread.isFinished()); // thread should be waiting

            //- Wait (max 500ms) until thread is finished; await releases the lock for the worker.
            status = lock.waitForCondition(()->thread.isFinished(), MS500);
        }
        assertTrue(status); // thread-is-finished == true
        assertNull(thread.getException()); // no (timeout-)exception
    }

    /**
     * Tests for tryLock() method with timeout, which should wait until timeout and throw exception
     */
    @Test
    public void testTryLockTimeout()
    {
        CloseableLock lock = new CloseableLock();
        try (AutoCloseableLock acl = lock.lock())
        {
            ThreadObject thread = new ThreadObject(lock, MODE.TRY_LOCK_200MS_TIMEOUT, null); // wait with 200ms timeout
            thread.start();

            //- block lock for 500ms, timeout in thread after 200ms
            CloseableLock lock2 = new CloseableLock();
            lock2.wait(MS500);
            assertTrue(thread.isFinished());
            assertNotNull(thread.getException());
        }
    }

    /**
     * Tests for wait() method, which should be interrupted and throw exception
     */
    @Test
    public void testWaitInterrupted()
    {
        CloseableLock lock = new CloseableLock();
        try (AutoCloseableLock acl = lock.lock())
        {
            ThreadObject thread = new ThreadObject(lock, MODE.WAIT_500MS, null); // wait 500ms
            thread.start();
            //- Wait (max 500ms) until thread is started
            boolean status = lock.waitForCondition(()->thread.isStarted(), MS500);
            assertTrue(status);

            thread.interrupt();

            lock.waitForCondition(() -> thread.isFinished(), MS500);
            assertNotNull(thread.getException());
        }
    }

    /**
     * Tests for constructor with ReentrantLock parameter, which should create a CloseableLock instance
     */
    @Test
    public void testLockConstructor()
    {
        ReentrantLock baseLock = new ReentrantLock();
        new CloseableLock(baseLock);
    }

    @Test
    void testLockConstructorRejectsNull()
    {
        assertThrows(NullPointerException.class, () -> new CloseableLock(null));
    }

    @Test
    void testWaitForConditionRejectsNullSupplier()
    {
        CloseableLock lock = new CloseableLock();
        assertThrows(NullPointerException.class, () -> lock.waitForCondition(null, MS500));
    }

    /**
     * Tests for getCondition() method, which should return a BooleanLockCondition instance with initial state false
     */
    @Test
    public void testCondition()
    {
        CloseableLock lock = new CloseableLock();
        BooleanLockCondition condition = new BooleanLockCondition(lock);
        assertEquals(Boolean.FALSE, condition.getState());
    }

    /**
     * Tests for wait() method with null and zero timeout, which should throw exception immediately
     */
    @Test
    public void testWait()
    {
        CloseableLock lock = new CloseableLock();
        try (AutoCloseableLock acl = lock.lock())
        {
            lock.wait(MS100); // block lock for 100ms
            assertThrows(LockException.class, () -> lock.wait(null));
            assertThrows(LockException.class, () -> lock.wait(Duration.ofNanos(0L)));
        }
    }

    /**
     * Tests for signal() and signalAll() methods, which should not throw any exception
     */
    @Test
    public void testSignal()
    {
        CloseableLock lock = new CloseableLock();
        lock.signal();
        lock.signalAll();
        try (AutoCloseableLock acl = lock.lock())
        {
            lock.signal();
            lock.signalAll();
            new BooleanLockCondition(lock); // create condition
            lock.signal();
            lock.signalAll();
        }
    }

    /**
     * Tests for close() method, which should throw IllegalMonitorStateException when called without holding the lock
     */
    @Test
    public void testClose()
    {
        CloseableLock closeableLock = new CloseableLock();
        assertThrows(IllegalMonitorStateException.class, closeableLock::close);
    }


    /**
     * Test that signalAll() is safe to call before any waiter exists.
     * With lazy initialization, this should not create a Condition.
     */
    @Test
    void testSignalAllWithoutWaiters() throws Exception
    {
        CloseableLock lock = new CloseableLock();
        final Field conditionField = CloseableLock.class.getDeclaredField("condition");
        conditionField.setAccessible(true);

        // Should not throw, should return immediately (no condition created)
        try (AutoCloseableLock autoLock = lock.lock())
        {
            lock.signalAll();
            assertNull(conditionField.get(lock), "signalAll must not create a Condition");
        }
    }

    /**
     * Test that signal() is safe to call before any waiter exists.
     */
    @Test
    void testSignalWithoutWaiters()
    {
        CloseableLock lock = new CloseableLock();

        try (AutoCloseableLock autoLock = lock.lock())
        {
            lock.signal();
        }
    }

    /**
     * Test that getCondition() creates condition lazily.
     */
    @Test
    void testConditionLazyCreation() throws Exception
    {
        CloseableLock lock = new CloseableLock();

        // Access condition field via reflection to verify it's null initially
        final Field conditionField = CloseableLock.class.getDeclaredField("condition");
        conditionField.setAccessible(true);

        assertNull(conditionField.get(lock), "Condition should be null initially");

        // Trigger condition creation via waitForCondition with a condition that is initially false
        AtomicBoolean flag = new AtomicBoolean(false);

        // Start a thread that will set the flag after a short delay
        Thread signaler = new Thread(() -> {
            try
            {
                Thread.sleep(50);
                try (AutoCloseableLock autoLock = lock.lock())
                {
                    flag.set(true);
                    lock.signalAll();
                }
            }
            catch (InterruptedException e)
            {
                Thread.currentThread().interrupt();
            }
        });
        signaler.start();

        // This will call getCondition() because flag is initially false
        lock.waitForCondition(flag::get, MS500);
        signaler.join();

        assertNotNull(conditionField.get(lock), "Condition should be created after waitForCondition");
    }



    private static void waitUntilStarted(final ThreadObject thread, final Duration timeout) throws InterruptedException
    {
        final long deadline = System.nanoTime() + timeout.toNanos();
        while (!thread.isStarted() && System.nanoTime() < deadline)
        {
            Thread.sleep(5);
        }
        assertTrue(thread.isStarted(), "Thread should start within " + timeout);
    }

    /**
     * Helper Object
     */
    public static class ThreadObject extends Thread
    {
        /**
         * Modes for ThreadObject to test different lock acquisition methods and wait behavior
         */
        public static enum MODE
        {
            /** tryLock with null timeout, which should not wait and throw exception immediately */
            TRY_LOCK_NULL,
            /** tryLock with zero timeout, which should not wait and throw exception immediately */
            TRY_LOCK_ZERO,
            /** tryLock with negative timeout, which should wait indefinitely until lock is acquired */
            TRY_LOCK_NEG,
            /** tryLock with 200ms timeout, which should wait until timeout and throw exception */
            TRY_LOCK_200MS_TIMEOUT,
            /** tryLock with 1s timeout, which should wait until timeout and throw exception */
            TRY_LOCK_1S_TIMEOUT,
            /** wait for 500ms, which should be interrupted and throw exception */
            WAIT_500MS;
        };

        private final MODE mode;

        private CloseableLock lock;
        private LockException exception;
        private boolean started;
        private boolean finished;
        private Duration waitAtEnd;

        /**
         * Constructor for ThreadObject
         * @param lock the CloseableLock instance to use
         * @param mode the mode of operation for the thread
         * @param waitAtEnd the duration to wait at the end of the run method (can be null)
         */
        public ThreadObject(CloseableLock lock, MODE mode, Duration waitAtEnd)
        {
            this.lock = lock;
            this.mode = mode;
            this.waitAtEnd = waitAtEnd;
        }

        /**
         * Getter for the exception thrown during lock acquisition
         * @return the LockException instance if an exception was thrown, null otherwise
         */
        public LockException getException()
        {
            return exception;
        }

        /**
         * Getter for the finished state of the thread
         * @return true if the thread has finished execution, false otherwise
         */
        public boolean isFinished()
        {
            return finished;
        }

        /**
         * Getter for the started state of the thread
         * @return true if the thread has started execution, false otherwise
         */
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

                    case TRY_LOCK_200MS_TIMEOUT:
                        try (AutoCloseableLock acl = lock.tryLock(Duration.ofMillis(200))) // wait with 200ms timeout
                        {
                        }
                        break;

                    case TRY_LOCK_1S_TIMEOUT:
                        try (AutoCloseableLock acl = lock.tryLock(Duration.ofSeconds(1))) // wait with 1s timeout
                        {
                        }
                        break;

                    case WAIT_500MS:
                        lock.wait(Duration.ofMillis(500));

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
