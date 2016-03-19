package das.message;
import das.Client;
import das.Node_RMI;
import das.Server;


public class PulseMessage extends Message {
	private static final long serialVersionUID = 7233974155175482868L;

	public PulseMessage(Server from, String to_id) {
		super(from, to_id);
	}

	@Override
	public void receive(Node_RMI node) {
		((Client) node).receivePulse(this);
	}

}
