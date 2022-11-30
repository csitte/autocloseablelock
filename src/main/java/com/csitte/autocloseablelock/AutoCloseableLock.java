package com.csitte.autocloseablelock;

public interface AutoCloseableLock extends AutoCloseable
{
    /**
     * Unlocking doesn't throw any checked exception.
     */
    @Override
    void close();
}