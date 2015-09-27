package fs.sicarius.monitor.watchers;

import static java.nio.file.StandardWatchEventKinds.ENTRY_CREATE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_DELETE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_MODIFY;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

	public void initialize(String path) {
		Path dir = Paths.get(path);
		try {
			log.debug("Registering file watcher on {}", path);
			WatchService watcher = FileSystems.getDefault().newWatchService();
			dir.register(watcher, ENTRY_CREATE, ENTRY_DELETE, ENTRY_MODIFY);
			watchers.put(path, watcher);
		} catch (IOException e) {
			log.error("Unable to register new file watcher");
		}
	}

	@Override
	public boolean check() {
		boolean allowCreate = this.event==null || this.event.contains("CREATE");
		boolean allowDelete = this.event==null || this.event.contains("DELETE");
		boolean allowModify = this.event==null || this.event.contains("MODIFY");
		String filter = null;
		
		// prepare filter for regex
		if (this.filter!=null) {
			filter = this.filter.replace("?", ".?").replace("*", ".*?");
		} else {
			filter = ".*?";
		}

		// Register watcher in case it does not exist
		if (!watchers.containsKey(this.path)) {
			initialize(this.path);
		}
		
		// Wait for changes
		try {
			boolean changes = false;
			WatchKey watckKey = watchers.get(this.path).take();
			List<WatchEvent<?>> events = watckKey.pollEvents();
			for (WatchEvent<?> event : events) {
				if (allowCreate && event.kind() == ENTRY_CREATE) {
					if (event.context().toString().matches(filter)) {
						if (isFinished(this.path + "/" + event.context().toString())) {
							log.info("Created: " + event.context().toString());
							changes = true;
						}
					}
				}
				if (allowDelete && event.kind() == ENTRY_DELETE) {
					if (event.context().toString().matches(filter)) {
						log.info("Delete: " + event.context().toString());
						changes = true;
					}
				}
				if (allowModify && event.kind() == ENTRY_MODIFY) {
					if (event.context().toString().matches(filter)) {
						if (isFinished(this.path + "/" + event.context().toString())) {
							log.info("Modify: " + event.context().toString());
							changes = true;
						}
					}
				}
			}

			watckKey.cancel();
			watchers.get(this.path).close();
			watchers.remove(this.path);
			return changes;
		} catch (Exception e) {
			log.error("Unable to check for filesystem changes on {}", this.path);
		}

		return false;
	}

	/** 
	 * Returns if the given file is finished copying
	 * @param fileName
	 * @return
	 */
	private boolean isFinished(String fileName) {
		try {
			File f = new File(fileName);
			TimeUnit.MILLISECONDS.sleep(100);
			long size = f.length();
			TimeUnit.MILLISECONDS.sleep(1000);
			//log.debug("{}", size!=f.length());
			if (size!=f.length()) {
				return isFinished(fileName);
			}
		} catch (InterruptedException e) {
			log.error("isFinished could not execute due to {}", e.getMessage());
		}

		return true;
	}
}
