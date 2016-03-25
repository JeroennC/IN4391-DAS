package das;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import das.action.NewPlayer;
import das.message.*;


public class Server extends Node {
	private static final long serialVersionUID = -7107765751618924352L;
	public static final Address[] ADDRESSES = {
		new Address("localhost", Main.port),
		new Address("localhost", Main.port),
		new Address("localhost", Main.port),
		new Address("localhost", Main.port),
		new Address("localhost", Main.port),
		}; //TODO temporal solution, Addresses can also be read from a file
	public static final int PULSE = 1000;//ms
	private static final int[] TSS_DELAYS = {0, 200, 500, 1000, 10000};
	
	private long deltaTime;
	private Map<String, Integer> lastMessageSentID;
	private Map<String, Integer> lastDataMessageSentID;
	private Map<String, List<Integer>> acks;
	private Map<String, Integer> serverLoad;
	private Map<String, Address> clientAddresses;
	private Map<String, Address> serverAddresses;
	private List<Message> unacknowledgedMessages;
	
	private ServerState[] trailingStates;
	
	public Server(int id) throws RemoteException {
		super(id, "Server_"+id);
		state = State.Disconnected;
		trailingStates = new ServerState[TSS_DELAYS.length];
		for(int i=0; i<trailingStates.length;i++) {
			trailingStates[i] = new ServerState(this, TSS_DELAYS[i]);
			if (i>0) trailingStates[i].setFasterState(trailingStates[i-1]);
			if (i>0) trailingStates[i-1].setSlowerState(trailingStates[i]);
		}
		lastMessageSentID = new HashMap<String, Integer>();
		lastDataMessageSentID = new HashMap<String, Integer>();
		serverLoad = new HashMap<String, Integer>();
		clientAddresses = new HashMap<String, Address>();
		serverAddresses = new HashMap<String, Address>();
		serverLoad.put(getName(), 0);
		unacknowledgedMessages = new LinkedList<Message>();
		acks = new HashMap<String, List<Integer>>();
	}

	@Override
	public void run() {
		connect();
		while(state != State.Exit) {
			
		}
		
		close();
	}
	
	private void connect() {
		for(int i=0;i<ADDRESSES.length;i++)
			if(id != i)
				sendMessage(new PingMessage(this, ADDRESSES[i], "Server_"+i));
		
		for(int i=0; i<20 && !unacknowledgedMessages.isEmpty(); i++) {
			try { Thread.sleep(100); } catch (InterruptedException e) { }
		}
		changeState(State.Initialization);
		if(unacknowledgedMessages.size() == ADDRESSES.length-1) {
			Battlefield bf = new Battlefield();
			bf.initialize();
			for(ServerState ss: trailingStates)
				ss.init(bf);
		} else {
			int copyServerId = ((int)(Math.random() * Integer.MAX_VALUE)) % serverAddresses.size();
			String serverName = (String) serverAddresses.keySet().toArray()[copyServerId];
			Address a = serverAddresses.get(serverName);
			sendMessage(new InitServerMessage(this, a, serverName));
		}
	}
	
	public void receiveActionMessage(ActionMessage m) {
		if(trailingStates[0].isPossible(m.getAction())) {
			for(ServerState ss: trailingStates)
				ss.receive(m);
			//TODO send DataMessage back to Client. Here or from first tss?
		} else {
			sendMessage(new DenyMessage(this, clientAddresses.get(m.getFrom_id()), m.getFrom_id(), m.getID()));
		}
	}
	
	public void receiveRetransmitMessage(RetransmitMessage m) {
		//TODO how to store messages, and for how long? What when a retransmitmessage is received for a message that is expired?
	}
	
	public void receivePingMessage(PingMessage m) {
		if(m.getFrom_id().startsWith("Client"))
			sendMessage(new PulseMessage(this, clientAddresses.get(m.getFrom_id()), m.getFrom_id()));
		else
			sendMessage(new ServerUpdateMessage(this, serverAddresses.get(m.getFrom_id()), m.getFrom_id()));
	}
	
