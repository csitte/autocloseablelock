package com.csitte.autocloseablelock;

import java.time.Duration;

/**
 * Runtime exception thrown when a lock acquisition times out.
 */
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

    /**
     * Returns the elapsed time before timeout.
     *
     * @return elapsed time before timeout
     */
    public Duration getElapsedTime()
    {
        return elapsedTime;
    }
}
