package das.message;
import das.Client;
import das.Node_RMI;


public class RetransmitMessage extends Message {
	private static final long serialVersionUID = 345879987762306922L;
	
	private int firstMessage_id;
	private int lastMessage_id;
	
	public RetransmitMessage(Client from, int to_id, int firstMessage_id, int lastMessage_id) {
		super(from, to_id);
		this.firstMessage_id = firstMessage_id;
		this.lastMessage_id = lastMessage_id;
	}

	@Override
	public void receive(Node_RMI node) {
		// TODO Auto-generated method stub

	}

}
