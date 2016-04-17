package das.message;

import das.Node;
import das.Node_RMI;
import das.server.Server;

/**
 * Message for requesting an initialization of the game state, from Server to Server.
 */
public class InitServerMessage extends Message {
	private static final long serialVersionUID = 1521758817083440868L;

	public InitServerMessage(Node from, Address to, String to_id) {
		super(from, to, to_id);
	}

	@Override
	public void receive(Node_RMI node) {
		((Server) node).receiveInitServerMessage(this);
	}

}
