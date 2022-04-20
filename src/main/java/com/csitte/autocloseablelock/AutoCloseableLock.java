package com.csitte.autocloseablelock;

import java.time.Duration;

public interface AutoCloseableLock extends AutoCloseable
{
    /**
     * Unlocking doesn't throw any checked exception.
     */
    @Override
    void close();

    void wait(Duration timeout);

    void signalAll();
    void signal();
}