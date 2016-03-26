package das.message;

import server.Server;
import das.Node;
import das.Node_RMI;

public class InitServerMessage extends Message {

	public InitServerMessage(Node from, Address to, String to_id) {
		super(from, to, to_id);
	}

	@Override
	public void receive(Node_RMI node) {
		((Server) node).receiveInitServerMessage(this);
	}

}
