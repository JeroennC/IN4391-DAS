package das.message;

import das.Node;
import das.Node_RMI;
import das.server.Server;

public class InitServerMessage extends Message {

	public InitServerMessage(Node from, Address to, String to_id) {
		super(from, to, to_id);
	}

	@Override
	public void receive(Node_RMI node) {
		((Server) node).receiveInitServerMessage(this);
	}

}
