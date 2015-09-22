package fs.sicarius.monitor.watchers;

import java.io.BufferedReader;
import java.io.InputStreamReader;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Monitors UNIX/MacOS processes
 * @author alonso
 *
 */
public class ProcessMonitor implements IMonitor {
	private Logger log = LoggerFactory.getLogger(ProcessMonitor.class);
	private String process;
	
	@Override
	public boolean check() {
		Integer found = 0;
		
		try {
	        String line;
	        Process p = Runtime.getRuntime().exec("ps -e");
	        BufferedReader input =
	                new BufferedReader(new InputStreamReader(p.getInputStream()));
	        while ((line = input.readLine()) != null) {
	            if (line.contains(process)) {
	            	found++;
	            }
	        }
	        input.close();
	    } catch (Exception err) {
	        log.error("Unable to fetch processes due to {}", err.getMessage());
	    }
		
		Integer max = Integer.parseInt(expect.get("processes").toString());
		return found.equals(max);
	}
}
