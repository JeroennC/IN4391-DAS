package das.server;

import das.message.Address;

public class ClientConnection extends Connection {
	private int lastDataMessageSentID;
	private boolean _canMove;
	
	public ClientConnection(Address a) {
		super(a);
		this.setLastDataMessageSentID(0);
		_canMove = true; 
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

	public boolean canMove() {
		return _canMove;
	}

	public void canMove(boolean _canMove) {
		this._canMove = _canMove;
	}

	
}
