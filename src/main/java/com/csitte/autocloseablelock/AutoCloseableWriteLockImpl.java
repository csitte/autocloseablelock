package com.csitte.autocloseablelock;

import java.time.Duration;
import java.util.function.BooleanSupplier;


/**
 *  The thread that holds the write lock can downgrade to a read lock.
 *  It can acquire a read lock before it releases the write lock to downgrade from a write lock to a read lock
 *  while ensuring that no other thread is permitted to acquire the write lock
 *  while it is in the process of downgrading.
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

    void writeLock()
    {
        if (autoCloseableWriteLock != null || autoCloseableReadLock != null)
        {
            throw new LockException(TXT_INVALID_STATE);
        }
        autoCloseableWriteLock = readWriteLock.getWriteLock().lock();
    }

    void writeLockInterruptibly()
    {
        if (autoCloseableWriteLock != null || autoCloseableReadLock != null)
        {
            throw new LockException(TXT_INVALID_STATE);
        }
        autoCloseableWriteLock = readWriteLock.getWriteLock().lockInterruptibly();
    }

    void tryWriteLock(Duration timeout)
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
