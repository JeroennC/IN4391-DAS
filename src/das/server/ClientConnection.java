package das.server;

import das.message.Address;

public class ClientConnection extends Connection {
	private int lastDataMessageSentID;
	
	public ClientConnection(Address a) {
		super(a);
		this.setLastDataMessageSentID(0);
	}

	public int incrementLastDataMessageSentID() {
		return lastDataMessageSentID++;
	}
	
	public int getLastDataMessageSentID() {
		return lastDataMessageSentID;
	}

	public void setLastDataMessageSentID(int lastDataMessageSentID) {
		this.lastDataMessageSentID = lastDataMessageSentID;
	}

	
}
