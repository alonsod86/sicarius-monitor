package fs.sicarius.monitor;

import com.google.gson.Gson;

public class Stat {

	private Boolean alive;
	private Long timestamp;
	public Stat(Boolean alive, Long timestamp) {
		super();
		this.alive = alive;
		this.timestamp = timestamp;
	}
	public Boolean getAlive() {
		return alive;
	}
	public void setAlive(Boolean alive) {
		this.alive = alive;
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
