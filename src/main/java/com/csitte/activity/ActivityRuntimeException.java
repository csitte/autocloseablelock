package com.csitte.activity;

/** Runtime Exception for Activity Exceptions */
public class ActivityRuntimeException extends RuntimeException
{
    private static final long serialVersionUID = -8376644119766382385L;

    /** Constructor */
    public ActivityRuntimeException()
    {
        super();
    }

    /** Constructor */
    public ActivityRuntimeException(final Throwable throwable)
    {
        super(throwable);
    }

    /** Constructor */
    public ActivityRuntimeException(final String message, final Throwable throwable)
    {
        super(message, throwable);
    }

    /** Constructor */
    public ActivityRuntimeException(final String message)
    {
        super(message);
    }
}