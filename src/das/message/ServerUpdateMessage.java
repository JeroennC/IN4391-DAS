package das.message;

import das.Node_RMI;
import das.server.Server;

public class ServerUpdateMessage extends Message {
	private static final long serialVersionUID = 8624974370403036157L;
	
	private int serverLoad;

	public ServerUpdateMessage(Server from, Address to, String to_id, int serverLoad) {
		super(from, to, to_id);
		this.setServerLoad(serverLoad);
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
