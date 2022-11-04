package com.csitte.activity;

import java.io.Closeable;

public interface CloseableActivity extends Closeable
{
    @Override
    void close();
}