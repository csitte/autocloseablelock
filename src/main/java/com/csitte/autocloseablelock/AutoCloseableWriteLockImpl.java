package com.csitte.autocloseablelock;

import java.time.Duration;
import java.util.function.BooleanSupplier;


/**
 * The AutoCloseableWriteLockImpl class provides a wrapper for a CloseableReadWriteLock object,
 * allowing the thread that holds the write lock to downgrade to a read lock.
 *
 * This is done by acquiring a read lock before releasing the write lock,
 * ensuring that no other thread can acquire the write lock while the thread is
 * in the process of downgrading.
 *
 * It is important to note that the methods waitForCondition, signalAll, and
 * signal are only usable with write-lock.
 * If they are called with a read-lock, it will throw a LockException of
 * 'invalid state'.
 *
 * Also the methods downgradeToReadLock and downgradeToReadLockInterruptibly
 * should only be called after the thread has acquired a write lock.
 * If they are called before a write lock is acquired, it will throw a
 * LockException of 'invalid state'
 */
public class AutoCloseableWriteLockImpl implements AutoCloseableWriteLock
{
    private CloseableReadWriteLock readWriteLock;

    private AutoCloseableLock autoCloseableReadLock;
    private AutoCloseableLock autoCloseableWriteLock;

    private static final String TXT_INVALID_STATE = "invalid state";


    /**
     *  Constructor.
     *
     *  @param  readWriteLock   use this ReadWriteLock as basis
     */
    public AutoCloseableWriteLockImpl(CloseableReadWriteLock readWriteLock)
    {
        this.readWriteLock = readWriteLock;
    }

    /**
     * Obtain an exclusive write lock on the associated readWriteLock instance.
     */
    protected void writeLock()
    {
        if (autoCloseableWriteLock != null || autoCloseableReadLock != null)
        {
            throw new LockException(TXT_INVALID_STATE);
        }
        autoCloseableWriteLock = readWriteLock.getWriteLock().lock();
    }

    protected void writeLockInterruptibly()
    {
        if (autoCloseableWriteLock != null || autoCloseableReadLock != null)
        {
            throw new LockException(TXT_INVALID_STATE);
        }
        autoCloseableWriteLock = readWriteLock.getWriteLock().lockInterruptibly();
    }

    protected void tryWriteLock(Duration timeout)
    {
        if (autoCloseableWriteLock != null || autoCloseableReadLock != null)
        {
            throw new LockException(TXT_INVALID_STATE);
        }
        autoCloseableWriteLock = readWriteLock.getWriteLock().tryLock(timeout);
    }

    /**
     *  Wait for timeout.
     */
    @Override
    public void wait(Duration timeout)
    {
        if (autoCloseableWriteLock == null)
        {
            throw new LockException(TXT_INVALID_STATE);
        }
        if (timeout == null || timeout.isZero())
        {
            throw new LockException("invalid timeout value: " + timeout);
        }
        readWriteLock.getWriteLock().waitForCondition(()->false, timeout);
    }

    @Override
    public void downgradeToReadLock()
    {
        if (autoCloseableWriteLock == null)
        {
            throw new LockException(TXT_INVALID_STATE);
        }
        autoCloseableReadLock = readWriteLock.getReadLock().lock();
        autoCloseableWriteLock.close();
        autoCloseableWriteLock = null;
    }

    @Override
    public void downgradeToReadLockInterruptibly()
    {
        if (autoCloseableWriteLock == null)
        {
            throw new LockException(TXT_INVALID_STATE);
        }
        autoCloseableReadLock = readWriteLock.getReadLock().lockInterruptibly();
        autoCloseableWriteLock.close();
        autoCloseableWriteLock = null;
    }

    @Override
    public boolean waitForCondition(BooleanSupplier fCondition, Duration timeout)
    {
        if (autoCloseableWriteLock == null) // only usable with write-lock
        {
            throw new LockException(TXT_INVALID_STATE);
        }
        return readWriteLock.getWriteLock().waitForCondition(fCondition, timeout);
    }

    @Override
    public void signalAll()
    {
        if (autoCloseableWriteLock == null) // only usable with write-lock
        {
            throw new LockException(TXT_INVALID_STATE);
        }
        readWriteLock.getWriteLock().signalAll();
    }

    @Override
    public void signal()
    {
        if (autoCloseableWriteLock == null) // only usable with write-lock
        {
            throw new LockException(TXT_INVALID_STATE);
        }
        readWriteLock.getWriteLock().signal();
    }

    @Override
    public void close()
    {
        if (autoCloseableWriteLock != null)
        {
            autoCloseableWriteLock.close();
            autoCloseableWriteLock = null;
        }
        else if (autoCloseableReadLock != null)
        {
            autoCloseableReadLock.close();
            autoCloseableReadLock = null;
        }
    }
}
