package com.csitte.autocloseablelock;

/**
 * The AutoCloseableLock interface provides an automatic lock release mechanism
 * when used in try-with-resources statement.
 *
 * This interface is intended to be used in conjunction with the CloseableLock class.
 *
 * Usage example:
 * try (AutoCloseableLock lock = closeableLock.lock()) {
 *      // protected code
 * }
 *
 */
@SuppressWarnings("PMD.CommentSize")
public interface AutoCloseableLock extends AutoCloseable
{
    /**
     * Overrides the close method of the AutoCloseable interface,
     * providing an automatic lock release when the try-with-resources block is exited.
     *
     * Unlocking doesn't throw any checked exception.
     *
     * @see AutoCloseable#close()
     */
    @Override
    void close();
}