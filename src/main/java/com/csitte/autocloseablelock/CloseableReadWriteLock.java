package com.csitte.autocloseablelock;

import java.time.Duration;
import java.util.concurrent.locks.Lock;
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
     *  Default Constructor.
     *
     *  Uses {@link ReentrantReadWriteLock} as default lock implementation
     */
    public CloseableReadWriteLock()
    {
        this(new ReentrantReadWriteLock());
    }

    /**
     *  Constructor.
     *
     *  @param  readWriteLock   use this {@link ReadWriteLock} as underlying lock
     *  @param  info    description object about caller (used for logging)
     */
    public CloseableReadWriteLock(ReadWriteLock readWriteLock)
    {
        this.closeableReadLock  = new CloseableLock(readWriteLock.readLock());
        this.closeableWriteLock = new CloseableLock(readWriteLock.writeLock());
    }

    /**
     * @return the lock used for reading
     */
    public CloseableLock getReadLock()
    {
        return closeableReadLock;
    }

    /**
     * @return the lock used for writing
     */
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
     * @see Lock#lock()
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
     * @see Lock#lockInterruptibly()
     */
    public AutoCloseableWriteLock writeLockInterruptibly()
    {
        AutoCloseableWriteLockImpl lock = new AutoCloseableWriteLockImpl(this);
        lock.writeLockInterruptibly();
        return lock;
    }

    /**
     *  @param timeout  0==return immediately or throw LockException if locked
     *
     *  @return an {@link AutoCloseableLock} once the read-lock has been acquired.
     */
    public AutoCloseableLock tryReadLock(Duration timeout)
    {
        return closeableReadLock.tryLock(timeout);
    }

    /**
     *  @param timeout  0==return immediately or throw LockException if locked
     *
     *  @return an {@link AutoCloseableWriteLock} once the write-lock has been acquired.
     */
    public AutoCloseableWriteLock tryWriteLock(Duration timeout)
    {
        AutoCloseableWriteLockImpl lock = new AutoCloseableWriteLockImpl(this);
        lock.tryWriteLock(timeout);
        return lock;
    }
}
