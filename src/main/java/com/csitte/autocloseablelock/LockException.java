/*
 * Copyright 2022-2025 C.Sitte Softwaretechnik
 * SPDX-License-Identifier: MIT
 */
package com.csitte.autocloseablelock;


/**
 * A runtime exception thrown to indicate errors related to lock operations in the {@code AutoCloseableLock} framework.
 * This exception is typically used when a lock cannot be acquired, released, or managed properly.
 */
public class LockException extends RuntimeException
{
    private static final long serialVersionUID = 1;

    /**
     * Constructs a new {@code LockException} with no detail message or cause.
     */
    public LockException()
    {
        super();
    }

    /**
     * Constructs a new {@code LockException} with the specified detail message.
     *
     * @param message the detail message describing the error
     */
    public LockException(final String message)
    {
        super(message);
    }

    /**
     * Constructs a new {@code LockException} with the specified cause.
     *
     * @param throwable the cause of the exception
     */
    public LockException(final Throwable throwable)
    {
        super(throwable);
    }

    /**
     * Constructs a new {@code LockException} with the specified detail message and cause.
     *
     * @param message   the detail message describing the error
     * @param throwable the cause of the exception
     */
    public LockException(final String message, final Throwable throwable)
    {
        super(message, throwable);
    }
}
