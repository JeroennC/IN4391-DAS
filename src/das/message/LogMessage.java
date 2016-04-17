package das.message;

import java.util.LinkedList;
import java.util.List;

import das.Node_RMI;
import das.log.LogEntry;
import das.server.Server;

/**
 * Message for sending a collection of logs from Server to Server
 */
public class LogMessage extends Message {
	private static final long serialVersionUID = 5868198204479026835L;
	private List<LogEntry> entries;
	
	public LogMessage(Server from, Address to, String to_id) {
		super(from, to, to_id);
		this.entries = new LinkedList<LogEntry>();
	}
	
	public LogMessage(Server from, Address to, String to_id, List<LogEntry> e) {
		super(from, to, to_id);
		this.entries = e;
	}
	
	public synchronized void AddEntry(LogEntry e) {
		entries.add(e);
	}
	
	public synchronized void AddEntry(String source, String content, long timestamp) {
		entries.add(new LogEntry(source,content,timestamp));
	}
	
	public synchronized List<LogEntry> getEntries() {
		return entries;
	}
	
	@Override
	public void receive(Node_RMI node) {
		((Server) node).receiveLogMessage(this);
	}

}
