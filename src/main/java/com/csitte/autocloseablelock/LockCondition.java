package com.csitte.autocloseablelock;


/**
 * The LockCondition class provides a way to associate a state with a lock,
 * allowing threads to wait for a certain condition to be met before proceeding.
 *
 * Usage example:
 *  {@code
 *  LockCondition<String> lockCondition =
 *      new LockCondition<>(closeableLock, "initial state");
 *  lockCondition.setState("new state");
 *  }
 *
 * @param <T> the type of the state associated with the lock
 */
@SuppressWarnings("PMD.CommentSize")
public class LockCondition<T>
{
    /** State of condition */
    private T state;

    /** Lock for condition */
    private final CloseableLock lock;

    /**
     *  Constructor
     *
     *  @param lock         the associated lock
     *  @param initialState the initial state of this condition
     */
    public LockCondition(final CloseableLock lock, final T initialState)
    {
        this.lock = lock;
        this.state = initialState;
        lock.getOrCreateCondition();
    }

    /**
     *  Set lock condition.
     *
     *  Waiting threads are signalled.
     *
     *  @param state    new state
     */
    public void setState(final T state)
    {
        try (AutoCloseableLock autoCloseableLock = lock.lock())
        {
            assert autoCloseableLock != null; // ignored on runtime
            this.state = state;
            lock.signalAll();
        }
    }

    /**
     *  @return current lock condition
     */
    public T getState()
    {
        try (AutoCloseableLock autoCloseableLock = lock.lock())
        {
            assert autoCloseableLock != null; // ignored on runtime
            return state;
        }
    }

    /**
     *  Convenience class for Boolean.
     */
    public static class BooleanLockCondition extends LockCondition<Boolean>
    {
        /** Constructor */
        public BooleanLockCondition(final CloseableLock lock)
        {
            super(lock, false);
        }
        /** @return state is true */
        public boolean isTrue()
        {
            return Boolean.TRUE.equals(getState());
        }
    }

    @Override
    public String toString()
    {
        return state==null? "": state.toString();
    }
}
