package fs.sicarius.monitor.actuators;

import fs.sicarius.monitor.watchers.IMonitor;

/**
 * Sleep action
 * @author alonso
 *
 */
public class SleepAction implements IAction {
	private Long millis = null;
	@Override
	public void execute(IMonitor monitor) {
		try {
			Thread.sleep(millis);
		} catch (InterruptedException e) {}
	}

}
