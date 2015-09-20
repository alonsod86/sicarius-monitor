package fs.sicarius.monitor.actuators;

import fs.sicarius.monitor.watchers.IMonitor;

/**
 * Defines a standard action when a monitor returns failure
 * @author alonso
 *
 */
public interface IAction {

	public void execute(IMonitor monitor);
}
