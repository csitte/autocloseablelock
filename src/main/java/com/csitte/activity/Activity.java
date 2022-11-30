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
public class Activity<T>
{
    //- Activity timestamps
    private Instant startOfActivity;
    private Instant lastActivity = Instant.EPOCH;
    private Instant endOfActivity;
    private LockCondition<T> condition;
    private final CloseableLock activityLock = new CloseableLock();


    public CloseableLock getLock()
    {
        return activityLock;
    }

    protected LockCondition<T> getCondition()
    {
        if (condition == null)
        {
            try (AutoCloseableLock lock = activityLock.lock())
            {
                if (condition == null)
                {
                    condition = new LockCondition<>(activityLock, null);
                }
            }
        }
        return condition;
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

    public void updateStatus(T status)
    {
        try (AutoCloseableLock lock = activityLock.lock())
        {
            getCondition().setState(status);
            lastActivity = Instant.now();
        }
    }

    public T getStatus()
    {
        return getCondition().getState();
	}

    /**
     *	Start activity.
     *
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
                throw new ActivityRuntimeException("not active");
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
            lastActivity = Instant.now();
            endOfActivity = lastActivity;
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
            	 + " status="          + condition;
        }
    }
}
