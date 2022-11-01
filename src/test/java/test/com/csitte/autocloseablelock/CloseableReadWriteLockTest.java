package test.com.csitte.autocloseablelock;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.util.concurrent.locks.Condition;

import org.junit.jupiter.api.Test;

import com.csitte.autocloseablelock.AutoCloseableLock;
import com.csitte.autocloseablelock.AutoCloseableWriteLock;
import com.csitte.autocloseablelock.AutoCloseableWriteLockImpl;
import com.csitte.autocloseablelock.CloseableLock;
import com.csitte.autocloseablelock.CloseableReadWriteLock;
import com.csitte.autocloseablelock.LockException;


/**
 * Tests for CloseableReadWriteLock class
 */
class CloseableReadWriteLockTest
{
    private static final Duration SEC10 = Duration.ofSeconds(10L);
    private static final Duration SEC2 = Duration.ofSeconds(2L);


    @Test
    void testConstructor()
    {
        CloseableReadWriteLock lock;
        lock = new CloseableReadWriteLock();
        assertTrue(lock.getName().startsWith("test.com.csitte.autocloseablelock.CloseableReadWriteLockTest."));
        CloseableLock readLock = lock.getReadLock();
        assertNotNull(readLock);
        CloseableLock writeLock = lock.getWriteLock();
        assertNotNull(writeLock);

        lock = new CloseableReadWriteLock(this);
        assertTrue(lock.getName().startsWith("CloseableReadWriteLockTest-lock-"));
    }

    @Test
    void testReadLock()
    {
        CloseableReadWriteLock lock = new CloseableReadWriteLock();
        try (AutoCloseableLock acl = lock.readLock())
        {
            String name = acl.getName();
            assertTrue(name.startsWith("test.com.csitte.autocloseablelock.CloseableReadWriteLockTest.")
                            && name.indexOf(".read-lock-") > 0);
        }
    }

    @Test
    void testTryReadLock()
    {
        CloseableReadWriteLock lock = new CloseableReadWriteLock();
        try (AutoCloseableLock acl = lock.tryReadLock(SEC10))
        {
            String name = acl.getName();
            assertTrue(name.startsWith("test.com.csitte.autocloseablelock.CloseableReadWriteLockTest.")
                            && name.indexOf(".read-lock-") > 0);
        }
    }

    @Test
    void testWriteLock()
    {
        CloseableReadWriteLock lock = new CloseableReadWriteLock();
        try (AutoCloseableWriteLock acwl = lock.writeLock())
        {
            String name = acwl.getName();
            assertTrue(name.startsWith("test.com.csitte.autocloseablelock.CloseableReadWriteLockTest."));
            acwl.downgradeToReadLock();
        }
        try (AutoCloseableWriteLock acwl = lock.writeLockInterruptibly())
        {
            acwl.downgradeToReadLockInterruptibly();
        }
    }

    @Test
    void testWriteLockInterruptibly()
    {
        CloseableReadWriteLock lock = new CloseableReadWriteLock();
        try (AutoCloseableWriteLock acwl = lock.writeLockInterruptibly())
        {
            String name = acwl.getName();
            assertTrue(name.startsWith("test.com.csitte.autocloseablelock.CloseableReadWriteLockTest."));
        }
    }

    @Test
    void testTryWriteLock()
    {
        CloseableReadWriteLock lock = new CloseableReadWriteLock();
        assertTrue(lock.getName().startsWith("test.com.csitte.autocloseablelock.CloseableReadWriteLockTest."));
        try (AutoCloseableWriteLock acwl = lock.tryWriteLock(SEC10))
        {
            String name = acwl.getName();
            assertTrue(name.startsWith("test.com.csitte.autocloseablelock.CloseableReadWriteLockTest."));
            acwl.downgradeToReadLock();
        }
    }

    @Test
    void testWait()
    {
        CloseableReadWriteLock lock = new CloseableReadWriteLock();
        assertTrue(lock.getName().startsWith("test.com.csitte.autocloseablelock.CloseableReadWriteLockTest."));
        try (AutoCloseableWriteLock acwl = lock.writeLock())
        {
            acwl.wait(SEC2); // block lock for 2 seconds
        }
    }

    @Test
    void testWaitForCondition()
    {
        CloseableReadWriteLock lock = new CloseableReadWriteLock();
        try (AutoCloseableWriteLock acwl = lock.writeLock())
        {
            boolean result = acwl.waitForCondition(()->false, SEC2);
            assertFalse(result);
            result = acwl.waitForCondition(()->false, "false", SEC2);
            assertFalse(result);
            acwl.downgradeToReadLock();
            LockException lockException = null;
            try
            {
                result = acwl.waitForCondition(()->false, "false", SEC2);
            }
            catch (LockException x)
            {
                lockException = x; // no condition allowed for read-lock's
            }
            assertNotNull(lockException);
        }
    }

    @Test
    void testSignal()
    {
        CloseableReadWriteLock lock = new CloseableReadWriteLock();
        try (AutoCloseableWriteLock acwl = lock.writeLock())
        {
            acwl.signal();
            acwl.signalAll();
            acwl.getCondition();
            acwl.signal();
            acwl.signalAll();
            acwl.downgradeToReadLock();
            LockException lockException = null;
            try
            {
                acwl.signal();
            }
            catch (LockException x)
            {
                lockException = x; // no condition allowed for read-lock's
            }
            assertNotNull(lockException);
        }
    }

    @Test
    void testCondition()
    {
        CloseableReadWriteLock lock = new CloseableReadWriteLock();
        try (AutoCloseableWriteLock acwl = lock.writeLock())
        {
            Condition condition1 = acwl.getCondition();
            Condition condition2 = acwl.getCondition();
            assertTrue(condition1 != null && condition1 == condition2);
        }

    }

    @Test
    void testClose()
    {
        CloseableReadWriteLock lock = new CloseableReadWriteLock();
        AutoCloseableWriteLock acwl = new AutoCloseableWriteLockImpl(lock);
        assertTrue(acwl.getName().startsWith("test.com.csitte.autocloseablelock.CloseableReadWriteLockTest."));
        acwl.close();
    }
}
