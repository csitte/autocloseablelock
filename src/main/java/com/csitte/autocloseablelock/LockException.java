package com.csitte.autocloseablelock;

public class LockException extends RuntimeException
{
    private static final long serialVersionUID = 1;

    public LockException()
    {
        // empty constructor
    }

    public LockException(String msg)
    {
        super(msg);
    }

    public LockException(Throwable throwable)
    {
        super(throwable);
    }

    public LockException(String message, Throwable throwable)
    {
        super(message, throwable);
    }

}
