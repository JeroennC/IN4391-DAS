package das;
import java.rmi.RemoteException;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import das.action.Action;
import das.action.Heal;
import das.action.Hit;
import das.action.Move;
import das.action.MoveType;
import das.message.ActionMessage;
import das.message.Address;
import das.message.ConnectMessage;
import das.message.DataMessage;
import das.message.DenyMessage;
import das.message.InitMessage;
import das.message.Message;
import das.message.PingMessage;
import das.message.PulseMessage;
import das.message.RedirectMessage;
import das.message.RefreshMessage;
import das.message.RetransmitMessage;
import das.server.Server;

public class Client extends Node {
	private static final long serialVersionUID = 8743582021067062104L;
	
	private static final long EXPIRATION_TIME = 10 * 1000L; // 10 seconds
	private int proposed_server_id;
	private volatile int server_id;
	private volatile Address serverAddress;
	private volatile boolean _canMove;
	private volatile Thread pulseTimer;
	private List<Message> sentMessages;
	private volatile List<DataMessage> dataMessageBuffer;
	//private List<ActionMessage> sentActionMessages;
	private ActionMessage lastActionMessage;
	private Object lastActionAccess;
	private int lastMessageSentID;
	private volatile int expectedDataMessageID;
	private volatile int expectedMessageID;
	private volatile List<Integer> receivedPastExpected;
	private volatile long lastTimestamp;
	private int retransmitRequested;
	private Map<ActionMessage, Long> actionTimeStamp;
	private long responseTime;
	
	private Battlefield bf;
	private volatile Unit player;
	
	public Client(int id) throws RemoteException {
		super(id, "Client_"+id);
		this.proposed_server_id = -1;
		bf = new Battlefield();
		actionTimeStamp = new HashMap<ActionMessage, Long>();
		responseTime = -2;
		pulseTimer = (new Thread() {
			public void run() {
				while(state != Client.State.Exit) {
					try { Thread.sleep(2 * Server.PULSE); } catch (InterruptedException e) { continue; }
					if(!interrupted()) sendMessage(new PingMessage(Client.this, serverAddress, server_id));
					else continue;
					try { Thread.sleep(3 * Server.PULSE); } catch (InterruptedException e) { continue; }
					if(!interrupted()) {
						Print("Pulse_time_out");
						connect();
					}
					else continue;
				}
			}
		});
		reset();
	}
	
	public Client(int id, int proposed_server_id) throws RemoteException {
		this(id);
		this.proposed_server_id = proposed_server_id;
	}
	
	/**
	 * Resets all client variables
	 */
	public synchronized void reset() {
		synchronized (pulseTimer) {
			if(pulseTimer != null && !pulseTimer.isAlive()) pulseTimer.interrupt();
		}
		_canMove = false;
		changeState(State.Disconnected);
		sentMessages = new LinkedList<Message>();
		dataMessageBuffer = new LinkedList<DataMessage>();
		lastActionMessage = null;
		lastActionAccess = new Object();
		expectedDataMessageID = 0;
		expectedMessageID = 0;
		lastMessageSentID = 0;
		receivedPastExpected = new LinkedList<Integer>();
		retransmitRequested = 0;
	}

	/**
	 * Starts client connection and main action loop
	 */
	@Override
	public void run() {
		//Sleep so that not all clients start at the same moment.
		//try {Thread.sleep((int) (Math.random() * 2000));} catch (InterruptedException e) {}
		pulseTimer.start();
		connect();
		
		int it = 0;
		// Start main loop
		while(state != State.Exit) {
			if(state == State.Disconnected) {
				//Do nothing
			} else if(state == State.Running && _canMove) {
				if (player != null) {
					Action a = player.isAlive() ? doMove() : null;
					if(a != null) {
						synchronized(lastActionAccess) {
							lastActionMessage = new ActionMessage(this, serverAddress, server_id, a, responseTime);
						}
						sendMessage(lastActionMessage);
					}
					if(!player.isAlive() && player.getTimestamp() < lastTimestamp - 2 * Server.PULSE)
						changeState(State.Exit);
				}
				if (it == 10) {
					it = 0;
					// Request old units
					List<Unit> requestUnits = new LinkedList<Unit>();
					for (Unit u : bf.getUnitList()) {
						if (u.getTimestamp() < lastTimestamp - EXPIRATION_TIME) {
							requestUnits.add(u);
						}
					}
					if (!requestUnits.isEmpty())
						sendMessage(new RefreshMessage(this, serverAddress, server_id, requestUnits));
				} else {
					it++;
				}
			} 
			try {Thread.sleep(100);} catch (InterruptedException e) {}
		}
		// Disconnect
		Print("Game is over.");
		stopPulseTimer();
		close();
		Main.removeThread(this.getName());
	}
	
