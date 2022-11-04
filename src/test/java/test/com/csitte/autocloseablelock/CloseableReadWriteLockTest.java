package test.com.csitte.autocloseablelock;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.time.Duration;

import org.junit.jupiter.api.Test;

import com.csitte.autocloseablelock.AutoCloseableLock;
import com.csitte.autocloseablelock.AutoCloseableWriteLock;
import com.csitte.autocloseablelock.AutoCloseableWriteLockImpl;
import com.csitte.autocloseablelock.CloseableLock;
import com.csitte.autocloseablelock.CloseableReadWriteLock;
import com.csitte.autocloseablelock.LockCondition.BooleanLockCondition;
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
        CloseableLock readLock = lock.getReadLock();
        assertNotNull(readLock);
        CloseableLock writeLock = lock.getWriteLock();
        assertNotNull(writeLock);

        lock = new CloseableReadWriteLock();
    }

    @Test
    void testReadLock()
    {
        CloseableReadWriteLock lock = new CloseableReadWriteLock();
        try (AutoCloseableLock acl = lock.readLock())
        {
        }
    }

    @Test
    void testTryReadLock()
    {
        CloseableReadWriteLock lock = new CloseableReadWriteLock();
        try (AutoCloseableLock acl = lock.tryReadLock(SEC10))
        {
        }
    }

    @Test
    void testWriteLock()
    {
        CloseableReadWriteLock lock = new CloseableReadWriteLock();
        try (AutoCloseableWriteLock acwl = lock.writeLock())
        {
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
        }
    }

    @Test
    void testTryWriteLock()
    {
        CloseableReadWriteLock lock = new CloseableReadWriteLock();
        try (AutoCloseableWriteLock acwl = lock.tryWriteLock(SEC10))
        {
            acwl.downgradeToReadLock();
        }
    }

    @Test
    void testWait()
    {
        CloseableReadWriteLock lock = new CloseableReadWriteLock();
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
            result = acwl.waitForCondition(()->false, SEC2);
            assertFalse(result);
            acwl.downgradeToReadLock();
            LockException lockException = null;
            try
            {
                result = acwl.waitForCondition(()->false, SEC2);
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
            new BooleanLockCondition(lock.getWriteLock());
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
            new BooleanLockCondition(lock.getWriteLock());
        }

    }

    @Test
    void testClose()
    {
        CloseableReadWriteLock lock = new CloseableReadWriteLock();
        AutoCloseableWriteLock acwl = new AutoCloseableWriteLockImpl(lock);
        acwl.close();
    }
}
