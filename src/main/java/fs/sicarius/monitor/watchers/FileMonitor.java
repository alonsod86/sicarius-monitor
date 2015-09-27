package fs.sicarius.monitor.watchers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.*;
import java.util.HashMap;
import java.util.List;

import static java.nio.file.StandardWatchEventKinds.*;

/**
 * Monitors UNIX/MacOS processes
 * @author alonso
 *
 */
public class FileMonitor implements IAsyncMonitor {
	private Logger log = LoggerFactory.getLogger(FileMonitor.class);
	private static HashMap<String, WatchService> watchers = new HashMap<>(); 

	private String path;
	private String filter;
	private String event;

	public void registerNewWatcher(String path) {
		Path dir = Paths.get(path);
		try {
			WatchService watcher = FileSystems.getDefault().newWatchService();
			dir.register(watcher, ENTRY_CREATE, ENTRY_DELETE, ENTRY_MODIFY);
			watchers.put(path, watcher);
		} catch (IOException e) {
			log.error("Unable to register new file watcher");
		}

		// prepare filter for regex
		if (this.filter!=null) {
			this.filter = this.filter.replace("?", ".?").replace("*", ".*?");
		} else {
			this.filter = ".*?";
		}
	}

	@Override
	public boolean check() {
		boolean allowCreate = this.event==null || this.event.contains("CREATE");
		boolean allowDelete = this.event==null || this.event.contains("DELETE");
		boolean allowModify = this.event==null || this.event.contains("MODIFY");

		// Register watcher in case it does not exist
		if (!watchers.containsKey(this.path)) {
			registerNewWatcher(this.path);
		}
		// Wait for changes
		try {
			boolean changes = false;
			WatchKey watckKey = watchers.get(this.path).take();
			List<WatchEvent<?>> events = watckKey.pollEvents();
			for (WatchEvent<?> event : events) {
				if (allowCreate && event.kind() == ENTRY_CREATE) {
					if (event.context().toString().matches(this.filter)) {
						log.info("Created: " + event.context().toString());
						changes = true;
					}
				}
				if (allowDelete && event.kind() == ENTRY_DELETE) {
					if (event.context().toString().matches(this.filter)) {
						log.info("Delete: " + event.context().toString());
						changes = true;
					}
				}
				if (allowModify && event.kind() == ENTRY_MODIFY) {
					if (event.context().toString().matches(this.filter)) {
						log.info("Modify: " + event.context().toString());
						changes = true;
					}
				}
			}

			watckKey.reset();
			return changes;
		} catch (Exception e) {
			log.error("Unable to check for filesystem changes on {}", this.path);
		}

		return false;
	}
}
