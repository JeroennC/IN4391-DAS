package das;
import java.rmi.RemoteException;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import das.action.MoveType;
import das.message.*;
import das.server.Server;

public class Client extends Node {
	private static final long serialVersionUID = 8743582021067062104L;
	
	//TODO which variables are volatile?
	private int server_id;
	private Address serverAddress;
	private volatile boolean _canMove;
	private volatile Thread pulseTimer;
	private List<Message> sentMessages;
	private List<DataMessage> dataMessageBuffer;
	private List<ActionMessage> sentActionMessages;
	private int lastMessageSentID;
	private int expectedDataMessageID;
	private int expectedMessageID;
	private List<Integer> receivedPastExpected;
	
	private Battlefield bf;
	private Unit player;
	
	public Client(int id) throws RemoteException {
		super(id, "Client_"+id);
		sentActionMessages = new LinkedList<ActionMessage>();
		bf = new Battlefield();
		reset();
	}
	
	public synchronized void reset() {
		if(pulseTimer != null && !pulseTimer.isAlive()) pulseTimer.interrupt();
		pulseTimer = new Thread();
		_canMove = false;
		changeState(State.Disconnected);
		sentMessages = new LinkedList<Message>();
		dataMessageBuffer = new LinkedList<DataMessage>();
		expectedDataMessageID = 0;
		expectedMessageID = 0;
		lastMessageSentID = 0;
		receivedPastExpected = new LinkedList<Integer>();
	}

	@Override
	public void run() {
		//Sleep so that not all clients start at the same moment.
		try {Thread.sleep((int) (Math.random() * 2000));} catch (InterruptedException e) {}
		
		connect();
		
		// TODO Initialize battlefield
		// Start main loop
		while(state != State.Exit) {
			if(state == State.Disconnected) {
				//Do nothing
			} else if(state == State.Running && _canMove) {
				if (player != null)
					doMove();
			} 
			// TODO sleep for a little time? Busy waiting is a bit much
			try {Thread.sleep(100);} catch (InterruptedException e) {}
		}
		// TODO Disconnect
		Print("Game is over.");
		stopPulseTimer();
		close();
	}
	
	// Synchronized on battlefield so other threads don't mess things up
	public void doMove() {
		synchronized(bf) {
			if(!bf.hasDragons() || !player.isAlive()) {
				// Game is over, change client state
				Print("Game over");
				changeState(State.Exit);
				return;
			}
			List<Unit> nearUnits = bf.getSurroundingUnits(player);
			
			// Heal other player if near and hurt
			for (Unit u : nearUnits) {
				if (u.isType() && u.needsHealing()) {
					bf.healUnit(player, u);
					Print("Healed player " + u.getId());
					return;
				}
			}
			// Attack dragon if it is near
			for (Unit u : nearUnits) {
				if (!u.isType()) {
					bf.attackUnit(player, u);
					Printf("Attacked %d, health left: %d", u.getId(), u.getHp());
					return;
				}
			}
			// Move towards nearest dragon
			Unit dragon = bf.getClosestDragon(player);
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
		}
	}
	
	public void connect() {
		reset();
		server_id = (int) (Math.random() * Server.ADDRESSES.length);
		// DEV:
		server_id = 1;
		serverAddress = Server.ADDRESSES[server_id];
		Print("Try to connect with server "+(server_id));
		sendMessage(new ConnectMessage(this, serverAddress, server_id));
		resetPulseTimer();
	}

	@Override
	public synchronized void receiveMessage(Message m) throws RemoteException {
		Print("received message: "+m.toString());
		resetPulseTimer();
		if (m.getID() > expectedMessageID) {
			//TODO You could also set a timer for this to wait a little longer before requesting retransmission
			sendMessage(new RetransmitMessage(this, serverAddress, server_id, expectedMessageID, m.getID() - 1));
			receivedPastExpected.add(m.getID());
		} else if (m.getID() == expectedMessageID ){
			expectedMessageID++;
			receivedPastExpected.sort(null);
			while(!receivedPastExpected.isEmpty() && receivedPastExpected.get(0) <= expectedMessageID)
				expectedMessageID = Math.max(expectedMessageID, receivedPastExpected.remove(0));
		} else
			return;
		if(receivedPastExpected.contains(m.getID()))
			return;
		m.receive(this);		
	}
	
	public void receivePulse(PulseMessage m) {
		_canMove = true;		
	}
	
	private void resetPulseTimer() {
		synchronized(pulseTimer) {
			if(pulseTimer != null && pulseTimer.isAlive()) pulseTimer.interrupt();
			pulseTimer = (new Thread() {
				public void run() {
					try { Thread.sleep(2 * Server.PULSE); } catch (InterruptedException e) { return; }
					if(!interrupted()) sendMessage(new PingMessage(Client.this, serverAddress, server_id));
					else return;
					try { Thread.sleep(3 * Server.PULSE); } catch (InterruptedException e) { return; }
					if(!interrupted()) connect();
					else return;
				}
			});
			pulseTimer.start();
		}
	}
	
	private void stopPulseTimer() {
		synchronized(pulseTimer) {
			if(pulseTimer != null && pulseTimer.isAlive()) pulseTimer.interrupt();
		}
	}
	
	public void receiveDenial(DenyMessage m) {
		sentActionMessages.removeIf(n -> n.getID() == m.getDeniedMessage_id());
	}
	
	public void receiveRedirectMessage(RedirectMessage m) {
		if(state == State.Running)
			reset();
		expectedMessageID = 0;
		Print("Change server id from "+server_id +" to "+m.getServer_id());
		server_id = m.getServer_id();
		serverAddress = Server.ADDRESSES[server_id];
		changeState(State.Initialization);
		sendMessage(new InitMessage(this, serverAddress, server_id));
	}
	
	public void receiveData(DataMessage m) {
		Print("Datamesssage "+m);
		sentActionMessages.removeIf(n -> n.getID() == m.getActionMessage_id());
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
	
	public void deliverData(DataMessage m) {
		synchronized(bf) {
			for(Unit u_new: m.getData().getUpdatedUnits()) {
				Unit u_old = bf.getUnit(u_new);
				if (u_old != null ) {
					bf.updateUnit(u_old, u_new);
				} else {
					bf.placeUnit(u_new);
					if(u_new.equals(m.getData().getPlayer()))
						player = u_new;
				}
			}
		}
		if(state == State.Initialization) {
			for(ActionMessage am: sentActionMessages)
				sendMessage(am);
			changeState(State.Running);
		}
	}
	
	public synchronized void sendMessage(Message m) {
		m.setID(lastMessageSentID++);
		m.addTail(sentMessages);
		if(sentMessages.size() >= Message.FWD_COUNT)
			sentMessages.remove(0);
		sentMessages.add(m);
		m.send();
	}
	
	public void setPlayer(Unit player) {
		this.player = player;
	}
	
	public Battlefield getBattlefield() {
		return bf;
	}
	
	public boolean isRunning() {
		return state != State.Exit;
	}
}
