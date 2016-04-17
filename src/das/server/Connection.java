package das.server;

import java.util.LinkedList;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Queue;

import das.message.Address;
import das.message.Message;

/**
 * Base connection class, contains basic information on a connection to another node 
 *
 */
public abstract class Connection {
	private int lastMessageSentID;
	private Address address;
	private Queue<Message> sentMessages;
	private long lastConnectionTime;
	private static final int maxQueueSize = 20;
	
	public Connection(Address a) {
		this.address = a;
		this.lastMessageSentID = 0;
		this.sentMessages = new PriorityQueue<Message>(1, (Message m1, Message m2) -> (int) (m1.getID() - m2.getID()));
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
	
	/**
	 * Adds Message m to the sent message buffer
	 */
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
	
	public long getLastConnectionTime() {
		return lastConnectionTime;
	}

	public void setLastConnectionTime(long lastConnectionTime) {
		this.lastConnectionTime = lastConnectionTime;
	}
}