	/**
	 * Performs a move if possible
	 */
	public Action doMove() {
		synchronized(bf) {
			_canMove = false;
			if(!bf.hasDragons() || !player.isAlive()) {
				// Game is over, change client state
				Print("Game over");
				changeState(State.Exit);
				return null;
			}
			List<Unit> nearUnits = bf.getSurroundingUnits(player);
			
			// Heal other player if near and hurt
			for (Unit u : nearUnits) {
				if (u.isType() && u.needsHealing()) {
					bf.healUnit(player, u);
					Print("Healed player " + u.getId());
					return new Heal(player.getId(), u.getId());
				}
			}
			// Attack dragon if it is near
			for (Unit u : nearUnits) {
				if (!u.isType()) {
					bf.attackUnit(player, u);
					Printf("Attacked %d, health left: %d", u.getId(), u.getHp());
					return new Hit(player.getId(), u.getId());
				}
			}
			// Move towards nearest dragon
			Unit dragon = bf.getClosestDragon(player);
			if(dragon == null)
				return null;
			int distX = dragon.getX() - player.getX();
			int distY = dragon.getY() - player.getY();
			MoveType m;
			if (Math.abs(distX) < Math.abs(distY)) {
				// Attempt move on x
				m = distX < 0 ? MoveType.Left : MoveType.Right;
				if (distX == 0 || !bf.moveUnit(player, m)) {
					// Otherwise just try on y
					m = distY < 0 ? MoveType.Down : MoveType.Up;
					bf.moveUnit(player, m);
				}
			} else {
				// Attempt move on y
				m = distY < 0 ? MoveType.Down : MoveType.Up;
				if (distY == 0 || !bf.moveUnit(player, m)) {
					// Otherwise just try on x
					m = distX < 0 ? MoveType.Left : MoveType.Right;
					bf.moveUnit(player, m);
				}
			}
			Printf("Moved to %s, now at x %d, y %d ", m.toString(), player.getX(), player.getY());
			return new Move(player.getId(), m);
		}
	}
	
	/**
	 * Connects to a random server
	 */
	public void connect() {
		reset();
		server_id = (int) (Math.random() * Main.ADDRESSES.size());
		// DEV:
		//server_id = 1;
		if (proposed_server_id != - 1) server_id = proposed_server_id;
		serverAddress = Main.ADDRESSES.get(server_id);
		Print("Try to connect with server "+(server_id));
		sendMessage(new ConnectMessage(this, serverAddress, server_id));
		resetPulseTimer();
	}

