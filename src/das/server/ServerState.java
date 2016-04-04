package das.server;

import java.io.Serializable;
import java.util.Comparator;
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
	private long nextCommandNr;
	private Queue<StateCommand> inbox;
	private volatile State state;
	private enum State {Start, Running, Inconsistent, Exit};
	private Thread runningThread;
	private static Comparator<StateCommand> comparator = (Comparator<StateCommand> & Serializable)((StateCommand m1, StateCommand m2) -> (int) (m1.getTimestamp() - m2.getTimestamp()));
	private long lastExecutedTime;
	
	public ServerState(Server server, long delay) {
		super();
		this.server = server;
		this.delay = delay;
		bf = new Battlefield();
		this.state = State.Start;
		nextCommandNr = 0;
		lastExecutedTime = 0;
		inbox = new PriorityQueue<StateCommand>(1, comparator);
	}
	
	public void init(Battlefield battlefield) {
		// Deep object copy, new clone of battlefield
		this.bf = battlefield.clone();
	}
	
	@Override
	public void run() {
		runningThread = Thread.currentThread();
		state = State.Running;
		boolean needsRollback;
		boolean generateRollback = true;
		if(delay == 0) return;
		while(server.isRunning()) {
			Thread.interrupted();
			StateCommand firstCommand = null;
			needsRollback = false;
			synchronized(inbox) {
				firstCommand = inbox.peek();
				// Generate rollback
				if (generateRollback && delay == 1000 && firstCommand != null) {
					if (nextCommandNr == 10) {
						StateCommand backup = inbox.poll();
						firstCommand = inbox.peek();
						inbox.add(backup);
					}
				}
			}
			if(firstCommand == null) {
				try { Thread.sleep(1000); } catch (InterruptedException e) { continue; }
			} else if(firstCommand.getTimestamp() <= getTime()) {
				if (!firstCommand.isValid()) {
					synchronized(inbox) {
						inbox.remove(firstCommand);
						//TODO add to other list
					}
					continue;
				}
				// Execute command
				synchronized(bf) {
					if(deliver(firstCommand) != null) {
						// Stamp command nr
						firstCommand.setCommandNr(nextCommandNr++);
						lastExecutedTime = Math.max(firstCommand.getTimestamp(), lastExecutedTime);
						// Check if inconsistent with already executed state
						if(!firstCommand.isConsistent()) {
							Print("Inconsistent! " + firstCommand.getCommandNr()
									+ " <> " + firstCommand.getCommands()[firstCommand.getPosition() - 1].getCommandNr());
							needsRollback = true;
						}
					} else {
						needsRollback = true;
					}
				}
				synchronized(inbox) {
					inbox.remove(firstCommand);
					//TODO add to other list
				}
				if (needsRollback) {
					generateRollback = false;
					// Rollback previous state
					Print("Doing a rollback!");
					fasterState.rollback();
				}
			} else {
				try { Thread.sleep(Math.max(1, firstCommand.getTimestamp() - getTime())); } catch (InterruptedException e) { continue; }
			}	
		}
	}
	
	public Data deliver(StateCommand sc) {
		Action a = sc.getMessage().getAction();
		// Check if action is possible, if not, rollback previous state, and invalidate the commands for later states
		if (!isPossible(a)) {
			sc.InvalidateUpwards();
			Print("Invalid action: " + a.toString());
			return null;
		}
		boolean newPlayer = (a instanceof NewPlayer && ((NewPlayer) a).getNewUnit() == null);
		Data d = new Data();
		Unit u = bf.doAction(a);
		if(a instanceof Heal) 
			d.updateUnit(bf.getUnit(((Heal) a).getReceiverId()));
		else if (a instanceof Hit) {
			if(bf.getUnit(((Hit) a).getReceiverId()) == null)
				d.deleteUnit(((Hit) a).getReceiverId());
			else
				d.updateUnit(bf.getUnit(((Hit) a).getReceiverId()));
			
		} else
			d.updateUnit(u);
		if(newPlayer) {
			d.setUpdatedUnits(bf.getUnitList());
			d.setPlayer(u);
		}
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
		Data d = null;
		synchronized (inbox) {
			synchronized (bf) {
				if(fasterState == null)
					d = slowerState.bf.difference(bf);
				bf = slowerState.cloneBattlefield();
				inbox = slowerState.cloneInbox();
				nextCommandNr = slowerState.nextCommandNr;
				// TODO: Make this neater, For now, all synchronized
				StateCommand firstCommand = inbox.peek();
				int executed = 0;
				// Get up to date
				while(firstCommand != null) {
					Thread.interrupted();
					firstCommand = null;
					firstCommand = inbox.peek();
					if(firstCommand == null) {
						break;
					} else if(firstCommand.getTimestamp() <= getTime()) {
						if (!firstCommand.isValid()) {
							inbox.remove(firstCommand);
							//TODO add to other list
							continue;
						}
						// Execute command
						if(deliver(firstCommand) != null) {
							executed++;
							lastExecutedTime = Math.max(firstCommand.getTimestamp(), lastExecutedTime);
							// Stamp command nr
							firstCommand.setCommandNr(nextCommandNr++);
						} 
						inbox.remove(firstCommand);
					} else {
						break;
					}	
				}
				Print("And we're back, executed: " + executed);
			}
		}
		
		runningThread.interrupt();
		
		// Cascade rollback, otherwise inconsistencies are not found (because the original inconsistency is not processed again)
		if (fasterState != null) 
			fasterState.rollback();
		else if(d != null)
			server.sendRollback(d);
	}
	
	public long getTime() {
		return server.getTime() - delay;
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

	public Data receive(StateCommand sc) {
		if (slowerState == null && sc.getTimestamp() < lastExecutedTime) {
			// Last trailing state is possibly inconsistent
			// TODO handle it
			Print("I am inconsistent :(");
		}
		if(delay > 0) {
			synchronized(inbox) {		
				if(!inbox.contains(sc))
					inbox.add(sc);
			}
			runningThread.interrupt();
			return null;
		} else {
			Data d;
			synchronized(bf) {
				d = deliver(sc);
				lastExecutedTime = Math.max(sc.getTimestamp(), lastExecutedTime);
				if (d != null)
					sc.setCommandNr(nextCommandNr++);
			}
			return d;
		}
	}
	
	public List<Unit> getUnitList() {
		return bf.getUnitList();
	}
	
	public int getNextUnitId() {
		return bf.getNextUnitId();
	}
	
	public long getNextCommandNr() {
		return nextCommandNr;
	}
	
	public synchronized Queue<StateCommand> cloneInbox() {
		Queue<StateCommand> result = new PriorityQueue<StateCommand>(1, comparator);
		if (fasterState == null) return result;
		
		StateCommand c_mine;
		Iterator<StateCommand> it = inbox.iterator();
		while(it.hasNext()) {
			c_mine = it.next();
			result.add(c_mine.getPreviousCommand().reset());
		}
		
		return result;
	}
	
	public Battlefield cloneBattlefield() {
		synchronized(bf) {
			return bf.clone();
		}
	}
	
	public Data getData() {
		synchronized (bf) {
			return bf.getData();
		}
	}
	
	public void Print(String print) {
		server.Print(print + " / "+delay);
	}
}
