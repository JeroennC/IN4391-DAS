package das.message;
import das.Client;
import das.Node_RMI;
import das.server.Server;

/**
 * Message for sending a Pulse update from Servers to Clients
 */
public class PulseMessage extends Message {
	private static final long serialVersionUID = 7233974155175482868L;

	public PulseMessage(Server from, Address to, String to_id) {
		super(from, to, to_id);
	}

	@Override
	public void receive(Node_RMI node) {
		((Client) node).receivePulse(this);
	}

}
