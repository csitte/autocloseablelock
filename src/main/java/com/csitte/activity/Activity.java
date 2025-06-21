package com.csitte.activity;

import java.time.Instant;

import com.csitte.autocloseablelock.CloseableLock;
import com.csitte.autocloseablelock.LockCondition;


/**
 *	Represents an activity which has a start,
 *  some sort of activity with optional states and an end.
 *	Use the getLock() method to access the activity lock.
 *  Use the getCondition() method to synchronize with any activity condition.
 */
public interface Activity<T>
{
    /** @return  activity-lock */
    CloseableLock getLock();

    /** @return activity-condition */
    LockCondition<T> getCondition();

    /** @return is activity active? */
    boolean isActive();

    /** @return start-of-activity timestamp */
    Instant getStartOfActivity();

    /** @return timestamp of last activity */
    Instant getLastActivity();

    /** @return end-of-activity timestamp. null == unknown */
    Instant getEndOfActivity();

    /** Update activity-status condition */
    void updateStatus(T status);

    /** @return activity-status condition */
    T getStatus();

    /** Update last-activity timestamp */
    Instant touch();

    /**
     *	Close this activity
     */
    void close();
}
