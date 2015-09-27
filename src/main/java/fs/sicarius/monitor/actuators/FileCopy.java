package fs.sicarius.monitor.actuators;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fs.sicarius.monitor.watchers.IMonitor;

/**
 * File copier 
 * @author tiocansino
 *
 */
public class FileCopy implements IAction {
	private Logger log = LoggerFactory.getLogger(FileCopy.class);

	private String from;
	private String to;
	private String move;
	
	@Override
	public void execute(IMonitor monitor) {
		try {
			Files.copy(new File(this.from).toPath(), new File(this.to).toPath());
			if (this.move!=null && "true".equals(this.move)) {
				new File(this.from).delete();
			}
			log.info("File {} copied to {}", this.from, this.to);
		} catch (IOException e) {
			log.error("Unable to copy file {} to destination {} due to {}", from, to, e.getMessage());
		}
	}

}
