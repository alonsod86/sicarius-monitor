package fs.sicarius.monitor;

import com.google.gson.Gson;

public class Event {

	private String message;
	
	private Long timestamp;

	public Event(String message, Long timestamp) {
		super();
		this.message = message;
		this.timestamp = timestamp;
	}

	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
	}

	public Long getTimestamp() {
		return timestamp;
	}

	public void setTimestamp(Long timestamp) {
		this.timestamp = timestamp;
	}
	
	public String toJson() {
		return new Gson().toJson(this);
	}
}
