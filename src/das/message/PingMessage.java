package das.message;
import das.Client;
import das.Node;
import das.Node_RMI;
import das.Server;


public class PingMessage extends Message {
	private static final long serialVersionUID = -9060493535028320273L;

	public PingMessage(Node from, Address to,  String to_id) {
		super(from, to, to_id);
	}
	
	public PingMessage(Client from, Address to, int to_id) {
		super(from, to, to_id);
	}

	@Override
	public void receive(Node_RMI node) {
		if (node instanceof Server) {
			((Server)node).receivePingMessage(this);
		}
	}

}
