package das.message;
import das.Client;
import das.Node_RMI;


public class PingMessage extends Message {
	private static final long serialVersionUID = -9060493535028320273L;

	public PingMessage(Client from, int to_id) {
		super(from, to_id);
	}

	@Override
	public void receive(Node_RMI node) {
		// TODO Auto-generated method stub

	}

}
