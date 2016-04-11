package das.message;

import java.util.List;
import java.util.Queue;

import das.Battlefield;
import das.Node;
import das.Node_RMI;
import das.server.Server;
import das.server.StateCommand;

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

	public Battlefield getBf() {
		return bf;
	}

	public void setBf(Battlefield bf) {
		this.bf = bf;
	}

	public Queue<StateCommand> getInbox() {
		return inbox;
	}

	public void setInbox(Queue<StateCommand> inbox) {
		this.inbox = inbox;
	}

	public long getTime() {
		return time;
	}

	public void setTime(long time) {
		this.time = time;
	}
	
	public List<String> getLog() {
		return log;
	}
	
	public void setLog(List<String> log) {
		this.log = log;
	}
	

}
