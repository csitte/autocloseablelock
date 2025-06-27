/*
 * Copyright 2022-2025 C.Sitte Softwaretechnik
 * SPDX-License-Identifier: MIT
 */
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
@SuppressWarnings("PMD.CommentSize")
public class AutoCloseableWriteLockImpl implements AutoCloseableWriteLock
{
    /** Read-Write-Lock used */
    private final CloseableReadWriteLock readWriteLock;

    /** AutoCloseableLock for read-lock */
    private AutoCloseableLock autoReadLock = NullAutoCloseableLock.INSTANCE;
    /** AutoCloseableLock for write-lock */
    private AutoCloseableLock autoWriteLock = NullAutoCloseableLock.INSTANCE;

    /** default error text for invalid state errors */
    private static final String TXT_INVALID_STATE = "invalid state";


    /**
     *  Constructor.
     *
     *  @param  readWriteLock   use this ReadWriteLock as basis
     */
    public AutoCloseableWriteLockImpl(final CloseableReadWriteLock readWriteLock)
    {
        this.readWriteLock = readWriteLock;
    }

    /**
     * Obtain an exclusive write lock on the associated readWriteLock instance.
     */
    protected void writeLock()
    {
        if (autoWriteLock != NullAutoCloseableLock.INSTANCE || autoReadLock != NullAutoCloseableLock.INSTANCE)
        {
            throw new LockException(TXT_INVALID_STATE);
        }
        autoWriteLock = readWriteLock.lockWriteLock();
    }

    /**
     *  Lock write-lock (interruptibly)
     */
    protected void writeLockInterruptibly()
    {
        if (autoWriteLock != NullAutoCloseableLock.INSTANCE || autoReadLock != NullAutoCloseableLock.INSTANCE)
        {
            throw new LockException(TXT_INVALID_STATE);
        }
        autoWriteLock = readWriteLock.lockWriteLockInterruptibly();
    }

    /**
     * Try write-lock (with timeout).
     *
     * @param timeout duration to wait for the lock
     */
    protected void tryWriteLock(final Duration timeout)
    {
        if (autoWriteLock != NullAutoCloseableLock.INSTANCE || autoReadLock != NullAutoCloseableLock.INSTANCE)
        {
            throw new LockException(TXT_INVALID_STATE);
        }
        autoWriteLock = readWriteLock.tryLockWriteLock(timeout);
    }

    /**
     *  Wait for timeout.
     */
    @Override
    public void wait(final Duration timeout)
    {
        if (autoWriteLock == NullAutoCloseableLock.INSTANCE)
        {
            throw new LockException(TXT_INVALID_STATE);
        }
        if (timeout == null || timeout.isZero())
        {
            throw new LockException("invalid timeout value: " + timeout);
        }
        readWriteLock.waitForWriteLockCondition(()->false, timeout);
    }

    @Override
    public void downgradeToReadLock()
    {
        if (autoWriteLock == NullAutoCloseableLock.INSTANCE)
        {
            throw new LockException(TXT_INVALID_STATE);
        }
        autoReadLock = readWriteLock.readLock();
        autoWriteLock.close();
        autoWriteLock = NullAutoCloseableLock.INSTANCE;
    }

    @Override
    public void downgradeToReadLockInterruptibly()
    {
        if (autoWriteLock == NullAutoCloseableLock.INSTANCE)
        {
            throw new LockException(TXT_INVALID_STATE);
        }
        autoReadLock = readWriteLock.readLockInterruptibly();
        autoWriteLock.close();
        autoWriteLock = NullAutoCloseableLock.INSTANCE;
    }

    @Override
    public boolean waitForCondition(final BooleanSupplier fCondition, final Duration timeout)
    {
        if (autoWriteLock == NullAutoCloseableLock.INSTANCE) // only usable with write-lock
        {
            throw new LockException(TXT_INVALID_STATE);
        }
        return readWriteLock.waitForWriteLockCondition(fCondition, timeout);
    }

    @Override
    public void signalAll()
    {
        if (autoWriteLock == NullAutoCloseableLock.INSTANCE) // only usable with write-lock
        {
            throw new LockException(TXT_INVALID_STATE);
        }
        readWriteLock.signalAllWriteLock();
    }

    @Override
    public void signal()
    {
        if (autoWriteLock == NullAutoCloseableLock.INSTANCE) // only usable with write-lock
        {
            throw new LockException(TXT_INVALID_STATE);
        }
        readWriteLock.signalWriteLock();
    }

    @Override
    public void close()
    {
        if (autoWriteLock != NullAutoCloseableLock.INSTANCE)
        {
            autoWriteLock.close();
            autoWriteLock = NullAutoCloseableLock.INSTANCE;
        }
        if (autoReadLock != NullAutoCloseableLock.INSTANCE)
        {
            autoReadLock.close();
            autoReadLock = NullAutoCloseableLock.INSTANCE;
        }
    }
}
