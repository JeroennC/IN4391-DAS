package das.log;

import java.io.File;
import java.util.LinkedList;
import java.util.List;

import das.server.Server;

/**
 * Runnable class that collects Log entries and periodically (waitTime ms) sends the content to all other servers
 *
 */
public class LogSender implements Runnable{
	private Server server;
	private List<LogEntry> logStash;
	private long waitTime = 5000;
	
	public LogSender(Server server) {
		this.server = server;	
		logStash = new LinkedList<LogEntry>();
		
		// Make sure directory exists
		File f = new File(Server.LOG_DIR);
		if (!f.exists())
			f.mkdirs();
	}
	
	public void add(LogEntry e) {
		synchronized(logStash) {
			logStash.add(e);
		}
	}
	
	/**
	 * Start periodically sending logs
	 */
	@Override
	public void run() {
		List<LogEntry> sendList = new LinkedList<LogEntry>();
		while (server.isRunning()) {
			synchronized(logStash) {
				if (!logStash.isEmpty()) {
					sendList = new LinkedList<LogEntry>(logStash);
					logStash.clear();
				}
			}
			server.sendLog(sendList);
			try { Thread.sleep(waitTime); } catch (InterruptedException e) { continue; }
		}
	}

}
