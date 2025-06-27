/*
 * Copyright 2022-2025 C.Sitte Softwaretechnik
 * SPDX-License-Identifier: MIT
 */
package com.csitte.activity;

import java.time.Instant;

import com.csitte.autocloseablelock.AutoCloseableLock;
import com.csitte.autocloseablelock.CloseableLock;
import com.csitte.autocloseablelock.LockCondition;


/**
 *	Represents an activity which has a start, some sort of activity and an end.
 *
 *  <p>Use the {@link #startActivity()} method to access the resources of an activity
 *  and {@link #getCondition()} to synchronize with any activity condition.</p>
 *
 *  @param <T> type used for activity status
 */
@SuppressWarnings("PMD.AtLeastOneConstructor")
public class ActivityImpl<T> implements Activity<T>
{
    /** Default constructor */
    public ActivityImpl() {
        // nothing to initialize
    }

    //- Activity timestamps

    /** Start-Of-Activity timestamp */
    private Instant startOfActivity = Instant.EPOCH; // '1970' (no activity)

    /** Timestamp of last activity */
    private Instant lastActivity = Instant.EPOCH; // '1970' (unknown)

    /** End-Of-Activity Timestamp */
    private Instant endOfActivity = Instant.MAX; // unknown end

    /** Activity-Condition */
    private LockCondition<T> condition;

    /** Lock to support multi-threaded access */
    private final CloseableLock activityLock = new CloseableLock();


    /** @return  activity-lock */
    @Override
    public CloseableLock getLock()
    {
        return activityLock;
    }

    /** @return activity-condition */
    @Override
    public LockCondition<T> getCondition()
    {
        if (condition == null)
        {
            try (AutoCloseableLock lock = activityLock.lock())
            {
                assert lock != null; // ignored on runtime
                if (condition == null)
                {
                    condition = new LockCondition<>(activityLock, null);
                }
            }
        }
        return condition;
    }

    /** @return is activity active? */
    @Override
    public boolean isActive()
    {
        try (AutoCloseableLock lock = activityLock.lock())
        {
            assert lock != null; // ignored on runtime
            return startOfActivity != Instant.EPOCH && endOfActivity == Instant.MAX;
        }
    }

    /** @return start-of-activity timestamp */
    @Override
    public Instant getStartOfActivity()
    {
        return startOfActivity == Instant.EPOCH? null: startOfActivity;
    }

    /** @return timestamp of last activity */
    @Override
    public Instant getLastActivity()
    {
        return lastActivity;
    }

    /** @return end-of-activity timestamp. null == unknown */
    @Override
    public Instant getEndOfActivity()
    {
        return endOfActivity==Instant.MAX? null: endOfActivity;
    }

    /** Update activity-status condition */
    @Override
    public void updateStatus(final T status)
    {
        try (AutoCloseableLock lock = activityLock.lock())
        {
            assert lock != null; // ignored on runtime
            getCondition().setState(status);
            lastActivity = Instant.now();
        }
    }

    /** @return activity-status condition */
    @Override
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
            assert lock != null; // ignored on runtime
            if (isActive())
            {
                throw new ActivityRuntimeException("already active");
            }
            //- setup timestamps
            startOfActivity = Instant.now();
            lastActivity = startOfActivity;
            endOfActivity = Instant.MAX; // unknown end-of-activity
            return this::close;
        }
    }

    /** Update last-activity timestamp */
    @Override
    public Instant touch()
    {
        try (AutoCloseableLock lock = activityLock.lock())
        {
            assert lock != null; // ignored on runtime
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
    @Override
    public void close()
    {
        try (AutoCloseableLock autoCloseableLock = activityLock.lock())
        {
            assert autoCloseableLock != null; // ignored on runtime
            if (!isActive())
            {
                throw new ActivityRuntimeException("not active");
            }
            lastActivity = Instant.now();
            endOfActivity = lastActivity;   // set end-of-activity (= last activity)
        }
    }

    @Override
    public String toString()
    {
        try (AutoCloseableLock lock = activityLock.lock())
        {
            assert lock != null; // ignored on runtime
            return " startOfActivity=" + (startOfActivity==Instant.EPOCH? "": startOfActivity.toString())
            	 + " lastActivity="    + lastActivity
            	 + " endOfActivity="   + (endOfActivity==Instant.MAX? "": endOfActivity.toString())
            	 + " status="          + condition;
        }
    }
}
