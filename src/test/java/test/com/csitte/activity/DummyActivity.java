package test.com.csitte.activity;


import java.time.Duration;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.csitte.activity.ActivityImpl;
import com.csitte.autocloseablelock.AutoCloseableLock;
import com.csitte.autocloseablelock.AutoCloseableWriteLock;
import com.csitte.autocloseablelock.CloseableReadWriteLock;
import com.csitte.autocloseablelock.LockCondition.BooleanLockCondition;


@SuppressWarnings("PMD")
public final class DummyActivity extends ActivityImpl<String> implements Runnable
{
	private static final Logger LOG = LogManager.getLogger(DummyActivity.class);

	private BooleanLockCondition threadIsRunning;
	private final CloseableReadWriteLock closeableRWLock = new CloseableReadWriteLock();
	private BooleanLockCondition shutdownRequest;


	/**
	 *	Constructor
	 */
	private DummyActivity()
	{
		super();
	}

	private void initialize()
	{
	    shutdownRequest = new BooleanLockCondition(getLock());
	    threadIsRunning = new BooleanLockCondition(getLock());
	}

	public static DummyActivity createInstance()
	{
	    final DummyActivity activity = new DummyActivity();
	    activity.initialize();
	    return activity;
	}


	public void startActivityThread()
	{
		try (AutoCloseableLock acl = getLock().lock())
		{
		    assert acl != null;
			threadIsRunning.setState(false);
			LOG.debug("create & start ACTIVITY-THREAD");
			new Thread(this, "ACTIVITY-THREAD").start();
		}
	}

	@Override
	public void run()
	{
		//- Do nothing for 10 sec in one sec intervals
		LOG.debug("start run()-method");
		try (AutoCloseableLock acl = getLock().lock())
		{
		    assert acl != null;
			threadIsRunning.setState(true);
			for (int idx=1; idx <= 10; idx++)
			{
				LOG.debug("thread loop #{}", idx);
				//- wait 1 second
				if (getLock().waitForCondition(this::isShutdownRequest, Duration.ofSeconds(1)))
				{
				    break;
				}
			}
		}
		finally
		{
			threadIsRunning.setState(false);
			getLock().signalAll();
			LOG.debug("end of run()-method");
		}
	}

	public boolean isThreadRunning()
	{
	    return threadIsRunning.isTrue();
	}

	public void setShutdownRequest(final boolean value)
	{
		try (AutoCloseableWriteLock acwl = closeableRWLock.writeLock())
		{
		    //final Boolean prevValue =
		    shutdownRequest.getState();
			shutdownRequest.setState(value);
			acwl.downgradeToReadLock(); // only for junit test
			//return prevValue;
		}
	}

	public boolean isShutdownRequest()
	{
	    return shutdownRequest.getState();
	}
}
