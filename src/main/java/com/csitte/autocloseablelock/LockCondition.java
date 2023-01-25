package com.csitte.autocloseablelock;


/**
 * The LockCondition class provides a way to associate a state with a lock,
 * allowing threads to wait for a certain condition to be met before proceeding.
 *
 * Usage example:
 *  LockCondition<String> lockCondition = new LockCondition<>(closeableLock, "initial state");
 *  lockCondition.setState("new state");
 *
 * @param <T> the type of the state associated with the lock
 */
public class LockCondition<T>
{
    private T state;

    private final CloseableLock lock;

    /**
     *  Constructor
     *
     *  @param lock         the associated lock
     *  @param initalState  the initial state of this condition
     */
    public LockCondition(CloseableLock lock, T initalState)
    {
        this.lock = lock;
        this.state = initalState;
        lock.getOrCreateCondition();
    }

    /**
     *  Set lock condition.
     *
     *  Waiting threads are signalled.
     *
     *  @param state    new state
     */
    public void setState(T state)
    {
        try (AutoCloseableLock autoCloseableLock = lock.lock())
        {
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
            return state;
        }
    }

    /**
     *  Convenience class for Boolean
     */
    public static class BooleanLockCondition extends LockCondition<Boolean>
    {
        public BooleanLockCondition(CloseableLock lock)
        {
            super(lock, false);
        }

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
