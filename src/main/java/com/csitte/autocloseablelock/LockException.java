package com.csitte.autocloseablelock;

/** Lock Runtime Exception */
public class LockException extends RuntimeException
{
    private static final long serialVersionUID = 1;

    /** Constructor */
    public LockException()
    {
        super();
    }

    /** Constructor */
    public LockException(final String msg)
    {
        super(msg);
    }

    /** Constructor */
    public LockException(final Throwable throwable)
    {
        super(throwable);
    }

    /** Constructor */
    public LockException(final String message, final Throwable throwable)
    {
        super(message, throwable);
    }
}
