package test.com.csitte.autocloseablelock;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.junit.jupiter.api.Test;

import com.csitte.autocloseablelock.AutoCloseableWriteLock;
import com.csitte.autocloseablelock.AutoCloseableWriteLockImpl;
import com.csitte.autocloseablelock.CloseableReadWriteLock;
import com.csitte.autocloseablelock.LockException;

@SuppressWarnings("PMD")
public class AutoCloseableWriteLockImplTest
{

    @Test
    public void testWriteLock()
    {
        CloseableReadWriteLock readWriteLock = new CloseableReadWriteLock(new ReentrantReadWriteLock());
        AutoCloseableWriteLockImplWrapper lock = new AutoCloseableWriteLockImplWrapper(readWriteLock);
        LockException exception;

        // Test write-lock and unlock
        lock.writeLock();
        lock.close();

        // Test LockException
        lock.writeLock();
        exception = assertThrows(LockException.class, lock::writeLock);
        assertEquals("invalid state", exception.getMessage());
        lock.close();

        // Test write-lock and downgrade to read-lock
        lock.writeLock();
        lock.downgradeToReadLock();
        lock.close();

        lock.writeLock();
        lock.downgradeToReadLock();
        exception = assertThrows(LockException.class, lock::writeLock);
        assertEquals("invalid state", exception.getMessage());
        lock.close();

        lock.writeLock();
        lock.downgradeToReadLock();
        exception = assertThrows(LockException.class, lock::downgradeToReadLock);
        assertEquals("invalid state", exception.getMessage());
        lock.close();

        // call extra close (does nothing)
        lock.close();
    }

    @Test
    public void testWriteLockInterruptibly() throws InterruptedException
    {
        CloseableReadWriteLock readWriteLock = new CloseableReadWriteLock(new ReentrantReadWriteLock());

        AutoCloseableWriteLock acwl = readWriteLock.writeLockInterruptibly();
        RunnableObject runnable = new RunnableObject(readWriteLock);
        Thread thread1 = new Thread(runnable);
        thread1.start(); // thread is also trying to acquire the write-lock.
        TimeUnit.MILLISECONDS.sleep(100);
        thread1.interrupt();
        thread1.join();
        assertNotNull(runnable.getException());
        assertTrue(runnable.getException() instanceof LockException);
        acwl.close();

        //-
        LockException exception;
        AutoCloseableWriteLockImplWrapper lock = new AutoCloseableWriteLockImplWrapper(readWriteLock);
        lock.writeLockInterruptibly();
        exception = assertThrows(LockException.class, lock::writeLockInterruptibly);
        assertEquals("invalid state", exception.getMessage());
        lock.downgradeToReadLockInterruptibly();
        exception = assertThrows(LockException.class, lock::writeLockInterruptibly);
        assertEquals("invalid state", exception.getMessage());
        lock.close();

        //-
        exception = assertThrows(LockException.class, lock::downgradeToReadLockInterruptibly);
        assertEquals("invalid state", exception.getMessage());
    }

    @Test
    public void testTryWriteLock() throws InterruptedException
    {
        LockException exception;
        CloseableReadWriteLock readWriteLock = new CloseableReadWriteLock(new ReentrantReadWriteLock());

        //-
        AutoCloseableWriteLockImplWrapper lock = new AutoCloseableWriteLockImplWrapper(readWriteLock);
        lock.tryWriteLock(null);
        exception = assertThrows(LockException.class, lock::writeLockInterruptibly);
        assertEquals("invalid state", exception.getMessage());
        lock.downgradeToReadLockInterruptibly();
        exception = assertThrows(LockException.class, lock::writeLockInterruptibly);
        assertEquals("invalid state", exception.getMessage());
        lock.close();

        //-
        exception = assertThrows(LockException.class, lock::downgradeToReadLockInterruptibly);
        assertEquals("invalid state", exception.getMessage());
    }

    public static class AutoCloseableWriteLockImplWrapper extends AutoCloseableWriteLockImpl
    {
        public AutoCloseableWriteLockImplWrapper(CloseableReadWriteLock readWriteLock)
        {
            super(readWriteLock);
        }
        @Override
        public void writeLock()
        {
            super.writeLock();
        }
        @Override
        public void writeLockInterruptibly()
        {
            super.writeLockInterruptibly();
        }
        @Override
        public void tryWriteLock(Duration timeout)
        {
            super.tryWriteLock(timeout);
        }
    }

    public static class RunnableObject implements Runnable
    {
        private CloseableReadWriteLock lock;
        private Exception exception;

        public RunnableObject(CloseableReadWriteLock lock)
        {
            this.lock = lock;
        }
        @Override
        public void run()
        {
            try
            {
                lock.writeLockInterruptibly();
            }
            catch (RuntimeException x)
            {
                exception = x;
            }
        }
        public Exception getException()
        {
            return exception;
        }
    }

}
