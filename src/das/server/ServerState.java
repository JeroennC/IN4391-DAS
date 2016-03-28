package das.server;

import java.util.Iterator;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Queue;

import das.Battlefield;
import das.Unit;
import das.action.Action;
import das.action.Heal;
import das.action.Hit;
import das.action.Move;
import das.action.NewPlayer;
import das.message.Data;

public class ServerState implements Runnable {
	private Server server;
	private Battlefield bf;
	private final long delay;
	private ServerState fasterState = null;
	private ServerState slowerState = null;
	private Queue<StateCommand> inbox;
	private volatile State state;
	private enum State {Start, Running, Inconsistent, Exit};
	private Thread runningThread;
	
	public ServerState(Server server, long delay) {
		super();
		this.server = server;
		this.delay = delay;
		bf = new Battlefield();
		this.state = State.Start;
		inbox = new PriorityQueue<StateCommand>(1, (StateCommand m1, StateCommand m2) -> (int) (m1.getTimestamp() - m2.getTimestamp()));
	}
	
	public void init(Battlefield battlefield) {
		// Deep object copy, new clone of battlefield
		this.bf = battlefield.clone();
	}
	
	@Override
	public void run() {
		runningThread = Thread.currentThread();
		state = State.Running;
		while(server.isRunning()) {
			Thread.interrupted();
			StateCommand firstCommand = null;
			synchronized(inbox) {
				firstCommand = inbox.peek();
			}
			if(firstCommand == null) {
				try { Thread.sleep(1000); } catch (InterruptedException e) { continue; }
			} else if(firstCommand.getTimestamp() <= getTime() && firstCommand.isValid() ) {
				// Execute command
				deliver(firstCommand);
				// Check if inconsistent with already executed state
				if(!firstCommand.isConsistent()) {
					// Rollback previous state
					fasterState.rollback();
				}
				synchronized(inbox) {
					inbox.remove(firstCommand);
					//TODO add to other list
				}
			} else {
				try { Thread.sleep(firstCommand.getTimestamp() - getTime()); } catch (InterruptedException e) { continue; }
			}	
		}			
	}
	
	public Data deliver(StateCommand sc) {
		Action a = sc.getMessage().getAction();
		// Check if action is possible, if not, rollback previous state, and invalidate the commands for later states
		if (!isPossible(a)) {
			sc.InvalidateUpwards();
			fasterState.rollback();
			return null;
		}
		boolean newPlayer = (a instanceof NewPlayer && ((NewPlayer) a).getNewUnit() == null);
		Data d = new Data();
		Unit u = bf.doAction(a);
		if(a instanceof Heal) 
			d.updateUnit(bf.getUnit(((Heal) a).getReceiverId()));
		else if (a instanceof Hit) 
			d.updateUnit(bf.getUnit(((Hit) a).getReceiverId()));
		else
			d.updateUnit(u);
		if(newPlayer)
			d.setPlayer(u);
		return d;
	}
	
	public boolean isPossible(Action action) {
		return bf.isActionAllowed(action);
	}
	
	/**
	 * Take the state + inbox from the faster state
	 */
	public void rollback() {
		// Care, always acquire locks in this order, no deadlocks :)
		synchronized (inbox) {
			synchronized (bf) {
				bf = slowerState.bf.clone();
				inbox = slowerState.cloneInbox();
			}
		}
	}
	
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

	public Data receive(StateCommand m) {
		if(delay > 0) {
			synchronized(inbox) {		
				inbox.add(m);
			}
			runningThread.interrupt();
			return null;
		} else
			return deliver(m);
	}
	
	public List<Unit> getUnitList() {
		return bf.getUnitList();
	}
	
	public int getNextUnitId() {
		return bf.getNextUnitId();
	}
	
	public synchronized Queue<StateCommand> cloneInbox() {
		Queue<StateCommand> result = new PriorityQueue<StateCommand>(1, (StateCommand m1, StateCommand m2) -> (int) (m1.getTimestamp() - m2.getTimestamp()));
		if (fasterState == null) return result;
		
		StateCommand c_mine, c_new;
		Iterator<StateCommand> it = inbox.iterator();
		while(it.hasNext()) {
			c_mine = it.next();
			c_new = new StateCommand(c_mine.getCommands(), c_mine.getPosition() - 1, c_mine.getMessage());
			// Update references for these two serverstates, this is fine? 
			// Should be as rollback only goes down, important is that the refreshed inbox has references to faster states 
			c_mine.getCommands()[c_new.getPosition()] = c_new;
			c_new .getCommands()[c_new.getPosition()] = c_new;
		}
		
		return result;
	}
}
