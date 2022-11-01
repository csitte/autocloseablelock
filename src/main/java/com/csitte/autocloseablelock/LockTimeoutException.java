package com.csitte.autocloseablelock;

import java.time.Duration;

public class LockTimeoutException extends LockException
{
    private static final long serialVersionUID = 1;

    private final Duration elapsedTime;


    /**
     *	Constructor.
     *
     *	@param	name			name of lock
     *	@param	elapsedTime		elapsed time waiting for a lock
     */
    public LockTimeoutException(String name, Duration elapsedTime)
    {
        super(name + " - timeout after " + elapsedTime);
        this.elapsedTime = elapsedTime;
    }

    /**
     *	Constructor.
     *
     *	@param	elapsedTime		elapsed time waiting for a lock
     */
    public LockTimeoutException(Duration elapsedTime)
    {
        super("timeout after " + elapsedTime);
        this.elapsedTime = elapsedTime;
    }

    public Duration getElapsedTime()
    {
        return elapsedTime;
    }
}
