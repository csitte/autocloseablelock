package com.csitte.activity;

import java.io.Closeable;

/** Closeable Activity */
public interface CloseableActivity extends Closeable
{
    @Override
    void close();
}