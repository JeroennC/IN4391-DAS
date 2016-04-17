package das.message;
import das.Client;
import das.Node_RMI;
import das.server.Server;

/**
 * Message for requesting a retransmission of a message, from Client to Server
 */
public class RetransmitMessage extends Message {
	private static final long serialVersionUID = 345879987762306922L;
	
	private int firstMessage_id;
	private int lastMessage_id;
	
	public RetransmitMessage(Client from, Address to, int to_id, int firstMessage_id, int lastMessage_id) {
		super(from, to, to_id);
		this.firstMessage_id = firstMessage_id;
		this.lastMessage_id = lastMessage_id;
	}

	@Override
	public void receive(Node_RMI node) {
		((Server) node).receiveRetransmitMessage(this);
	}
	
	@Override
	public synchronized String toString() {
		return "RetransmitMessage ("+getFrom_id()+"->"+getReceiver_id()+") [ "+firstMessage_id + ", "+lastMessage_id + "]";
	}
	
	public synchronized int getFirstMessage_id() {
		return firstMessage_id;
	}

	public synchronized void setFirstMessage_id(int firstMessage_id) {
		this.firstMessage_id = firstMessage_id;
	}

	public synchronized int getLastMessage_id() {
		return lastMessage_id;
	}

	public synchronized void setLastMessage_id(int lastMessage_id) {
		this.lastMessage_id = lastMessage_id;
	}

}
