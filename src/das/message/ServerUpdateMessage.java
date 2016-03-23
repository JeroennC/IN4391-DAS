package das.message;

import das.Node_RMI;
import das.Server;

public class ServerUpdateMessage extends Message {
	private static final long serialVersionUID = 8624974370403036157L;
	
	private int serverLoad;

	public ServerUpdateMessage(Server from, String to_id) {
		super(from, to_id);
	}

	@Override
	public void receive(Node_RMI node) {
		((Server) node).receiveServerUpdateMessage(this);
	}

	public int getServerLoad() {
		return serverLoad;
	}

	public void setServerLoad(int serverLoad) {
		this.serverLoad = serverLoad;
	}

}
