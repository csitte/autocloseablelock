/*
 * Copyright 2022-2025 C.Sitte Softwaretechnik
 * SPDX-License-Identifier: MIT
 */
package com.csitte.autocloseablelock;

/** Null-Object pattern class */
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