	/**
	 * Main receival of messages, sets overall message variables
	 */
	@Override
	public synchronized void receiveMessage(Message m) throws RemoteException {
		resetPulseTimer();
		if(!m.getFrom_id().equals("Server_"+server_id));
		if(m instanceof DataMessage) {
			DataMessage dm = (DataMessage) m;
			ActionMessage a = actionTimeStamp.keySet().parallelStream().filter( am -> am.getID() == dm.getActionMessage_id()).findFirst().orElse(null);
			if(a != null) {
				responseTime = (System.currentTimeMillis() - actionTimeStamp.get(a));
			}				
		}
		lastTimestamp = m.getTimestamp() > lastTimestamp ? m.getTimestamp() : lastTimestamp;
		if(((state == State.Disconnected || state == State.Initialization) && m.getID() > expectedMessageID + 20) || m.getID() > expectedMessageID + 30)
			return;
		if (m.getID() > expectedMessageID + 20) {
			Print("message_missed_overflow");
			connect();
			return;
		} else if (m.getID() > expectedMessageID) {
			receivedPastExpected.add(m.getID());
		} else if (m.getID() == expectedMessageID ){
			expectedMessageID++;
			Collections.sort(receivedPastExpected);
			while(!receivedPastExpected.isEmpty() && receivedPastExpected.get(0) <= expectedMessageID)
				expectedMessageID = Math.max(expectedMessageID, receivedPastExpected.remove(0)) + 1;
		} else if(!(m instanceof RedirectMessage))
			return;
		if(receivedPastExpected.contains(m.getID()))
			return;
		if (m.getID() > expectedMessageID + 5 * (retransmitRequested+1)) {
			//missed 5 messages, request these messages
			sendMessage(new RetransmitMessage(this, serverAddress, server_id, expectedMessageID, m.getID() - 1));
			retransmitRequested++;
		} else if(receivedPastExpected.isEmpty())
			retransmitRequested = 0;
		
		m.receive(this);		
	}
	
	/**
	 * Allow self to move
	 */
	public void receivePulse(PulseMessage m) {
		_canMove = true;		
	}
	
	/**
	 * Resets the pulse
	 */
	private void resetPulseTimer() {
		synchronized(pulseTimer) {
			if(pulseTimer != null && pulseTimer.isAlive()) pulseTimer.interrupt();
		}
	}
	
	/**
	 * Stops the pulse
	 */
	private void stopPulseTimer() {
		synchronized(pulseTimer) {
			if(pulseTimer != null && pulseTimer.isAlive()) pulseTimer.interrupt();
		}
	}
	
	/**
	 * Handles DenyMessages, which are not used in the current version
	 */
	public void receiveDenial(DenyMessage dm) {
		/*
		//sentActionMessages.removeIf(n -> n.getID() == dm.getDeniedMessage_id());
		boolean reinit = false;
		synchronized(lastActionAccess) {
			if (lastActionMessage != null && lastActionMessage.getID() == dm.getDeniedMessage_id()) {
				// Undo last performed action
				if (player.isAlive()) {
					Action a = lastActionMessage.getAction();
					synchronized(bf) {
						// Only if player is still alive, otherwise this could be the cause of denial
						if (a instanceof Move) {
							MoveType m = ((Move) a).getMoveType();
							switch (m) {
							case Left:
								m = MoveType.Right;
								break;
							case Right:
								m = MoveType.Left;
								break;
							case Up:
								m = MoveType.Down;
								break;
							case Down:
								m = MoveType.Up;
								break;
							}
							if (player.getTimestamp() < dm.getTimestamp()) {
								bf.moveUnit(player, m);
							}
						} else if (a instanceof Hit) {
							Unit receiver = bf.getUnit(((Hit) a).getReceiverId());
							if (receiver == null) {
								// Unit is gone, check when it was last updated by the server
								receiver = bf.getDeletedUnit(((Hit) a).getReceiverId());
								if (receiver == null) {
									reinit = true;
								} else {
									if (receiver.getTimestamp() < dm.getTimestamp()) {
										// Server hasn't changed this unit since my action, so i must have deleted it
										receiver.setHp(receiver.getHp() + player.getAp());
										bf.placeUnit(receiver);
									}
								}
							} else {
								// Replenish health
								if (receiver.getTimestamp() < dm.getTimestamp()) {
									receiver.setHp(receiver.getHp() + player.getAp());
								}
							}
						} else if (a instanceof Heal) {
							Unit receiver = bf.getUnit(((Heal) a).getReceiverId());
							if (receiver != null) {
								// Undo healing
								if (receiver.getTimestamp() < dm.getTimestamp()) {
									receiver.setHp(receiver.getHp() - player.getAp());
								}
							}
						} else {
							reinit = true;
						}
					}
				}
			} else {
				reinit = true;
			}
			// Last action was denied, can move!
			_canMove = true;
		}
		if (reinit) {
			Print("!!!!! Reinit should happen");
		}
		*/
	}
	
