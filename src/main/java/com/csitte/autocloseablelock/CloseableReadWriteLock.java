package com.csitte.autocloseablelock;

import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 *  Handles {@link ReadWriteLock}-type locks
 *
 * @author sit
 */
public class CloseableReadWriteLock
{
    private final CloseableLock closeableReadLock;
    private final CloseableLock closeableWriteLock;

    /**
     *  Constructor.
     *
     *  uses {@ ReentrantReadWriteLock} as default lock implementation
     */
    public CloseableReadWriteLock()
    {
        this(new ReentrantReadWriteLock());
    }

    /**
     *  Constructor.
     */
    public CloseableReadWriteLock(ReadWriteLock readWriteLock)
    {
        this.closeableReadLock = new CloseableLock(readWriteLock.readLock());
        this.closeableWriteLock = new CloseableLock(readWriteLock.writeLock());
    }

    public CloseableLock getReadLock()
    {
        return closeableReadLock;
    }

    public CloseableLock getWriteLock()
    {
        return closeableWriteLock;
    }

    /**
     * @return an {@link AutoCloseableLock} once the read-lock has been acquired.
     */
    public AutoCloseableLock readLock()
    {
        return closeableReadLock.lock();
    }

    /**
     * @return an {@link AutoCloseableLock} once the write-lock has been acquired.
     *
     * @see {@link java.util.concurrent.locks.Lock#lock()}
     */
    public AutoCloseableWriteLock writeLock()
    {
        AutoCloseableWriteLockImpl lock = new AutoCloseableWriteLockImpl(this);
        lock.writeLock();
        return lock;
    }

    /**
     * @return an {@link AutoCloseableLock} once the write-lock has been acquired.
     *
     * @see {@link java.util.concurrent.locks.Lock#lockInterruptibly()}
     */
    public AutoCloseableWriteLock writeLockInterruptibly()
    {
        AutoCloseableWriteLockImpl lock = new AutoCloseableWriteLockImpl(this);
        lock.writeLockInterruptibly();
        return lock;
    }

    public AutoCloseableLock tryReadLock(int timeoutInSeconds)
    {
        return closeableReadLock.tryLock(timeoutInSeconds);
    }

    public AutoCloseableWriteLock tryWriteLock(int timeoutInSeconds)
    {
        AutoCloseableWriteLockImpl lock = new AutoCloseableWriteLockImpl(this);
        lock.tryWriteLock(timeoutInSeconds);
        return lock;
    }
}