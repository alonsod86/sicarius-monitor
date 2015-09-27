package fs.sicarius.monitor.watchers;

import java.util.HashMap;

/**
 * Interface to define a new monitor pluggable in this system.
 * @author alonso
 *
 */
public interface IMonitor {
	/** Expected result that triggers the monitor */
	public HashMap<String, Object> expect = new HashMap<>();
	
	/** Executes the monitor to check the conditions */
	public boolean check();
}
