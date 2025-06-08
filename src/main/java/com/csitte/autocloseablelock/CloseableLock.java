package com.csitte.autocloseablelock;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.BooleanSupplier;

/**
 * The CloseableLock class provides a wrapper for a java.util.concurrent.locks.Lock object
 * that can be used with the Java try-with-resources functionality.
 *
 * This class allows for lock acquisition through the standard lock(), lockInterruptibly()
 * and tryLock(Duration timeout) methods.
 * The lock is automatically released when the try-with-resources block is exited.
 *
 * This class does not implement the Lock interface in order to ensure
 * that it can only be used through the try-with-resources mechanism.
 *
 * It properly handles InterruptedExceptions and throws a custom LockException when appropriate.
 *
 * Usage example:
 * try (AutoCloseableLock lock = closeableLock.lock()) {
 *      // protected code
 * }
*/
@SuppressWarnings({"PMD.CommentSize", "PMD.DoNotUseThreads", "PMD.TooManyMethods"})
public class CloseableLock
{
    /**
     * The lock to use.
     */
    private final Lock myLock;

    /**
     *  Optional {@link Condition}-object.
     *  Will be created only on demand.
     */
    private Condition condition;

    /** One Second Constant */
    private static final long SECOND_IN_NANOS = 1_000_000_000L;


    /**
     *  Default Constructor.
     *
     *  Uses {@link ReentrantLock}
     */
    public CloseableLock()
    {
        this(new ReentrantLock());
    }

    /**
     *  Constructor.
     *
     *  @param  lock    the lock object to use
     */
    public CloseableLock(final Lock lock)
    {
        this.myLock = lock;
    }

    /**
     * Acquires the lock.
     *
     * If the lock is not available then the current thread becomes disabled
     * for thread scheduling purposes and lies dormant until the lock has been acquired.
     *
     * @return an {@link AutoCloseableLock} once the lock has been acquired
     *         which automatically unlocks the lock if used in an try-with-resources situation.
     *
     * @see Lock#lock()
     * @see #close()
     */
    public AutoCloseableLock lock()
    {
        myLock.lock();
        return this::close;
    }

    /**
     * Acquires the lock unless the current thread is {@linkplain Thread#interrupt interrupted}.
     *
     * @return an {@link AutoCloseableLock} once the lock has been acquired.
     *
     * @see Lock#lockInterruptibly()
     */
    public AutoCloseableLock lockInterruptibly()
    {
        try
        {
            myLock.lockInterruptibly();
            return this::close;
        }
        catch (InterruptedException x)
        {
            Thread.currentThread().interrupt();
            throw new LockException(x);
        }
    }

    /**
     *  Acquires the lock if it is free within the given waiting time and the
     *  current thread has not been {@linkplain Thread#interrupt interrupted}.
     *
     *  It uses a timeout loop that re-evaluates the remaining wait time
     *  and it uses ChronoUnit.NANOS to get the remaining wait time in nanoseconds.
     *
     *  @param timeout  null or 0 means: Return immediately or throw LockException if locked.
     *                  A negative timeout value means to wait without timeout.
     *
     *  @see Lock#tryLock()
     *  @see Lock#lock()
     *  @see Lock#tryLock(long, TimeUnit)
     *
     *  @return an {@link AutoCloseableLock} once the lock has been acquired.
     *
     *  @throws LockTimeoutException on timeout.
     */
    public AutoCloseableLock tryLock(final Duration timeout)
    {
        if (timeout == null || timeout.isZero())
        {
            if (!myLock.tryLock()) // is locked?
            {
                throw new LockException("not acquired"); // no wait
            }
        }
        else if (timeout.isNegative())
        {
            myLock.lock();  // wait without timeout
        }
        else
        {
            tryLockWithTimeout(timeout);
        }
        return this::close;
    }

