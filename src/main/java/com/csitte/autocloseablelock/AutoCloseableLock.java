package com.csitte.autocloseablelock;

import java.time.Duration;
import java.util.function.BooleanSupplier;

public interface AutoCloseableLock extends AutoCloseable
{
    /**
     * Unlocking doesn't throw any checked exception.
     */
    @Override
    void close();

    /**
     *  Wait for timeout.
     *
     *  @param  timeout
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
     *  Wakes up threads which are waiting for the condition.
     */
    void signalAll();
    void signal();
}