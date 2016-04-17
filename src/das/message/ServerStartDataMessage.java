package das.message;

import java.util.List;
import java.util.Queue;

import das.Battlefield;
import das.Node;
import das.Node_RMI;
import das.server.Server;
import das.server.StateCommand;

/**
 * Message for passing an initialization state from Server to Server
 */
public class ServerStartDataMessage extends Message {
	private static final long serialVersionUID = -4816245041097986223L;
	
	Battlefield bf;
	Queue<StateCommand> inbox;
	List<String> log;
	long time;
	
	public ServerStartDataMessage(Node from, Address to, String to_id, Battlefield bf, Queue<StateCommand> inbox, List<String> log, long time) {
		super(from, to, to_id);
		this.bf = bf;
		this.inbox = inbox;
		this.log = log;
		this.time = time;
	}	
	
	@Override
	public void receive(Node_RMI node) {
		((Server) node).receiveServerStartDataMessage(this);

	}

	public synchronized Battlefield getBf() {
		return bf;
	}

	public synchronized void setBf(Battlefield bf) {
		this.bf = bf;
	}

	public synchronized Queue<StateCommand> getInbox() {
		return inbox;
	}

	public synchronized void setInbox(Queue<StateCommand> inbox) {
		this.inbox = inbox;
	}

	public synchronized long getTime() {
		return time;
	}

	public synchronized void setTime(long time) {
		this.time = time;
	}
	
	public synchronized List<String> getLog() {
		return log;
	}
	
	public synchronized void setLog(List<String> log) {
		this.log = log;
	}
	

}
