package das.message;

import das.Client;
import das.Node_RMI;
import das.server.Server;

/**
 * Message for requesting an initialization of the game state, from Client to Server.
 */
public class InitMessage extends Message {
	private static final long serialVersionUID = -2554610299918794996L;
	private int playerId;
	
	public InitMessage(Client from, Address to, int to_id, int playerId ) {
		super(from, to, to_id);
		this.setPlayerId(playerId);
	}

	@Override
	public void receive(Node_RMI node) {
		((Server) node).receiveInitMessage(this);
	}

	public synchronized int getPlayerId() {
		return playerId;
	}

	public synchronized void setPlayerId(int playerId) {
		this.playerId = playerId;
	}

}
