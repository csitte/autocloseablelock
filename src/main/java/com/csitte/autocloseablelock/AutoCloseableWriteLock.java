package com.csitte.autocloseablelock;

import java.time.Duration;
import java.util.function.BooleanSupplier;

/**
 *  The thread that holds the write lock can downgrade to a read lock.
 *  It can acquire a read lock before it releases the write lock to downgrade from a write lock to a read lock
 *  while ensuring that no other thread is permitted to acquire the write lock
 *  while it is in the process of downgrading.
*/
@SuppressWarnings("PMD.CommentSize")
public interface AutoCloseableWriteLock extends AutoCloseableLock
{
    /**
     *  Wait for timeout.
     *
     *  @param  timeout     null or 0 means: no timeout
     */
    void wait(Duration timeout);

    /**
     *  Allows a thread to wait for a specified condition to be met, with a specified timeout.
     *
     *  @param  fCondition  Represents a supplier of {@code boolean}-valued condition results.
     *                      It is important to ensure that the BooleanSupplier is efficient to avoid performance issues.
     *  @param  timeout     null or 0 means: no timeout
     *
     *  @return true == condition met; false == timeout or interrupt occurred
     */
    boolean waitForCondition(BooleanSupplier fCondition, Duration timeout);

    /**
     *  Allows a thread to wake up all waiting threads waiting on the lock.
     */
    void signalAll();

    /**
     *  Allows a thread to signal another thread waiting on the lock.
     *  If any threads are waiting on this condition then one is selected for waking up.
     */
    void signal();

    /**
     *  Downgrade write-lock to read-lock
     */
    void downgradeToReadLock();

    /**
     *  Downgrade write-lock to read-lock (interruptibly)
     */
    void downgradeToReadLockInterruptibly();
}