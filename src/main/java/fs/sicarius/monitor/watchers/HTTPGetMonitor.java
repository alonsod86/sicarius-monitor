package fs.sicarius.monitor.watchers;

import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

/**
 * Checks the given REST service
 * @author alonso
 *
 */
public class HTTPGetMonitor extends DefaultMonitor {
	private RestTemplate rest = null;
	private String path = null;

	public HTTPGetMonitor() {
		rest = new RestTemplate();
	}

	@Override
	public boolean check() {
		try {
			ResponseEntity<String> response = rest.exchange(path, HttpMethod.GET, null, String.class);
			// check that message contains the expected answer
			boolean code = expect.containsKey("status")?String.valueOf(response.getStatusCode().value()).equals(expect.get("status").toString()):true;
			boolean message = expect.containsKey("message")?response.getBody().contains(expect.get("message").toString()):true;
			return code && message;
		} catch (Exception e) {
			return false;
		}
	}

}
