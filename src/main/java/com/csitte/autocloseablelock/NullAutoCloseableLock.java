package com.csitte.autocloseablelock;

/** Null-Objekt-Pattern class */
public final class NullAutoCloseableLock implements AutoCloseableLock
{
    /** Singleton */
    public static final NullAutoCloseableLock INSTANCE = new NullAutoCloseableLock();

    /** Constructor */
    private NullAutoCloseableLock()
    {
        // do nothing
    }

    /** @see java.lang.AutoCloseable */
    @Override
    public void close()
    {
        // do nothing
    }
}
