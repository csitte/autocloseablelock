package com.csitte.activity;

public class ActivityRuntimeException extends RuntimeException
{
    public ActivityRuntimeException()
    {
        super();
    }

    public ActivityRuntimeException(Throwable throwable)
    {
        super(throwable);
    }

    public ActivityRuntimeException(String message, Throwable throwable)
    {
        super(message, throwable);
    }

    public ActivityRuntimeException(String message)
    {
        super(message);
    }
}