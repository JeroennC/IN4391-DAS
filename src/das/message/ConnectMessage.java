package das.message;
import das.Client;
import das.Node_RMI;
import das.server.Server;

/**
 * Message for Clients requesting to connect to a Server
 */
public class ConnectMessage extends Message {
	private static final long serialVersionUID = 5205567105382587637L;

	public ConnectMessage(Client from, Address to, int to_id) {
		super(from, to, to_id);
	}

	@Override
	public void receive(Node_RMI node) {
		((Server) node).receiveConnectMessage(this);

	}

}
