package das;

import java.util.List;

import das.message.Message;

public class ServerState implements Runnable {
	private Server server;
	private Battlefield bf;
	private final long delay;
	private ServerState fasterState;
	private ServerState slowerState;
	private List<Message> inbox;
	
	public ServerState(Server server, long delay, ServerState fasterState,
			ServerState slowerState) {
		super();
		this.server = server;
		this.delay = delay;
		this.fasterState = fasterState;
		this.slowerState = slowerState;
		bf = new Battlefield();
	}
	
	public void init(Battlefield battlefield) {
		//TODO copy battlefield (not reference copy, but deep object copy)
	}
	
	@Override
	public void run() {
		// TODO Auto-generated method stub
		
	}
	
	public void rollback() {}
	
	public long getTime() {
		return System.currentTimeMillis() + server.getDeltaTime() + delay;
	}
}
