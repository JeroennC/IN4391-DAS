package das.message;

import das.Client;
import das.Node_RMI;
import das.Server;

public class InitMessage extends Message {
	private static final long serialVersionUID = -2554610299918794996L;

	public InitMessage(Client from, int to_id ) {
		super(from, to_id);
	}

	@Override
	public void receive(Node_RMI node) {
		((Server) node).receiveInitMessage(this);
	}

}