	public void receiveConnectMessage(ConnectMessage m) {
		//TODO return address of least loaded server
		sendMessage(new RedirectMessage(this, clientAddresses.get(m.getFrom_id()), m.getFrom_id(), this.id));
	}
	
	public void receiveInitMessage(InitMessage m) {
		lastMessageSentID.put(m.getFrom_id(), 0);
		lastDataMessageSentID.put(m.getFrom_id(), 0);
		Data d = new Data();
		// Create new player
		int playerId = trailingStates[0].getNextUnitId();
		//TODO Create ActionMessage without using m.getFrom(), for this gives an exception because m.getFrom is a RMI object
		//ActionMessage newPlayer = new ActionMessage((Client)m.getFrom(), clientAddresses.get(m.getFrom_id()), this.id, new NewPlayer(playerId));
		//try { receiveMessage(newPlayer); } catch (RemoteException e1) {	}
		// Wait to ensure player exists in unit list
		try { Thread.sleep(500); } catch (InterruptedException e) { }
		// Gather data in data object from trailingStates[0]
		d.setUpdatedUnits(trailingStates[0].getUnitList());
		// Get player
		Unit player = null;
		for (Unit unit : d.getUpdatedUnits()) {
			if (unit.getId() == playerId) {
				player = unit;
				break;
			}
		}
		d.setPlayer(player);
		sendMessage(new DataMessage(this, clientAddresses.get(m.getFrom_id()), m.getFrom_id(), d, 0, 0));
	}
	
	public void receiveServerUpdateMessage(ServerUpdateMessage m) {
		serverLoad.put(m.getFrom_id(), m.getServerLoad());		
	}
	
	public void receiveNewServerMessage(NewServerMessage m) {
		serverLoad.put(m.getFrom_id(), 0);
		lastMessageSentID.put(m.getFrom_id(), 0);
		synchronized(acks) {
			List<Integer> ackList = new ArrayList<Integer>();
			ackList.add(m.getID());
			acks.put(m.getFrom_id(), ackList);
		}
	}
	
	public void receiveInitServerMessage(InitServerMessage initServerMessage) {
		// send data to server		
	}

	@Override
	public void receiveMessage(Message m) throws RemoteException {
		Print("received message: "+m.toString());
		//TODO for client messages receive in order of sending (so with messages in tail received first)
		if(m.getFrom_id().startsWith("Server")) {
			if(!(m instanceof NewServerMessage))
				updateTimer(m);
			serverAddresses.put(m.getFrom_id(), m.getFromAddress());
			synchronized(acks) {
				if(acks.containsKey(m.getFrom_id())) acks.get(m.getFrom_id()).add(m.getID());
			}
		} if(m.getFrom_id().startsWith("Client")) {
			m.setTimestamp(getTime());
			clientAddresses.put(m.getFrom_id(), m.getFromAddress());
		}
		m.receive(this);		
	}

	private void updateTimer(Message m) {
		long currentTime = getTime();
		if(currentTime < m.getTimestamp())
			updateDeltaTime(m.getTimestamp() - currentTime + 1);		
	}

	public long getDeltaTime() {
		return deltaTime;
	}

	public void updateDeltaTime(long deltaTime) {
		this.deltaTime += deltaTime;
	}
	
	public long getTime() {
		return System.currentTimeMillis() + getDeltaTime();
	}
	
	public synchronized void sendMessage(Message m) {
		int m_id = lastMessageSentID.containsKey(m.getReceiver_id()) ? lastMessageSentID.get(m.getReceiver_id()) : 0; 
		m.setID(m_id);
		lastMessageSentID.put(m.getReceiver_id(), m_id + 1);
		if(m.getReceiver_id().startsWith("Server"))
			unacknowledgedMessages.add(m);
		synchronized(acks) {
			List<Integer> a;
			if(acks.containsKey(m.getReceiver_id())) {
				 a = new ArrayList<Integer>(acks.get(m.getReceiver_id()));
				acks.get(m.getReceiver_id()).clear();
			} else
				a = new ArrayList<Integer>();
			m.setAcks(a);
		}
		m.send();
	}
}
