package fs.sicarius.monitor.actuators;

import java.io.BufferedReader;
import java.io.InputStreamReader;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fs.sicarius.monitor.watchers.IMonitor;

public class ShellExecutorAction implements IAction {
	private Logger log = LoggerFactory.getLogger(ShellExecutorAction.class);

	private String command;

	@Override
	public void execute(IMonitor monitor) {
		StringBuffer output = new StringBuffer();

		Process p;
		try {
			p = Runtime.getRuntime().exec(command);
			p.waitFor();
			BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));

			String line = "";			
			while ((line = reader.readLine())!= null) {
				output.append(line + "\n");
			}
			log.info(output.toString());

		} catch (Exception e) {
			log.error("Unable to execute {} due to {}", command, e.getMessage());
		}
	}

}
