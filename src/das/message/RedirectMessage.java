package das.message;

import das.Client;
import das.Node_RMI;

public class RedirectMessage extends Message {
	private static final long serialVersionUID = 6021654018838732614L;
	private int server_id;
	
	public RedirectMessage(Client from, int to_id, int server_id) {
		super(from, to_id);
		this.setServer_id(server_id);
	}

	@Override
	public void receive(Node_RMI node) {
		((Client) node).receiveRedirectMessage(this);

	}

	public int getServer_id() {
		return server_id;
	}

	public void setServer_id(int server_id) {
		this.server_id = server_id;
	}

}
