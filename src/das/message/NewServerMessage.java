package das.message;

import das.Node_RMI;
import das.server.Server;

/**
 * Message for a server joining the server pool
 */
public class NewServerMessage extends Message {
	private static final long serialVersionUID = -4587682238478282956L;

	public NewServerMessage(Server from, Address to, String to_id) {
		super(from, to, to_id);
	}

	@Override
	public void receive(Node_RMI node) {
		((Server) node).receiveNewServerMessage(this);
	}

}
