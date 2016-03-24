package das;

import java.util.List;
import java.util.PriorityQueue;
import java.util.Queue;


import das.action.Action;
import das.message.ActionMessage;

public class ServerState implements Runnable {
	private Server server;
	private Battlefield bf;
	private final long delay;
	private ServerState fasterState = null;
	private ServerState slowerState = null;
	private Queue<ActionMessage> inbox;
	private volatile State state;
	private enum State {Start, Running, Inconsistent, Exit};
	private Thread runningThread;
	
	public ServerState(Server server, long delay) {
		super();
		this.server = server;
		this.delay = delay;
		bf = new Battlefield();
		this.state = State.Start;
		inbox = new PriorityQueue<ActionMessage>(1, (ActionMessage m1, ActionMessage m2) -> (int) (m1.getTimestamp() - m2.getTimestamp()));
	}
	
	public void init(Battlefield battlefield) {
		// Deep object copy, new clone of battlefield
		this.bf = battlefield.clone();
	}
	
	@Override
	public void run() {
		runningThread = Thread.currentThread();
		state = State.Running;
		while(state != State.Exit) {
			Thread.interrupted();
			ActionMessage firstMessage = null;
			synchronized(inbox) {
				firstMessage = inbox.peek();
			}
			if(firstMessage == null) {
				try { Thread.sleep(1000); } catch (InterruptedException e) { continue; }
			} else if(firstMessage.getTimestamp() <= getTime() ) {
				deliver(firstMessage);
				synchronized(inbox) {
					inbox.remove(firstMessage);
					//TODO add to other list
				}
			} else {
				try { Thread.sleep(firstMessage.getTimestamp() - getTime()); } catch (InterruptedException e) { continue; }
			}	
		}			
	}
	
	public void deliver(ActionMessage m) {
		
	}
	
	public boolean isPossible(Action action) {
		return bf.isActionAllowed(action);
	}
	
	public boolean isConsistent(StateCommand cmd) {
		// TODO could be more sophisticated, is now very strict
		return cmd.getCommandNr() == cmd.getPreviousCommand().getCommandNr();
	}
	
	public void rollback() {}
	
	public long getTime() {
		return server.getTime() + delay;
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

	public void receive(ActionMessage m) {
		synchronized(inbox) {		
			inbox.add(m);
		}
		runningThread.interrupt();
	}
	
	public List<Unit> getUnitList() {
		return bf.getUnitList();
	}
	
	public int getNextUnitId() {
		return bf.getNextUnitId();
	}
}
