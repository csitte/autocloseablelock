package com.csitte.autocloseablelock;

/**
 *  The thread that holds the write lock can downgrade to a read lock.
 *  It can acquire a read lock before it releases the write lock to downgrade from a write lock to a read lock
 *  while ensuring that no other thread is permitted to acquire the write lock
 *  while it is in the process of downgrading.
 *
*/
public interface AutoCloseableWriteLock extends AutoCloseableLock
{
    void downgradeToReadLock();

    void downgradeToReadLockInterruptibly();
}