package test.com.csitte.activity;


import java.time.Duration;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.csitte.activity.Activity;
import com.csitte.autocloseablelock.AutoCloseableLock;
import com.csitte.autocloseablelock.AutoCloseableWriteLock;
import com.csitte.autocloseablelock.CloseableReadWriteLock;
import com.csitte.autocloseablelock.LockCondition.BooleanLockCondition;

public class DummyActivity extends Activity<String> implements Runnable
{
	private static final Logger LOG = LogManager.getLogger(DummyActivity.class);

	private BooleanLockCondition threadIsRunning;
	private CloseableReadWriteLock closeableReadWriteLock = new CloseableReadWriteLock();
	private BooleanLockCondition shutdownRequest;


	/**
	 *	Constructor
	 */
	public DummyActivity()
	{
		super();
		shutdownRequest = new BooleanLockCondition(getLock());
		threadIsRunning = new BooleanLockCondition(getLock());
	}


	public void startActivityThread()
	{
		try (AutoCloseableLock acl = getLock().lock())
		{
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
			threadIsRunning.setState(true);
			for (int idx=1; idx <= 10; idx++)
			{
				LOG.debug("thread loop #{}", idx);
				//- wait 1 second
				if (getLock().waitForCondition(() -> isShutdownRequest(), Duration.ofSeconds(1)))
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

	public boolean setShutdownRequest(boolean value)
	{
		try (AutoCloseableWriteLock acwl = closeableReadWriteLock.writeLock())
		{
		    Boolean prevValue = shutdownRequest.getState();
			shutdownRequest.setState(value);
			acwl.downgradeToReadLock(); // only for junit test
			return prevValue;
		}
	}

	public boolean isShutdownRequest()
	{
	    return shutdownRequest.getState();
	}
}
