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
 * This class provides an wrapper for a java.util.concurrent.locks.Lock
 * that can be used with the Java try-with-resources functionality.
 * It does not implement the Lock interface in order to ensure
 * that it can only be used through lock-with-resources mechanism.
*/
public class CloseableLock implements AutoCloseableLock
{
    private final Lock myLock;
    private Condition condition;
    private final String name;

    /**
     *  Default Constructor.
     *
     *  Uses {@link ReentrantLock}
     */
    public CloseableLock()
    {
        this(new ReentrantLock(), CloseableLock.class);
    }

    /**
     *  Default Constructor
     *
     *  Uses {@link ReentrantLock}
     */
    public CloseableLock(Object info)
    {
        this(new ReentrantLock(), info);
    }

    /**
     *  Constructor
     *
     *  @params lock    the lock object to use
     *  @param  info    info-text about caller (used for logging)
     */
    public CloseableLock(Lock lock, Object info)
    {
        this.myLock = lock;
        if (info instanceof String)
        {
            name = info.toString();
        }
        else if (info instanceof Class<?>)
        {
            name = ((Class<?>)info).getName();
        }
        else if (info != null)
        {
            name = info.getClass().getName();
        }
        else
        {
            name = "";
        }
    }

    /**
     *  @return info-text of caller-object
     */
    public String getName()
    {
        return name;
    }

    /**
     *  @return the underlying lock in use
     */
    protected Lock getLock()
    {
        return myLock;
    }

    /**
     *  @returns Condition instance that is bound to this Lock
     *           (create new condition on first call)
     *
     *  @see {@link Lock#newCondition()}
     */
    public Condition getCondition()
    {
        if (condition == null)
        {
            try (AutoCloseableLock conditionLock = lock())
            {
                if (condition == null)
                {
                    condition = myLock.newCondition();
                }
            }
        }
        return condition;
    }

    /**
     *  Wakes up all waiting threads which are waiting for the condition.
     *
     *   @see {@link java.util.concurrent.locks.Condition#signalAll()}
     */
    @Override
    public void signalAll()
    {
        try (AutoCloseableLock conditionLock = lock())
        {
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
     *   @see {@link java.util.concurrent.locks.Condition#signal()}
     */
    @Override
    public void signal()
    {
        try (AutoCloseableLock conditionLock = lock())
        {
            if (condition != null)
            {
                //- only if condition is in use
                condition.signal();
            }
        }
    }

    /**
     * @return an {@link AutoCloseable} once the lock has been acquired
     *          which automatically unlocks the lock if it goes out of scope.
     *
     * @see {@link java.util.concurrent.locks.Lock#lock()}
     */
    public AutoCloseableLock lock()
    {
        myLock.lock();
        return this;
    }

    /**
     * @return an {@link AutoCloseable} once the lock has been acquired.
     *
     * @see {@link java.util.concurrent.locks.Lock#lockInterruptibly()}
     */
    public AutoCloseableLock lockInterruptibly()
    {
        try
        {
            myLock.lockInterruptibly();
        }
        catch (InterruptedException x)
        {
            Thread.currentThread().interrupt();
            throw new LockException(name, x);
        }
        return this;
    }

    /**
     *  Wake up all waiting threads and unlock this lock
     *
     * @see {@link java.util.concurrent.locks.Condition#signalAll()}
     */
    @Override
    public void close()
    {
        if (condition != null)
        {
            condition.signalAll();
        }
        myLock.unlock();
    }

    /**
     *  @param timeoutInSeconds 0==return immediately or throw LockException if locked
     */
    public AutoCloseableLock tryLock(int timeoutInSeconds)
    {
        return tryLock(Duration.ofSeconds(timeoutInSeconds));
    }

    /**
     *  @param timeout  null==return immediately or throw LockException if locked
     *
     *  @see {@link java.util.concurrent.locks.Lock#tryLock()}
     *  @see {@link java.util.concurrent.locks.Lock#lock()}
     *  @see {@link java.util.concurrent.locks.Lock#tryLock(long, TimeUnit)}
     */
    public AutoCloseableLock tryLock(Duration timeout)
    {
        try
        {
            if (timeout == null || timeout.isZero())
            {
                if (!myLock.tryLock()) // is locked?
                {
                    throw new LockException(name + " locked"); // no wait
                }
            }
            else if (timeout.isNegative())
            {
                myLock.lock();  // wait without timeout
            }
            else
            {
                //- Timeout-loop
                Instant startOfWait = Instant.now();
                Duration remainingWaitTime = timeout;
                while (!myLock.tryLock(remainingWaitTime.get(ChronoUnit.NANOS), TimeUnit.NANOSECONDS))
                {
                    Duration elapsedTime = Duration.between(startOfWait, Instant.now());
                    remainingWaitTime = timeout.minus(elapsedTime);
                    if (remainingWaitTime.isZero() || remainingWaitTime.isNegative())
                    {
                        throw new LockTimeoutException(name, elapsedTime);
                    }
                }
            }
        }
        catch (InterruptedException x)
        {
            Thread.currentThread().interrupt();
            throw new LockException(name, x);
        }
        return this;
    }

    /**
     *  Wait for timeout.
     *
     *  @see {@link java.util.concurrent.locks.Condition#await()}
     *  @see {@link java.util.concurrent.locks.Condition#awaitNanos(long)}
     */
    @Override
    public void wait(Duration timeout)
    {
        if (timeout == null || timeout.isZero())
        {
            throw new LockException(name + " - invalid timeout value: " + timeout);
        }
        waitForCondition(()->false, timeout);
    }

    /**
     *  Wait for condition to become true or timeout.
     *
     *  @param  timeOutInSeconds    0 means: no timeout
     *
     *  @see {@link java.util.concurrent.locks.Condition#await()}
     *  @see {@link java.util.concurrent.locks.Condition#awaitNanos(long)}
     *
     *  @return true == condition met; false == timeout or interrrupt
     */
    public boolean waitForCondition(BooleanSupplier fCondition, int timeOutInSeconds)
    {
        return waitForCondition(fCondition, Duration.ofSeconds(timeOutInSeconds));
    }

    /**
     *  Wait for condition to become true or timeout.
     *
     *  Returns immediately if condition is met.
     *
     *  @see {@link java.util.concurrent.locks.Condition#await()}
     *  @see {@link java.util.concurrent.locks.Condition#awaitNanos(long)}
     *
     *  @param  fCondition  Represents a supplier of {@code boolean}-valued condition results
     *  @param  timeout     null or 0 means: no timeout
     *
     *  @return state of condition
     */
    public boolean waitForCondition(BooleanSupplier fCondition, Duration timeout)
    {
        try (AutoCloseableLock conditionLock = lock())
        {
            long nanos = timeout==null? 0L: timeout.toNanos();
            while (!fCondition.getAsBoolean())
            {
                if (nanos <= 0)
                {
                    getCondition().await(); // no timeout
                }
                else
                {
                    nanos = getCondition().awaitNanos(nanos);
                    if (nanos <= 0)
                    {
                        //- Final test of condition on timeout
                        return fCondition.getAsBoolean();
                    }
                }
            }
            return true;
        }
        catch (InterruptedException x)
        {
            Thread.currentThread().interrupt();
            throw new LockException(name + " - wait-for-condition interrupted", x);
        }
    }
}