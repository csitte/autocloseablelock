# AutoCloseable Lock

This package provides a wrapper for the java.util.concurrent.locks.Lock interface
which can be used with the Java try-with-resources functionality.

## Basic Usage

Instead of the traditional way:

        Lock myLock = new ReentrantLock();
        void method()
        {
            myLock.lock();
            try
            {
                do(something);
            }
            finally
            {
                myLock.close();
            }
        }
        
Use it with the try-with-resources feature:

        CloseableLock myLock = new CloseableLock();
        void method()
        {
            try (AutoCloseableLock autoCloseableLock = myLock.lock())
            {
                do(something);
            }
        }
        
When the scope is left, it is ensured that the lock will be released.
        
## Try lock with timeout

        CloseableLock myLock = new CloseableLock();
        Duration timeout = Duration.ofSeconds(10);
        try (AutoCloseableLock autoCloseableLock = myLock.tryLock(timeout))
        {
            do(something);
        }
        catch (LockTimeoutException runtimeException)
        {
            // do appropriate error handling
        }

If the lock cannot be aquired before the timeout duration expires, than a LockTimeoutException is thrown.

## Wait

The wait()-Method does what it's name says: waiting for the specified time.

        new CloseableLock().wait(Duration.ofSeconds(10));
        
## Wait for condition

You can use a BooleanSupplier as an argument to test if a condition is true, until the given timeout expires.

        CloseableLock myLock = new CloseableLock();
        if (myLock.waitForCondition(()->isReady(), Duration.ofSeconds(10))
        {
            # condition which is tested by isReady() is true. Continue processing...
        }
        else
        {
            # condition is not ready after 10 seconds. Do error handling...
        }
            
A timeout value of zero means, that the method only returns when the condition is true.
Any other thread can use the CloseableLock.signalAll() method to signal waiting threads a change in condition.
Otherwise the test is performed in one-second intervals.

## ReadWriteLock

Use CloseableReadWriteLock if you need the ReadWriteLock functionality and want also the AutoCloseable benefits.
The read lock may be simultaneously held by multiple threads as long as there is no write.
One speciality here is the possibility to downgrade a write-lock to a read-lock.

        CloseableReadWriteLock readWriteLock = new CloseableReadWriteLock();
        void method()
        {
            try (AutoCloseableWriteLock acwl = readWriteLock.writeLock())
            {
                modifyProtectedResource();
                
                acwl.downgradeToReadLock();
                
                doReadOnlyActivity();
            }
        }
        
The CloseableReadWriteLock() has the following locking-methods:

            AutoCloseableWriteLock writeLock()
            AutoCloseableWriteLock tryWriteLock(Duration)
            AutoCloseableLock readLock()
            AutoCloseableLock tryReadLock(Duration)
            void downgradeToReadLock()
            
## LockCondition

This class represents a state. It is bound to a lock. If the state of the LockCondition changes,
this is signalled to all waiting threads. The setState()-method aquires the lock before changing the state.

        CloseableLock myLock = new CloseableLock();
        LockCondition<State> state = new LockCondition<>(myLock, State.INIT);

        void doActivity()
        {
            state.setState(State.ACTIVE);
            doSomethine();
            state.setState(State.FINISHED);
        }
        
        void waitUntilActivityHasFinished()
        {
            myLock.waitForCondition(()->state.getState()==State.FINISHED, timeout);    
        }
        
## BooleanLockCondition

This is a convenience class for LockCondition<Boolean>. It has the initial default value of FALSE.

        CloseableLock myLock = new CloseableLock();
        BooleanLockCondition finished = new BooleanLockCondition<>(myLock);

        void doActivity()
        {
            doSomethine();
            finished.setState(true);
        }
        
        void waitUntilActivityHasFinished()
        {
            myLock.waitForCondition(()->finished.isTrue(), timeout);    
        }

