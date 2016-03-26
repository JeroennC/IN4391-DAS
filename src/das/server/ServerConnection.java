package das.server;

import java.util.LinkedList;
import java.util.List;

import das.message.Address;

public class ServerConnection extends Connection {
	private List<Integer> acks;
	private int serverLoad;
	
	public ServerConnection(Address a) {
		super(a);
		this.setAcks(new LinkedList<Integer>());
		this.setServerLoad(0);
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
}
