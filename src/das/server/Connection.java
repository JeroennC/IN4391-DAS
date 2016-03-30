package das.server;

import java.util.LinkedList;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Queue;

import das.message.Address;
import das.message.Message;


public abstract class Connection {
	private int lastMessageSentID;
	private Address address;
	private Queue<Message> sentMessages;
	private static final int maxQueueSize = 20;
	
	public Connection(Address a) {
		this.address = a;
		this.lastMessageSentID = 0;
		this.sentMessages = new PriorityQueue<Message>(1, (Message m1, Message m2) -> (int) (m1.getID() - m2.getID()));
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
	
	public void addMessage(Message m) {
		while(sentMessages.size() >= maxQueueSize)
			sentMessages.poll();
		sentMessages.add(m);
	}
	
	public Message getSentMessage(int id) {
		for(Message m: sentMessages) {
			if(m.getID() == id)
				return m;
		}
		return null;
	}
}
