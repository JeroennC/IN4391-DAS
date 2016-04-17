package das.server;

import das.message.Address;

/**
 * Extends the base Connection class with Client-specific information
 */
public class ClientConnection extends Connection {
	private int lastDataMessageSentID;
	private boolean _canMove;
	private int unitId;
	private int invalidMoves;
	
	public ClientConnection(Address a) {
		super(a);
		this.setLastDataMessageSentID(0);
		_canMove = true; 
		setInvalidMoves(0);
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

	public int getUnitId() {
		return unitId;
	}

	public void setUnitId(int unitId) {
		this.unitId = unitId;
	}

	public int getInvalidMoves() {
		return invalidMoves;
	}

	public void setInvalidMoves(int invalidMoves) {
		this.invalidMoves = invalidMoves;
	}
	
	public void incrementInvalidMoves() {
		this.invalidMoves++;
	}

	
}
