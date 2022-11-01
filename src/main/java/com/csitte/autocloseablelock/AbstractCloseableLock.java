package com.csitte.autocloseablelock;

import java.util.concurrent.atomic.AtomicInteger;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;


/**
 *  Abstract base class of CloseableLock's.
 */
public abstract class AbstractCloseableLock
{
    private static final Logger LOG = LogManager.getLogger(AbstractCloseableLock.class);

    //- Variables used for logging purposes:
    protected String name;
    protected static final AtomicInteger serial = new AtomicInteger();
    protected final AtomicInteger lockIndex = new AtomicInteger();


    /**
     *  Constructor.
     *
     *  @param  info  logging-info
     */
    protected AbstractCloseableLock(Object info)
    {
        int num = serial.incrementAndGet();
        String postfix = "-lock-" + num;
        if (info instanceof String)
        {
            name = info.toString() + postfix;
        }
        else if (info instanceof Class<?>)
        {
            name = ((Class<?>)info).getSimpleName() + postfix;
        }
        else if (info != null)
        {
            name = info.getClass().getSimpleName() + postfix;
        }
        else
        {
            name = this.getClass().getSimpleName();
            String pname = this.getClass().getPackage().getName();

            //- Search for calling object to use its name
            for (StackTraceElement element : Thread.currentThread().getStackTrace())
            {
                String className = element.getClassName();
                if (!className.startsWith(pname)
                    && !className.startsWith("java.")
                    && !className.startsWith("sun."))
                {
                    name = className + '.' + element.getLineNumber();
                    break;
                }
            }
            name = name + '-' + num;
        }
        LOG.debug("{} created", name);
    }

    protected int getNextLockIndex()
    {
        return lockIndex.incrementAndGet();
    }

    /**
     *  @return info-text
     */
    public String getName()
    {
        return name;
    }
}