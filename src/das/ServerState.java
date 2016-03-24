package das;

import java.util.List;

import das.action.Action;
import das.message.Message;

public class ServerState implements Runnable {
	private Server server;
	private Battlefield bf;
	private final long delay;
	private ServerState fasterState = null;
	private ServerState slowerState = null;
	private List<Message> inbox;
	
	public ServerState(Server server, long delay) {
		super();
		this.server = server;
		this.delay = delay;
		bf = new Battlefield();
	}
	
	public void init(Battlefield battlefield) {
		//TODO copy battlefield (not reference copy, but deep object copy)
	}
	
	@Override
	public void run() {
		// TODO Auto-generated method stub
		
	}
	
	public boolean isPossible(Action action) {
		return bf.isActionAllowed(action);
	}
	
	public void rollback() {}
	
	public long getTime() {
		return System.currentTimeMillis() + server.getDeltaTime() + delay;
	}

	public ServerState getFasterState() {
		return fasterState;
	}

	public void setFasterState(ServerState fasterState) {
		this.fasterState = fasterState;
	}

	public ServerState getSlowerState() {
		return slowerState;
	}

	public void setSlowerState(ServerState slowerState) {
		this.slowerState = slowerState;
	}

	public void deliver(Message m) {
		// TODO Auto-generated method stub
		inbox.add(m);
	}
}
