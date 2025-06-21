package com.csitte.activity;

/** Runtime Exception for Activity Exceptions */
public class ActivityRuntimeException extends RuntimeException
{
    private static final long serialVersionUID = -8376644119766382385L;

    /**
     * Creates an empty instance.
     */
    public ActivityRuntimeException()
    {
        super();
    }

    /**
     * Creates an instance wrapping another throwable.
     *
     * @param throwable the root cause
     */
    public ActivityRuntimeException(final Throwable throwable)
    {
        super(throwable);
    }

    /**
     * Creates an instance with a message and root cause.
     *
     * @param message    error message
     * @param throwable  the root cause
     */
    public ActivityRuntimeException(final String message, final Throwable throwable)
    {
        super(message, throwable);
    }

    /**
     * Creates an instance with a message.
     *
     * @param message error message
     */
    public ActivityRuntimeException(final String message)
    {
        super(message);
    }
}