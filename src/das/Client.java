package das;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Timer;

import das.message.ActionMessage;
import das.message.ConnectMessage;
import das.message.DataMessage;
import das.message.DenyMessage;
import das.message.Message;
import das.message.PingMessage;
import das.message.PulseMessage;
import das.message.RetransmitMessage;


public class Client implements Node_RMI, Runnable {
	private static final long serialVersionUID = 8743582021067062104L;
	
	private int id;
	private int server_id;
	private boolean _canMove = false;
	private Thread pulseTimer;
	private List<Message> sentMessages;
	private List<DataMessage> dataMessageBuffer;
	private List<ActionMessage> sentActionMessages;
	private int lastMessageSentID = 0;
	private int expectedDataMessageID = 0;
	private enum State { Disconnected, Initialization, Running, Exit };
	
	private State state;
	private List<Unit> units;
	private Unit player;
	
	public Client() {
		super();
		state = State.Disconnected;
		sentMessages = new LinkedList<Message>();
		sentActionMessages = new LinkedList<ActionMessage>();
		//List<Unit> units = new ArrayList<Unit>();
		dataMessageBuffer = new LinkedList<DataMessage>();
	}

	@Override
	public void run() {
		// TODO Connect to server
		// TODO Initialize battlefield
		_canMove = true;
		state = State.Running;
		// Start main loop
		while(state == State.Running) {
			if(_canMove) {
				System.out.print("Client doing move: ");
				doMove();
				System.out.print("\n");
			}
			// TODO sleep for a little time? Busy waiting is a bit much
			try {Thread.sleep(100);} catch (InterruptedException e) {}
		}
		// TODO Disconnect
		System.out.println("Game is over.");
	}
	
	// Synchronized on battlefield so other threads don't mess things up
	public void doMove() {
		Battlefield bf = Battlefield.getBattlefield();
		synchronized(bf) {
			if(!bf.hasDragons() || !player.isAlive()) {
				// Game is over, change client state
				System.out.print("Game over");
				state = State.Exit;
				return;
			}
			List<Unit> nearUnits = bf.getSurroundingUnits(player);
			
			// Heal other player if near and hurt
			for (Unit u : nearUnits) {
				if (u.isType() && u.needsHealing()) {
					bf.healUnit(player, u);
					System.out.print("Healed player " + u.getId());
					return;
				}
			}
			// Attack dragon if it is near
			for (Unit u : nearUnits) {
				if (!u.isType()) {
					bf.attackUnit(player, u);
					System.out.printf("Attacked %d, health left: %d", u.getId(), u.getHp());
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
			System.out.printf("Moved to %s, now at x %d, y %d ", m.toString(), player.getX(), player.getY());
		}
	}
	
	public void connect() {
		server_id = (int) (Math.random() * Server.addresses.length);
		try {
			sendMessage(new ConnectMessage(this, server_id));
		} catch (RemoteException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		resetPulseTimer();
	}

	@Override
	public void receiveMessage(Message m) throws RemoteException {
		resetPulseTimer();
		m.receive(this);		
	}
	
	public void receivePulse(PulseMessage m) {
		_canMove = true;		
	}
	
	private void resetPulseTimer() {
		if(pulseTimer != null && !pulseTimer.isAlive()) pulseTimer.interrupt();
		pulseTimer = (new Thread() {
			public void run() {
				try {
					Thread.sleep(2 * Server.PULSE);
				} catch (InterruptedException e) {
					return;
				}
				try {
					sendMessage(new PingMessage(Client.this, server_id));
				} catch (RemoteException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
				try {
					Thread.sleep(3 * Server.PULSE);
				} catch (InterruptedException e) {
					return;
				}
				connect();
			}
		});
		pulseTimer.start();
	}
	
	public void receiveDenial(DenyMessage m) {
		sentActionMessages.removeIf(n -> n.getID() == m.getDeniedMessage_id());
	}
	
	public void receiveData(DataMessage m) {
		sentActionMessages.removeIf(n -> n.getID() == m.getActionMessage_id());
		dataMessageBuffer.add(m);
		Collections.sort(dataMessageBuffer, 
				(DataMessage m1, DataMessage m2) -> m1.getDatamessage_id() - m2.getDatamessage_id());
		DataMessage firstMessage = dataMessageBuffer.get(0);
		
		if (firstMessage.getDatamessage_id() != expectedDataMessageID) {
			//TODO YOu could also set a timer for this to wait a little longer before requesting retransmision
			//TODO request all messages between first and expected
			try {
				sendMessage(new RetransmitMessage(this, server_id, expectedDataMessageID));
			} catch (RemoteException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		while (!dataMessageBuffer.isEmpty() && dataMessageBuffer.get(0).getDatamessage_id() == expectedDataMessageID) {
			deliverData(dataMessageBuffer.remove(0));
			expectedDataMessageID++;
		}
	}
	
	public void deliverData(DataMessage m) {
		for(Unit u_new: m.getData().getUpdatedUnits()) {
			boolean exists = false;
			for(Unit u_old: units) {
				if(u_new.equals(u_old)) {
					u_old.setHp(u_new.getHp());
					u_old.setX(u_new.getX());
					u_old.setY(u_new.getY());
					exists = true;
				}
			}
			if(!exists) {
				units.add(u_new);
				if(u_new.equals(m.getData().getPlayer()))
					player = u_new;
			}
		}
	}
	
	public synchronized void sendMessage(Message m) {
		m.setID(lastMessageSentID++);
		m.addTail(sentMessages);
		if(sentMessages.size() < Message.FWD_COUNT)
			sentMessages.remove(0);
		sentMessages.add(m);
		m.send();
	}

	@Override
	public int getID() {
		return id;		
	}
	
	@Override
	public String getName() {
		return "Client_"+id;		
	}
	
	public void setPlayer(Unit player) {
		this.player = player;
	}
}
