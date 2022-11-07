package com.csitte.activity;

import java.time.Instant;

import com.csitte.autocloseablelock.AutoCloseableLock;
import com.csitte.autocloseablelock.CloseableLock;
import com.csitte.autocloseablelock.LockCondition;


/**
 *	Represents an activity which has a start, some sort of activity and an end.
 *	Use the lock() method to access the resources of an activity.
 *  Use the waitForCondition() method to synchronize with any activity condition.
 */
public class Activity
{
    //- Activity timestamps
    private Instant startOfActivity;
    private Instant lastActivity = Instant.EPOCH;
    private Instant endOfActivity;
    private final LockCondition<String> activityCondition;
    private final CloseableLock activityLock;


    /**
     *  Constructor
     */
    public Activity()
    {
        activityLock = new CloseableLock();
        activityCondition = new LockCondition<>(activityLock, "");
    }

    public CloseableLock getLock()
    {
        return activityLock;
    }

    public boolean isActive()
    {
        try (AutoCloseableLock lock = activityLock.lock())
        {
            return startOfActivity != null && endOfActivity == null;
        }
    }

    public Instant getStartOfActivity()
    {
        return startOfActivity;
    }

    public Instant getLastActivity()
    {
        return lastActivity;
    }

    public Instant getEndOfActivity()
    {
        return endOfActivity;
    }

    public void updateStatus(String statusText)
    {
        try (AutoCloseableLock lock = activityLock.lock())
        {
            activityCondition.setState(statusText);
            lastActivity = Instant.now();
        }
    }

    public void errorStatus(String errorText)
    {
        try (AutoCloseableLock lock = activityLock.lock())
        {
            activityCondition.setState(errorText);
            lastActivity = Instant.now();
        }
    }

    public String getStatus()
    {
        return activityCondition.getState();
	}

    /**
     *	Start activity.
     *
     *	@param msg	activity-type text
     *  @return AutoCloseable object for try-with-resources functionality
     */
    public CloseableActivity startActivity()
    {
        try (AutoCloseableLock lock = activityLock.lock())
        {
            if (isActive())
            {
                throw new ActivityRuntimeException("already active");
            }
            //- setup timestamps
            startOfActivity = Instant.now();
            lastActivity = startOfActivity;
            endOfActivity = null;
            return this::close;
        }
    }

    public Instant touch()
    {
        try (AutoCloseableLock lock = activityLock.lock())
        {
            if (!isActive())
            {
                throw new ActivityRuntimeException();
            }
            lastActivity = Instant.now();
            return lastActivity;
        }
    }

    /**
     *	Close this activity
     */
    public void close()
    {
        try (AutoCloseableLock autoCloseableLock = activityLock.lock())
        {
            if (!isActive())
            {
                throw new ActivityRuntimeException("not active");
            }
            endOfActivity = Instant.now();
            lastActivity = endOfActivity;
        }
    }

    @Override
    public String toString()
    {
        try (AutoCloseableLock lock = activityLock.lock())
        {
            return " startOfActivity=" + startOfActivity
            	 + " lastActivity="    + lastActivity
            	 + " endOfActivity="   + endOfActivity
            	 + " status="          + activityCondition;
        }
    }
}
