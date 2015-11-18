package fs.sicarius.monitor.watchers;

import java.util.HashMap;

public abstract class DefaultMonitor implements IMonitor {
	/** Expected result that triggers the monitor */
	public HashMap<String, Object> expect = new HashMap<>();
}
