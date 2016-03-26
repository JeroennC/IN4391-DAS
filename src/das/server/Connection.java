package das.server;

import java.util.LinkedList;
import java.util.List;

import das.message.Address;


public abstract class Connection {
	private int lastMessageSentID;
	private Address address;
	
	public Connection(Address a) {
		this.address = a;
		this.lastMessageSentID = 0;
	}
	
	public static Connection createConnection(String id, Address a) {
		if(id.startsWith("Client"))
			return new ClientConnection(a);
		else
			return new ServerConnection(a);
	}
	
	public int getLastMessageSentID() {
		return lastMessageSentID;
	}
	
	public void setLastMessageSentID(int lastMessageSentID) {
		this.lastMessageSentID = lastMessageSentID;
	}
	
	public Address getAddress() {
		return address;
	}
	
	public void setAddress(Address address) {
		this.address = address;
	}
	
	public List<Integer> getAndResetAcks() {
		return new LinkedList<Integer>();
	}
}
