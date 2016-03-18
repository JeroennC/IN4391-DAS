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


public class Client extends UnicastRemoteObject implements Node_RMI, Runnable {
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
	
	private List<Unit> units;
	private Unit player;
	
	public Client() throws RemoteException {
		super();
		sentMessages = new LinkedList<Message>();
		sentActionMessages = new LinkedList<ActionMessage>();
		List<Unit> units = new ArrayList<Unit>();
		dataMessageBuffer = new LinkedList<DataMessage>();
	}

	@Override
	public void run() {
		// TODO Auto-generated method stub
		while(true) { //TODO exit condition
			if(_canMove) {
				
			}
		}
	}
	
	public void connect() {
		server_id = (int) (Math.random() * Server.addresses.length);
		sendMessage(new ConnectMessage(this, server_id));
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
				sendMessage(new PingMessage(Client.this, server_id));
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
			sendMessage(new RetransmitMessage(this, server_id, expectedDataMessageID));
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
}
