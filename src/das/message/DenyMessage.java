package das.message;
import das.Client;
import das.Node_RMI;
import das.server.Server;


public class DenyMessage extends Message {
	private static final long serialVersionUID = 2271377239141868250L;
	private int deniedMessage_id;

	public DenyMessage(Server from, Address to,  String to_id, int deniedMessage_id) {
		super(from, to, to_id);
		this.setDeniedMessage_id(deniedMessage_id);
	}

	@Override
	public void receive(Node_RMI node) {
		((Client) node).receiveDenial(this);

	}

	public int getDeniedMessage_id() {
		return deniedMessage_id;
	}

	public void setDeniedMessage_id(int deniedMessage_id) {
		this.deniedMessage_id = deniedMessage_id;
	}

}
