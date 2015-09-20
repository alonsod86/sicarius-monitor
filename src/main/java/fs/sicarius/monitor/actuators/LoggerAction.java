package fs.sicarius.monitor.actuators;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fs.sicarius.monitor.watchers.IMonitor;

/**
 * Logs an action on console
 * @author alonso
 *
 */
public class LoggerAction implements IAction {
	private Logger log = LoggerFactory.getLogger(LoggerAction.class);
	private String message = null;
	@Override
	public void execute(IMonitor monitor) {
		log = LoggerFactory.getLogger(monitor.getClass());
		log.info(message);
	}

}
