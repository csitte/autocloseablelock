package com.csitte.autocloseablelock;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.locks.Condition;
import java.util.function.BooleanSupplier;


class AutoCloseableLockImpl implements AutoCloseableLock
{
    private final CloseableLock lock;
    private static final long ONE_SECOND_IN_NANOS = 1000000000L;


    /**
     *  Constructor.
     *
     *  @param  lock    the (already locked) lock to close on end of context
     */
    public AutoCloseableLockImpl(CloseableLock lock)
    {
        this.lock = lock;
    }

    @Override
    public void close()
    {
        lock.close();
    }

    /**
     *   Wakes up one waiting thread.
     *
     *   @see Condition#signal()
     */
    @Override
    public void signal()
    {
        lock.signalCondition();
    }

    /**
     *  Wakes up all threads which are waiting for the condition.
     *
     *  @see Condition#signalAll()
     */
    @Override
    public void signalAll()
    {
        lock.signalAllCondition();
    }

    /**
     *  Wait for timeout.
     */
    @Override
    public void wait(Duration timeout)
    {
        if (timeout == null || timeout.isZero())
        {
            throw new LockException("invalid timeout value: " + timeout);
        }
        waitForCondition(()->false, timeout);
    }

    /**
     *  Wait for condition to become true or timeout.
     *
     *  Returns immediately if condition is met.
     *
     *  @see Condition#await()
     *  @see Condition#awaitNanos(long)
     *
     *  @param  fCondition  Represents a supplier of {@code boolean}-valued condition results
     *  @param  timeout     null or 0 means: no timeout
     *
     *  @return true == condition met; false == timeout or interrupt occured
     */
    @Override
    public boolean waitForCondition(BooleanSupplier fCondition, Duration timeout)
    {
        if (fCondition.getAsBoolean()) // test condition
        {
            return true;
        }
        //- calculate end of wait if valid timeout parameter is available
        Instant startOfWait = Instant.now();
        Instant endOfWait = null;
        if (timeout != null && !timeout.isZero() && !timeout.isNegative())
        {
            endOfWait = startOfWait.plus(timeout);
        }
        try
        {
            do
            {
            	long nanos = ONE_SECOND_IN_NANOS; // default wait-interval
                if (endOfWait != null)
                {
                	Instant now = Instant.now();
                    long remainingWaitTime = Duration.between(now, endOfWait).toNanos();
                    if (remainingWaitTime <= 0)
                    {
                        return false; // timeout
                    }
                    if (remainingWaitTime < ONE_SECOND_IN_NANOS) // wait less than default interval?
                    {
                        nanos = remainingWaitTime;
                    }
                }
                lock.getOrCreateCondition().awaitNanos(nanos);
            }
            while (!fCondition.getAsBoolean()); // test condition
            return true;
        }
        catch (InterruptedException x)
        {
            Thread.currentThread().interrupt();
            throw new LockException("interrupted", x);
        }
    }
}
