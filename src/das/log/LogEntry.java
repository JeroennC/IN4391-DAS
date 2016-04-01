package das.log;

import java.io.IOException;
import java.io.Serializable;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.sql.Timestamp;

import das.server.Server;

public class LogEntry implements Serializable {
	private static final long serialVersionUID = -7953115230912940579L;
	
	private String source;
	private String content;
	private long timestamp;
	
	public LogEntry(String source, String content, long timestamp) {
		this.source = source;
		this.content = content;
		this.timestamp = timestamp;
	}
	
	public void WriteToFile(String filename) {
		try {
			Files.write(Paths.get(Server.LOG_DIR + "/" + filename), this.toString().getBytes(), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	@Override
	public String toString() {
		return source + "(@" + new Timestamp(timestamp).toString() + "): " + content;
	}
}
