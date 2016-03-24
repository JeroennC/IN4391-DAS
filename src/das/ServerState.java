package das;

import java.util.PriorityQueue;
import java.util.Queue;

import das.message.Message;

public class ServerState implements Runnable {
	private Server server;
	private Battlefield bf;
	private final long delay;
	private ServerState fasterState = null;
	private ServerState slowerState = null;
	private Queue<Message> inbox;
	private volatile State state;
	private enum State {Start, Running, Inconsistent, Exit};
	private Thread runningThread;
	
	public ServerState(Server server, long delay) {
		super();
		this.server = server;
		this.delay = delay;
		bf = new Battlefield();
		this.state = State.Start;
		inbox = new PriorityQueue<Message>(0, (Message m1, Message m2) -> (int) (m1.getTimestamp() - m2.getTimestamp()));
	}
	
	public void init(Battlefield battlefield) {
		//TODO copy battlefield (not reference copy, but deep object copy)
	}
	
	@Override
	public void run() {
		runningThread = Thread.currentThread();
		state = State.Running;
		while(state != State.Exit) {
			Thread.interrupted();
			Message firstMessage = null;
			synchronized(inbox) {
				firstMessage = inbox.peek();
			}
			if(firstMessage == null) {
				try { Thread.sleep(1000); } catch (InterruptedException e) { continue; }
			} else if(firstMessage.getTimestamp() >= getTime() ) {
				deliver(firstMessage);
				synchronized(inbox) {
					inbox.remove(firstMessage);
				}
			} else {
				try { Thread.sleep(firstMessage.getTimestamp() - getTime()); } catch (InterruptedException e) { continue; }
			}			
		}			
	}
	
	public void deliver(Message m) {
		
	}
	
	public boolean isPossible(Action action) {
		//TODO Auto-generated method stub
		return false;
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

	public void receive(Message m) {
		synchronized(inbox) {		
			inbox.add(m);
		}
		runningThread.interrupt();
	}
}
