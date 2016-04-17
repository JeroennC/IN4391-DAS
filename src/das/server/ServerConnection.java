package das.server;

import java.util.LinkedList;
import java.util.List;

import das.message.Address;

/**
 * Extends the base Connection class with Server-specific information
 */
public class ServerConnection extends Connection {
	private static final int RECV_SIZE = 300;
	
	private List<Integer> acks;
	private LinkedList<Integer> receivedMessageIds;
	private int serverLoad;
	private long lastServerStatusTime;
	
	public ServerConnection(Address a, long lastConnectionTime) {
		super(a);
		this.setAcks(new LinkedList<Integer>());
		this.setReceivedMessageIds(new LinkedList<Integer>());
		this.setServerLoad(0);
		this.setLastConnectionTime(lastConnectionTime);
		this.setLastConnectionTime(lastConnectionTime);
	}
	
	public void addAck(int a) {
		synchronized(acks) {
			acks.add(a);
		}
	}
	
	public void resetAndAddAck(int a) {
		synchronized(acks) {
			acks.clear();
			acks.add(a);
		}
	}
	
	@Override
	public List<Integer> getAndResetAcks() {
		List<Integer> a;
		synchronized(acks) {
			a = new LinkedList<Integer>(acks);
			acks.clear();
		}
		return a;
	}

	public List<Integer> getAcks() {
		return acks;
	}

	public void setAcks(List<Integer> acks) {
		this.acks = acks;
	}

	public int getServerLoad() {
		return serverLoad;
	}

	public void setServerLoad(int serverLoad) {
		this.serverLoad = serverLoad;
	}

	public long getLastServerStatusTime() {
		return lastServerStatusTime;
	}

	public void setLastServerStatusTime(long lastServerStatusTime) {
		this.lastServerStatusTime = lastServerStatusTime;
	}

	public List<Integer> getReceivedMessageIds() {
		return receivedMessageIds;
	}

	public void setReceivedMessageIds(LinkedList<Integer> receivedMessageIds) {
		this.receivedMessageIds = receivedMessageIds;
	}
	
	public void addReceivedMessageId(int message_id) {
		if (receivedMessageIds.size() > RECV_SIZE)
			receivedMessageIds.removeFirst();
		this.receivedMessageIds.add(message_id);
	}
	
	public boolean hasMessageId(int message_id) {
		return receivedMessageIds.contains(message_id);
	}
}
