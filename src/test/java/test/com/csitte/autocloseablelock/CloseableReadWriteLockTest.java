package test.com.csitte.autocloseablelock;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicBoolean;

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
@SuppressWarnings("PMD")
public class CloseableReadWriteLockTest
{
    private static final Duration SEC10 = Duration.ofSeconds(10L);
    private static final Duration SEC2 = Duration.ofSeconds(2L);


    @Test
    public void testConstructor()
    {
        CloseableReadWriteLock lock;
        lock = new CloseableReadWriteLock();
        CloseableLock readLock = lock.getReadLock();
        assertNotNull(readLock);
        CloseableLock writeLock = lock.getWriteLock();
        assertNotNull(writeLock);

        new CloseableReadWriteLock();
    }

    @Test
    public void testReadLock()
    {
        CloseableReadWriteLock lock = new CloseableReadWriteLock();
        try (AutoCloseableLock acl = lock.readLock())
        {
        }
    }

    @Test
    public void testTryReadLock()
    {
        CloseableReadWriteLock lock = new CloseableReadWriteLock();
        try (AutoCloseableLock acl = lock.tryReadLock(SEC10))
        {
        }
    }

    @Test
    public void testWriteLock()
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
    public void testWriteLockInterruptibly()
    {
        CloseableReadWriteLock lock = new CloseableReadWriteLock();
        try (AutoCloseableWriteLock acwl = lock.writeLockInterruptibly())
        {
        }
    }

    @Test
    public void testTryWriteLock()
    {
        CloseableReadWriteLock lock = new CloseableReadWriteLock();
        try (AutoCloseableWriteLock acwl = lock.tryWriteLock(SEC10))
        {
            acwl.downgradeToReadLock();
        }
    }

    @Test
    public void testWait()
    {
        CloseableReadWriteLock lock = new CloseableReadWriteLock();
        try (AutoCloseableWriteLock acwl = lock.writeLock())
        {
            acwl.wait(SEC2); // block lock for 2 seconds
        }
    }

    @Test
    public void testWaitForCondition()
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
    public void testSignal()
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
    public void testWaitForConditionSignal()
    {
        CloseableReadWriteLock lock = new CloseableReadWriteLock();
        AtomicBoolean state = new AtomicBoolean(false);

        Thread thread = new Thread(() -> {
            try (AutoCloseableWriteLock acwl2 = lock.writeLock())
            {
                state.set(true);
                acwl2.signal();
            }
        });

        try (AutoCloseableWriteLock acwl = lock.writeLock())
        {
            thread.start();
            boolean result = acwl.waitForCondition(state::get, SEC10);
            assertTrue(result);
        }
        try
        {
            thread.join();
        }
        catch (InterruptedException e)
        {
            Thread.currentThread().interrupt();
        }
    }

    @Test
    public void testCondition()
    {
        CloseableReadWriteLock lock = new CloseableReadWriteLock();
        try (AutoCloseableWriteLock acwl = lock.writeLock())
        {
            new BooleanLockCondition(lock.getWriteLock());
        }

    }

    @Test
    public void testClose()
    {
        CloseableReadWriteLock lock = new CloseableReadWriteLock();
        AutoCloseableWriteLock acwl = new AutoCloseableWriteLockImpl(lock);
        acwl.close();
    }
}
