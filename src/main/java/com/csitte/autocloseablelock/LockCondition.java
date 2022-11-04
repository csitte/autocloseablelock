package com.csitte.autocloseablelock;


public class LockCondition<T>
{
    private T state;

    private final CloseableLock lock;

    /**
     *  Constructor
     *
     *  @param lock
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
            autoCloseableLock.signalAll();
        }
    }

    /**
     *  Get current lock condition
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
