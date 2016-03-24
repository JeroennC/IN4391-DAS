package das;

import das.message.Address;
import das.message.Message;

public class InitServerMessage extends Message {

	public InitServerMessage(Node from, Address to, String to_id) {
		super(from, to, to_id);
	}

	@Override
	public void receive(Node_RMI node) {
		((Server) node).receiveInitServerMessage(this);
	}

}
