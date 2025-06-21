package com.csitte.activity;

import java.time.Instant;

import com.csitte.autocloseablelock.CloseableLock;
import com.csitte.autocloseablelock.LockCondition;

/**
 *  Represents an activity which has a start, optional status updates and an end.
 *
 *  <p>Use {@link #getLock()} to access the activity lock and
 *  {@link #getCondition()} to wait for state changes.</p>
 *
 *  @param <T> type used for activity status
 */
public interface Activity<T>
{
    /**
     * Returns the lock protecting the activity.
     *
     * @return activity lock
     */
    CloseableLock getLock();

    /**
     * Returns the condition associated with the activity.
     *
     * @return activity condition
     */
    LockCondition<T> getCondition();

    /**
     * Indicates whether the activity is currently active.
     *
     * @return {@code true} if active
     */
    boolean isActive();

    /**
     * Returns the start time of the activity.
     *
     * @return start-of-activity timestamp or {@code null} if not started
     */
    Instant getStartOfActivity();

    /**
     * Returns the timestamp of the last recorded activity.
     *
     * @return timestamp of last activity
     */
    Instant getLastActivity();

    /**
     * Returns the time at which the activity ended.
     *
     * @return end-of-activity timestamp, {@code null} if unknown
     */
    Instant getEndOfActivity();

    /**
     * Updates the activity status.
     *
     * @param status new status value
     */
    void updateStatus(T status);

    /**
     * Returns the current status value.
     *
     * @return activity-status condition
     */
    T getStatus();

    /**
     * Updates the last-activity timestamp and returns it.
     *
     * @return updated timestamp
     */
    Instant touch();

    /**
     * Close this activity.
     */
    void close();
}
