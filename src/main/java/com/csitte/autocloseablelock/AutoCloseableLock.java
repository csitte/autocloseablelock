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
     */
    void wait(Duration timeout);

    /**
     *  Wait for condition to become true or timeout.
     */
    boolean waitForCondition(BooleanSupplier fCondition, Duration timeout);

    /**
     *  Wakes up threads which are waiting for the condition.
     */
    void signalAll();
    void signal();
}