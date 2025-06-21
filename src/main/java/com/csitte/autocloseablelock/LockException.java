package com.csitte.autocloseablelock;

/**
 * Runtime exception used for lock related errors.
 */
public class LockException extends RuntimeException
{
    private static final long serialVersionUID = 1;

    /**
     * Creates an instance with no detail message.
     */
    public LockException()
    {
        super();
    }

    /**
     * Creates an instance with an error message.
     *
     * @param msg error message
     */
    public LockException(final String msg)
    {
        super(msg);
    }

    /**
     * Creates an instance wrapping another exception.
     *
     * @param throwable the root cause
     */
    public LockException(final Throwable throwable)
    {
        super(throwable);
    }

    /**
     * Creates an instance with a message and root cause.
     *
     * @param message    error message
     * @param throwable  the root cause
     */
    public LockException(final String message, final Throwable throwable)
    {
        super(message, throwable);
    }
}
