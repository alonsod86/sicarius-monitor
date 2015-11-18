package fs.sicarius.monitor.watchers;


/**
 * Interface to define a new monitor pluggable in this system.
 * @author alonso
 *
 */
public interface IMonitor {
	/** Executes the monitor to check the conditions */
	public boolean check();
}
