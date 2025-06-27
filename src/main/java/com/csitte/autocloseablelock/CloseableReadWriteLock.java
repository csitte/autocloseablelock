/*
 * Copyright 2022-2025 C.Sitte Softwaretechnik
 * SPDX-License-Identifier: MIT
 */
package com.csitte.autocloseablelock;

import java.time.Duration;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.BooleanSupplier;


/**
 * A {@link ReadWriteLock} wrapper that provides {@link CloseableLock} handles
 * for both read and write locks. This simplifies the use of try-with-resources for
 * lock management.
 *
 * <p>Example usage:</p>
 * <pre>{@code
 *   CloseableReadWriteLock lock = new CloseableReadWriteLock();
 *   try (CloseableLock readLock = lock.getReadLock()) {
 *       // read operations
 *   }
 *   try (CloseableLock writeLock = lock.getWriteLock()) {
 *       // write operations
 *   }
 * }</pre>
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
     * Returns the read lock as a {@link CloseableLock}.
     * <p>
     * Use this to guard access to shared data during read operations.
     * The returned lock can be used with try-with-resources for automatic release.
     * </p>
     *
     * @return the read lock wrapper
     */
    public CloseableLock getReadLock()
    {
        return closeReadLock;
    }

    /**
     * Returns the write lock as a {@link CloseableLock}.
     * <p>
     * Use this to guard access to shared data during write operations.
     * The returned lock can be used with try-with-resources for automatic release.
     * </p>
     *
     * @return the write lock wrapper
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
     * Acquires the read lock and returns it wrapped in an {@link AutoCloseableLock}.
     *
     * @return an {@code AutoCloseableLock} for managing the read lock
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
        return closeWriteLock.waitForCondition(fCondition, timeout);
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
