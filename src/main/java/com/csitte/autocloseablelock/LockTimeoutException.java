package com.csitte.autocloseablelock;

import java.time.Duration;

/** Runtime Exception for lock-timeout */
public class LockTimeoutException extends LockException
{
    private static final long serialVersionUID = 1;

    /** Elapsed time till timeout */
    private final Duration elapsedTime;


    /**
     *	Constructor.
     *
     *	@param	name			name of lock
     *	@param	elapsedTime		elapsed time waiting for a lock
     */
    public LockTimeoutException(final String name, final Duration elapsedTime)
    {
        super(name + " - timeout after " + elapsedTime);
        this.elapsedTime = elapsedTime;
    }

    /**
     *	Constructor.
     *
     *	@param	elapsedTime		elapsed time waiting for a lock
     */
    public LockTimeoutException(final Duration elapsedTime)
    {
        super("timeout after " + elapsedTime);
        this.elapsedTime = elapsedTime;
    }

    /** return elapsed time before timeout */
    public Duration getElapsedTime()
    {
        return elapsedTime;
    }
}
