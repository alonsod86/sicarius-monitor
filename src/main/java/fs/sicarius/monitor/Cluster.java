package fs.sicarius.monitor;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import javax.annotation.PostConstruct;

import org.jgroups.JChannel;
import org.jgroups.blocks.ReplicatedHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

@Component
public class Cluster {
	private Logger log = LoggerFactory.getLogger(Cluster.class);

	@Autowired
	private Environment env;
	
	/** Channel for monitor communication inside the cluster */
	private JChannel serviceCh = null;
	
	/** Distributed metrics for cluster */
	private ReplicatedHashMap<String, HashMap<String, String>> metrics = null;
	
	/** Distributed event log */
	private ReplicatedHashMap<String, HashMap<String, List<String>>> events = null;
	
	/** Current member name */
	private String member = null;
	
	@PostConstruct
	public void init() {
		String path = getClass().getClassLoader().getResource("udp.xml").getFile();
		if (env.containsProperty("config.path") && !env.getProperty("config.path").isEmpty()) {
			path = env.getProperty("config.path") + "/udp.xml";
		}
		
		try {
			serviceCh = new JChannel(new File(path));
			metrics = new ReplicatedHashMap<>(serviceCh);
			events = new ReplicatedHashMap<String, HashMap<String,List<String>>>(serviceCh);
		} catch (Exception e) {
			log.error("Unable to open service communication channel due to {}", e.getMessage());
		}
	}
	
	public ReplicatedHashMap<String, HashMap<String, String>> getMetrics() {
		return metrics;
	}
	
	public void syncMember(String member) {
		this.member = member;
		metrics._put(this.member, new HashMap<>());
		events._put(this.member, new HashMap<>());
	}
	
	public void updateStats(String name, Stat value) {
		HashMap<String, String> stats = metrics.get(this.member);
		stats.put(name, value.toJson());
		metrics._put(this.member, stats);
	}
	
	public void updateStats(String name, Stat... values) {
		HashMap<String, String> stats = metrics.get(this.member);
		for (Stat value:values) {
			stats.put(name, value.toJson());
		}
		metrics._put(this.member, stats);
	}
	
	public void registerEvent(String name, Event event) {
		// get member event log for every monitor
		HashMap<String, List<String>> hEvents = events.get(this.member);
		if (!hEvents.containsKey(name)) {
			hEvents.put(name, new ArrayList<>());
		}
		List<String> evts = hEvents.get(name);
		evts.add(event.toJson());
		hEvents.put(name, evts);
		events._put(this.member, hEvents);
	}
}
