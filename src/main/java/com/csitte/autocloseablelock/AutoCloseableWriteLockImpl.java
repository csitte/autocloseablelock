package com.csitte.autocloseablelock;

import java.time.Duration;

/**
 *  The thread that holds the write lock can downgrade to a read lock.
 *  It can acquire a read lock before it releases the write lock to downgrade from a write lock to a read lock
 *  while ensuring that no other thread is permitted to acquire the write lock
 *  while it is in the process of downgrading.
 *
*/
public class AutoCloseableWriteLockImpl implements AutoCloseableWriteLock
{
    private CloseableLock currentLock;
    private CloseableLock closeableReadLock;
    private CloseableLock closeableWriteLock;

    /**
     *  Constructor
     */
    public AutoCloseableWriteLockImpl(CloseableReadWriteLock readWriteLock)
    {
        this.closeableWriteLock = readWriteLock.getWriteLock();
        this.closeableReadLock = readWriteLock.getReadLock();
    }

    void writeLock()
    {
        if (currentLock != null || closeableWriteLock == null)
        {
            throw new LockException("invalid state");
        }
        closeableWriteLock.lock();
        this.currentLock = closeableWriteLock;
    }

    void writeLockInterruptibly()
    {
        if (currentLock != null || closeableWriteLock == null)
        {
            throw new LockException("invalid state");
        }
        closeableWriteLock.lockInterruptibly();
        this.currentLock = closeableWriteLock;
    }

    void tryWriteLock(int timeoutInSeconds)
    {
        if (currentLock != null || closeableWriteLock == null)
        {
            throw new LockException("invalid state");
        }
        closeableWriteLock.tryLock(timeoutInSeconds);
        this.currentLock = closeableWriteLock;
    }

    void tryWriteLock(Duration timeout)
    {
        if (currentLock != null || closeableWriteLock == null)
        {
            throw new LockException("invalid state");
        }
        closeableWriteLock.tryLock(timeout);
        this.currentLock = closeableWriteLock;
    }

    /**
     *  Wait for timeout.
     */
    @Override
    public void wait(Duration timeout)
    {
        if (currentLock == null)
        {
            throw new LockException("no lock available");
        }
        if (timeout == null || timeout.isZero())
        {
            throw new LockException(currentLock.getName() + " - invalid timeout value: " + timeout);
        }
        currentLock.waitForCondition(()->false, timeout);
    }

    @Override
    public void downgradeToReadLock()
    {
        if (closeableWriteLock == null)
        {
            throw new LockException("invalid state");
        }
        closeableReadLock.lock();
        currentLock = closeableReadLock;
        closeableWriteLock.close();
        closeableWriteLock = null;
    }

    @Override
    public void downgradeToReadLockInterruptibly()
    {
        if (closeableWriteLock == null)
        {
            throw new LockException("invalid state");
        }
        closeableReadLock.lockInterruptibly();
        currentLock = closeableReadLock;
        closeableWriteLock.close();
        closeableWriteLock = null;
    }

    @Override
    public void signalAll()
    {
        if (currentLock != null)
        {
            currentLock.signalAll();
        }
    }

    @Override
    public void signal()
    {
        if (currentLock != null)
        {
            currentLock.signal();
        }
    }

    @Override
    public void close()
    {
        if (currentLock != null)
        {
            currentLock.close();
            currentLock = null;
        }
    }
}