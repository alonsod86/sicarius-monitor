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
	
	/** Flag that indicates if the monitor is executed in async mode */
	public default boolean isAsync() {
		return false;
	}
	
	/** Flag that indicates if the monitor has received a change in asyn mode */
	public default boolean isResolved() {
		return false;
	}
	
	/** Executes the async task in background */
	public default boolean defer() {
		return false;
	}
}
