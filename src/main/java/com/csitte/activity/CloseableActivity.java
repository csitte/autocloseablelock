/*
 * Copyright 2022-2025 C.Sitte Softwaretechnik
 * SPDX-License-Identifier: MIT
 */
package com.csitte.activity;

import java.io.Closeable;

/** Closeable Activity */
public interface CloseableActivity extends Closeable
{
    @Override
    void close();
}