package com.csitte.autocloseablelock;

import java.time.Duration;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.function.BooleanSupplier;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;


/**
 *  The thread that holds the write lock can downgrade to a read lock.
 *  It can acquire a read lock before it releases the write lock to downgrade from a write lock to a read lock
 *  while ensuring that no other thread is permitted to acquire the write lock
 *  while it is in the process of downgrading.
*/
public class AutoCloseableWriteLockImpl implements AutoCloseableWriteLock
{
    private static final Logger LOG = LogManager.getLogger(AutoCloseableWriteLockImpl.class);

    private CloseableReadWriteLock readWriteLock;

    private AutoCloseableLock autoCloseableReadLock;
    private AutoCloseableLock autoCloseableWriteLock;

    private final String name;

    private static final String TXT_INVALID_STATE = "invalid state";


    /**
     *  Constructor.
     *
     *  @param  readWriteLock   use this ReadWriteLock as basis
     */
    public AutoCloseableWriteLockImpl(CloseableReadWriteLock readWriteLock)
    {
        this(readWriteLock, null);
    }

    /**
     *  Constructor.
     *
     *  @param  readWriteLock   use this ReadWriteLock as basis
     *  @param  info    info-object (used for logging)
     */
    public AutoCloseableWriteLockImpl(CloseableReadWriteLock readWriteLock, Object info)
    {
        this.readWriteLock = readWriteLock;
        name = readWriteLock.getName() + '#' + readWriteLock.getNextLockIndex();
        LOG.debug("{} created", name);
    }

    @Override
    public String getName()
    {
        return name;
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
            throw new LockException(autoCloseableWriteLock.getName() + " - invalid timeout value: " + timeout);
        }
        autoCloseableWriteLock.waitForCondition(()->false, "false", timeout);
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
    public boolean waitForCondition(BooleanSupplier fCondition, String text, Duration timeout)
    {
        if (autoCloseableWriteLock == null) // only usable with write-lock
        {
            throw new LockException(TXT_INVALID_STATE);
        }
        return autoCloseableWriteLock.waitForCondition(fCondition, text, timeout);
    }

    @Override
    public boolean waitForCondition(BooleanSupplier fCondition, Duration timeout)
    {
        if (autoCloseableWriteLock == null) // only usable with write-lock
        {
            throw new LockException(TXT_INVALID_STATE);
        }
        return autoCloseableWriteLock.waitForCondition(fCondition, timeout);
    }

    @Override
    public void signalAll()
    {
        if (autoCloseableWriteLock == null) // only usable with write-lock
        {
            throw new LockException(TXT_INVALID_STATE);
        }
        autoCloseableWriteLock.signalAll();
    }

    @Override
    public void signal()
    {
        if (autoCloseableWriteLock == null) // only usable with write-lock
        {
            throw new LockException(TXT_INVALID_STATE);
        }
        autoCloseableWriteLock.signal();
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

    /**
     *  @return Condition instance that is bound to current active write-lock.
     *          read-lock's cannot provide Condition's.
     *          The condition is only created on the first call to this method
     *
     *  @see Lock#newCondition()
     */
    @Override
    public Condition getCondition()
    {
        if (autoCloseableWriteLock == null)
        {
            throw new LockException(TXT_INVALID_STATE);
        }
        return autoCloseableWriteLock.getCondition();
    }
}