	/**
	 * Sends a ping back to the sender
	 */
	public void receivePingMessage(PingMessage m) {
		sendMessage(new PingMessage(this, serverAddress, server_id));
	}
	
	/**
	 * Redirects itself to another server and connects
	 */
	public void receiveRedirectMessage(RedirectMessage m) {
		if(state == State.Running)
			reset();
		expectedMessageID = 0;
		Print("Change server id from "+server_id +" to "+m.getServer_id());
		server_id = m.getServer_id();
		serverAddress = Main.ADDRESSES.get(server_id);
		changeState(State.Initialization);
		int playerId = player == null ? -1 : player.getId();
		sendMessage(new InitMessage(this, serverAddress, server_id, playerId));
	}
	
	/**
	 * Receives the data from DataMessage and attempts to execute
	 */
	public void receiveData(DataMessage m) {
		//sentActionMessages.removeIf(n -> n.getID() == m.getActionMessage_id());
		if(m.getDatamessage_id() > expectedDataMessageID + 20) {
			Print("data_message_overflow ("+m.getDatamessage_id()+","+expectedDataMessageID+")");
			connect();
			return;
		}			
		dataMessageBuffer.add(m);
		Collections.sort(dataMessageBuffer, 
				(DataMessage m1, DataMessage m2) -> m1.getDatamessage_id() - m2.getDatamessage_id());
			
		while (!dataMessageBuffer.isEmpty() && dataMessageBuffer.get(0).getDatamessage_id() <= expectedDataMessageID) {
			DataMessage dm = dataMessageBuffer.remove(0);
			if(dm.getDatamessage_id() == expectedDataMessageID) {
				deliverData(dm);
				expectedDataMessageID++;
			}
		}
	}
	
	/**
	 * Executes a data message, updates the battlefield with the data in the message
	 */
	public void deliverData(DataMessage m) {
		synchronized(bf) {
			Print("Received data: "+m.getData());
			if(state == State.Initialization)
				for(Unit u_old: bf.getUnitList()) 
					if(m.getData().getUpdatedUnits().contains(u_old)) {
						u_old.setTimestamp(m.getTimestamp());
						bf.killUnit(u_old);
					}
			for(Unit u_new: m.getData().getUpdatedUnits()) {
				Unit u_old = bf.getUnit(u_new);
				u_new.setTimestamp(m.getTimestamp());
				if (u_old != null ) {
					if (u_old.getTimestamp() < u_new.getTimestamp())
						bf.updateUnit(u_old, u_new);
				} else {
					Print("New Unit: "+u_new);
					bf.placeUnit(u_new);
					if(u_new.equals(m.getData().getPlayer()))
						player = u_new;
				}
			}
			for(Integer id: m.getData().getDeletedUnits()) {
				Unit u = bf.getUnit(id);
				if(u != null) {
					u.setTimestamp(m.getTimestamp());
					bf.killUnit(u);
				}
			}	
		}
		if(state == State.Initialization) {
			//for(ActionMessage am: sentActionMessages)
			//	sendMessage(am);
			changeState(State.Running);
		}
	}
	
	/**
	 * Sends a message to destination
	 */
	public synchronized void sendMessage(Message m) {
		m.setID(lastMessageSentID++);
		m.addTail(sentMessages);
		if(sentMessages.size() >= Message.FWD_COUNT)
			sentMessages.remove(0);
		sentMessages.add(m);
		if(m instanceof ActionMessage)
			actionTimeStamp.put((ActionMessage) m, System.currentTimeMillis());
		m.send();
	}
	
	/**
	 * Sets this client's player
	 */
	public void setPlayer(Unit player) {
		this.player = player;
	}
	
	/**
	 * Returns the local battlefield
	 */
	public Battlefield getBattlefield() throws RemoteException {
		return bf;
	}
}
