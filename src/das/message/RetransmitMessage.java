package das.message;
import das.Client;
import das.Node_RMI;
import das.server.Server;


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
	public String toString() {
		return "RetransmitMessage ("+getFrom_id()+"->"+getReceiver_id()+") [ "+firstMessage_id + ", "+lastMessage_id + "]";
	}

}
