package com.csitte.autocloseablelock;

import java.time.Duration;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.BooleanSupplier;


/**
 *  Handles {@link ReadWriteLock}-type locks
 *
 *  @author sit
 */
@SuppressWarnings({"PMD.CommentSize", "PMD.TooManyMethods"})
public class CloseableReadWriteLock
{
    /** used to lock read-lock */
    private final CloseableLock closeReadLock;
    /** used to lock write-lock */
    private final CloseableLock closeWriteLock;

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
     */
    public CloseableReadWriteLock(final ReadWriteLock readWriteLock)
    {
        this.closeReadLock  = new CloseableLock(readWriteLock.readLock());
        this.closeWriteLock = new CloseableLock(readWriteLock.writeLock());
    }

    /**
     * @return the lock used for reading
     */
    public CloseableLock getReadLock()
    {
        return closeReadLock;
    }

    /**
     * @return the lock used for writing
     */
    public CloseableLock getWriteLock()
    {
        return closeWriteLock;
    }

    /** direct write-lock lock */
    protected AutoCloseableLock lockWriteLock()
    {
        return closeWriteLock.lock();
    }

    /** direct write-lock lock (interruptibly) */
    protected AutoCloseableLock lockWriteLockInterruptibly()
    {
        return closeWriteLock.lockInterruptibly();
    }

    /**
     * @return an {@link AutoCloseableLock} once the read-lock has been acquired.
     */
    public AutoCloseableLock readLock()
    {
        return closeReadLock.lock();
    }

    /**
     * @return an {@link AutoCloseableLock} once the read-lock has been acquired (interruptibly)
     */
    public AutoCloseableLock readLockInterruptibly()
    {
        return closeReadLock.lockInterruptibly();
    }

    /**
     * @return an {@link AutoCloseableLock} once the write-lock has been acquired.
     *
     * @see Lock#lock()
     */
    public AutoCloseableWriteLock writeLock()
    {
        final AutoCloseableWriteLockImpl lock = new AutoCloseableWriteLockImpl(this);
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
        final AutoCloseableWriteLockImpl lock = new AutoCloseableWriteLockImpl(this);
        lock.writeLockInterruptibly();
        return lock;
    }

    /**
     *  @param timeout  0==return immediately or throw LockException if locked
     *
     *  @return an {@link AutoCloseableLock} once the read-lock has been acquired.
     */
    public AutoCloseableLock tryReadLock(final Duration timeout)
    {
        return closeReadLock.tryLock(timeout);
    }

    /** try write-lock (with timeout) */
    protected AutoCloseableLock tryLockWriteLock(final Duration timeout)
    {
        return closeWriteLock.tryLock(timeout);
    }
    /**
     *  @param timeout  0==return immediately or throw LockException if locked
     *
     *  @return an {@link AutoCloseableWriteLock} once the write-lock has been acquired.
     */
    public AutoCloseableWriteLock tryWriteLock(final Duration timeout)
    {
        final AutoCloseableWriteLockImpl lock = new AutoCloseableWriteLockImpl(this);
        lock.tryWriteLock(timeout);
        return lock;
    }

    /** Wait for write-lock condition (with timeout) */
    protected boolean waitForWriteLockCondition(final BooleanSupplier fCondition, final Duration timeout)
    {
        return closeWriteLock.waitForCondition(()->false, timeout);
    }

    /** Signal all write-lock clients */
    protected void signalAllWriteLock()
    {
        closeWriteLock.signalAll();
    }

    /** Signal one of the write-lock clients */
    protected void signalWriteLock()
    {
        closeWriteLock.signal();
    }

}
