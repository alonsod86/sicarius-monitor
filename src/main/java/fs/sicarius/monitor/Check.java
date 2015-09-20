package fs.sicarius.monitor;

import java.util.ArrayList;
import java.util.List;

import fs.sicarius.monitor.actuators.IAction;
import fs.sicarius.monitor.watchers.IMonitor;

public class Check {
	public String id;
	public IMonitor monitor;
	public List<IAction> trigger;
	public Check(String id, IMonitor monitor, List<IAction> trigger) {
		super();
		this.id = id;
		this.monitor = monitor;
		this.trigger = trigger;
	}
	public Check(String id, IMonitor monitor, IAction trigger) {
		super();
		this.id = id;
		this.monitor = monitor;
		this.trigger = new ArrayList<>();
		this.trigger.add(trigger);
	}
}
