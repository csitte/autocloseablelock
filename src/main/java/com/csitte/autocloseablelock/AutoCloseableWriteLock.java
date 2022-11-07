package com.csitte.autocloseablelock;

import java.time.Duration;
import java.util.function.BooleanSupplier;

/**
 *  The thread that holds the write lock can downgrade to a read lock.
 *  It can acquire a read lock before it releases the write lock to downgrade from a write lock to a read lock
 *  while ensuring that no other thread is permitted to acquire the write lock
 *  while it is in the process of downgrading.
*/
public interface AutoCloseableWriteLock extends AutoCloseableLock
{
    /**
     *  Wait for timeout.
     *
     *  @param  timeout     null or 0 means: no timeout
     */
    void wait(Duration timeout);

    /**
     *  Wait for condition to become true or timeout.
     *
     *  @param  fCondition  Represents a supplier of {@code boolean}-valued condition results
     *  @param  timeout     null or 0 means: no timeout
     *
     *  @return true == condition met; false == timeout or interrupt occured
     */
    boolean waitForCondition(BooleanSupplier fCondition, Duration timeout);

    /**
     *  Wakes up thread(s) which are waiting for the condition.
     */
    void signalAll();
    void signal();

    void downgradeToReadLock();

    void downgradeToReadLockInterruptibly();
}