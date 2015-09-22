package fs.sicarius.monitor.watchers;

import static java.nio.file.StandardWatchEventKinds.ENTRY_CREATE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_DELETE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_MODIFY;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.HashMap;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Monitors UNIX/MacOS processes
 * @author alonso
 *
 */
public class FileMonitor implements IMonitor {
	private Logger log = LoggerFactory.getLogger(FileMonitor.class);
	private static HashMap<String, WatchService> watchers = new HashMap<>(); 

	private String path;

	private boolean resolved = false;
	
	public void registerNewWatcher(String path) {
		Path dir = Paths.get(path);
		try {
			WatchService watcher = FileSystems.getDefault().newWatchService();
			dir.register(watcher, ENTRY_CREATE, ENTRY_DELETE, ENTRY_MODIFY);
			watchers.put(path, watcher);
		} catch (IOException e) {
			log.error("Unable to register new file watcher");
		}
	}

	@Override
	public boolean check() {
		this.resolved = false;
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
				if (event.kind() == ENTRY_CREATE) {
					log.info("Created: " + event.context().toString());
					changes = true;
				}
				if (event.kind() == ENTRY_DELETE) {
					log.info("Delete: " + event.context().toString());
					changes = true;
				}
				if (event.kind() == ENTRY_MODIFY) {
					log.info("Modify: " + event.context().toString());
					changes = true;
				}
			}

			watckKey.reset();
			this.resolved = true;
			return changes;
		} catch (Exception e) {
			log.error("Unable to check for filesystem changes on {}", this.path);
		}

		return false;
	}
	
	@Override
	public boolean isAsync() {
		return true;
	}
	
	@Override
	public boolean isResolved() {
		return this.resolved;
	}
}
