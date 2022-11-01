package com.csitte.autocloseablelock;

import java.time.Duration;
import java.util.concurrent.locks.Condition;
import java.util.function.BooleanSupplier;

public interface AutoCloseableLock extends AutoCloseable
{
    /**
     * Unlocking doesn't throw any checked exception.
     */
    @Override
    void close();

    /**
     * Return condition instance that is bound to this Lock.
     */
    Condition getCondition();

    /**
     *  Wait for timeout.
     */
    void wait(Duration timeout);

    /**
     *  Wait for condition to become true or timeout.
     */
    boolean waitForCondition(BooleanSupplier fCondition, Duration timeout);
    boolean waitForCondition(BooleanSupplier fCondition, String text, Duration timeout);

    /**
     *  Wakes up threads which are waiting for the condition.
     */
    void signalAll();
    void signal();

    /**
     *	Return name of lock
     */
    String getName();
}