    /** tryLock with timeout */
    private void tryLockWithTimeout(final Duration timeout)
    {
        try
        {
            final Instant startOfWait = Instant.now();
            Duration remainingWaitTime = timeout;
            while (!myLock.tryLock(remainingWaitTime.get(ChronoUnit.NANOS), TimeUnit.NANOSECONDS))
            {
                final Duration elapsedTime = Duration.between(startOfWait, Instant.now());
                remainingWaitTime = timeout.minus(elapsedTime);
                if (remainingWaitTime.isZero() || remainingWaitTime.isNegative())
                {
                    throw new LockTimeoutException(elapsedTime);
                }
            }
          }
          catch (InterruptedException x)
          {
              Thread.currentThread().interrupt();
              throw new LockException(x);
          }
    }

    /**
     *  Wait for timeout.
     *
     *  @param timeout  value must be greater than zero
     *
     *  @see Condition#await()
     *  @see Condition#awaitNanos(long)
     *
     *  @throws LockException on invalid timeout value
     */
    public void wait(final Duration timeout)
    {
        if (timeout == null || timeout.isZero())
        {
            throw new LockException("invalid timeout value: " + timeout);
        }
        waitForCondition(()->false, timeout);
    }

    /**
     *  @return Condition instance that is bound to this Lock.
     *          The condition is created on the first call to this method
     *
     *  @see Lock#newCondition()
     */
    protected Condition getOrCreateCondition()
    {
        if (condition == null)
        {
            condition = myLock.newCondition();
        }
        return condition;
    }

    /**
     *  Wakes up all threads which are waiting for the condition.
     *
     *  @see Condition#signalAll()
     */
    public void signalAll()
    {
        try (AutoCloseableLock autoCloseableLock = lock())
        {
            assert autoCloseableLock != null; // ignored on runtime
            if (condition != null)
            {
                //- only if condition is in use
                condition.signalAll();
            }
        }
    }

    /**
     *   Wakes up one waiting thread.
     *
     *   If any threads are waiting on this condition then one is selected for waking up.
     *   That thread must then re-acquire the lock before returning from await.
     *
     *   @see Condition#signal()
     */
    public void signal()
    {
        try (AutoCloseableLock autoCloseableLock = lock())
        {
            assert autoCloseableLock != null; // ignored on runtime
            if (condition != null)
            {
                //- only if condition is in use
                condition.signal();
            }
        }
    }

    /**
     *  Release the lock.
     */
    public void close()
    {
        myLock.unlock();
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
     *  @return true == condition met; false == timeout or interrupt occurred
     */
    public boolean waitForCondition(final BooleanSupplier fCondition, final Duration timeout)
    {
        boolean result = true;
        if (!fCondition.getAsBoolean()) // test condition
        {
            try (AutoCloseableLock autoCloseableLock = lock())
            {
                assert autoCloseableLock != null; // ignored on runtime
                //- calculate end of wait if valid timeout parameter is available
                final Instant startOfWait = Instant.now();
                Instant endOfWait = null;
                if (timeout != null && !timeout.isZero() && !timeout.isNegative())
                {
                    endOfWait = startOfWait.plus(timeout);
                }
                try
                {
                    result = waitForCondition(fCondition, endOfWait);
                }
                catch (InterruptedException x)
                {
                    Thread.currentThread().interrupt();
                    throw new LockException("interrupted", x);
                }
            }
        }
        return result;
    }

    private boolean waitForCondition(final BooleanSupplier fCondition, final Instant endOfWait) throws InterruptedException
    {
        boolean result = true;
        do
        {
            long nanos = SECOND_IN_NANOS; // default wait-interval
            if (endOfWait != null)
            {
                final Instant now = Instant.now();
                final long remainingWaitTime = Duration.between(now, endOfWait).toNanos();
                if (remainingWaitTime <= 0) // timeout
                {
                    result = false;
                    break; // return;
                }
                if (remainingWaitTime < SECOND_IN_NANOS) // wait less than default interval?
                {
                    nanos = remainingWaitTime;
                }
            }
            getOrCreateCondition().awaitNanos(nanos);
        }
        while (!fCondition.getAsBoolean()); // test condition
        return result;
    }


    /**
     *  Wait for condition to become true.
     *
     *  @param  fCondition  Represents a supplier of {@code boolean}-valued condition results
     *
     *  @throws LockException if interrupted
     */
    public void waitForCondition(final BooleanSupplier fCondition)
    {
        waitForCondition(fCondition, (Duration)null);
    }
}
