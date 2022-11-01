package com.csitte.autocloseablelock;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.function.BooleanSupplier;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.ThreadContext;


public class AutoCloseableLockImpl implements AutoCloseableLock
{
    private static final Logger LOG = LogManager.getLogger(AutoCloseableLockImpl.class);

    private final CloseableLock lock;
    private static final long ONE_SECOND_IN_NANOS = 1000000000L;

    private final String name;

    /**
     *  Constructor.
     *
     *  @param  lock    the (already locked) lock to close on end of context
     */
    public AutoCloseableLockImpl(CloseableLock lock)
    {
        this.lock = lock;

        name = lock.getName() + '#' + lock.getNextLockIndex();
        LOG.debug("{} aquired", name);
        ThreadContext.push(name);
    }

    @Override
    public String getName()
    {
        return name;
    }

    @Override
    public void close()
    {
        lock.close();
        ThreadContext.pop();
        LOG.debug("{} released", name);
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
            throw new LockException(getName() + " - invalid timeout value: " + timeout);
        }
        waitForCondition(()->false, "timeout", timeout);
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
        return waitForCondition(fCondition, "", timeout);
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
     *  @param  text        describes the condition
     *  @param  timeout     null or 0 means: no timeout
     *
     *  @return true == condition met; false == timeout or interrupt occured
     */
    @Override
    public boolean waitForCondition(BooleanSupplier fCondition, String text, Duration timeout)
    {
        LOG.debug("{}.waitForCondition(\"{}\",{})...", getName(), text, timeout);
        Instant startOfWait = Instant.now();
        Instant endOfWait = null;
        if (timeout != null && !timeout.isZero() && !timeout.isNegative())
        {
            endOfWait = startOfWait.plus(timeout);
        }
        try
        {
            if (fCondition.getAsBoolean()) // condition already true?
            {
                Duration elapsedTime = Duration.between(startOfWait, Instant.now());
                LOG.debug("{} waitForCondition(\"{}\") = true after {}", getName(), text, elapsedTime);
                return true;
            }
            boolean result = true;
            long nanos = ONE_SECOND_IN_NANOS;
            do
            {
                if (endOfWait != null)
                {
                    long remainingWaitTime = Duration.between(Instant.now(), endOfWait).toNanos();
                    if (remainingWaitTime <= 0)
                    {
                        result = false; // timeout
                        break;
                    }
                    if (remainingWaitTime < nanos) // wait max 1 sec
                    {
                        nanos = remainingWaitTime;
                    }
                }
                LOG.debug("{}.waitForCondition(\"{}\",{})...", getName(), text, Duration.ofNanos(nanos));
                nanos = getCondition().awaitNanos(nanos);
            }
            while (!fCondition.getAsBoolean());

            Duration elapsedTime = Duration.between(startOfWait, Instant.now());
            LOG.debug("{} waitForCondition(\"{}\") = {} after {}", getName(), text, result, elapsedTime);
            return result;
        }
        catch (InterruptedException x)
        {
            Thread.currentThread().interrupt();
            throw new LockException(getName() + " waitForCondition(\"" + text + "\") interrupted", x);
        }
    }

    /**
     *  @return Condition instance that is bound to this Lock.
     *          The condition is only created on the first call to this method
     *
     *  @see Lock#newCondition()
     */
    @Override
    public Condition getCondition()
    {
        return lock.getOrCreateCondition();
    }
}
