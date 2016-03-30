package das.server;
import java.rmi.RemoteException;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import das.Battlefield;
import das.Main;
import das.Node;
import das.message.ActionMessage;
import das.message.Address;
import das.message.ConnectMessage;
import das.message.Data;
import das.message.DataMessage;
import das.message.DenyMessage;
import das.message.InitMessage;
import das.message.InitServerMessage;
import das.message.Message;
import das.message.NewServerMessage;
import das.message.PingMessage;
import das.message.PulseMessage;
import das.message.RedirectMessage;
import das.message.RetransmitMessage;
import das.message.ServerUpdateMessage;


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
	private Map<String, Connection> connections;
	private List<Message> unacknowledgedMessages;
	private Thread pulse;
	
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
		connections = new ConcurrentHashMap<String, Connection>();
		unacknowledgedMessages = new LinkedList<Message>();
		
		pulse = new Thread() {
			public void run() { 
				callPulse();
			}
		};
		pulse.start();
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
		if(getServerConnections().size() == 0) {
			// Start a new game, being the only server present
			Battlefield bf = new Battlefield();
			bf.initialize();
			for(ServerState ss: trailingStates)
				ss.init(bf);
			for(int i = 0; i < trailingStates.length; i++)
				new Thread(trailingStates[i], this.getName() + "_ts_" + i).start();
			changeState(State.Running);
		} else {
			// Copy state from other server
			Map<String, ServerConnection> serverConnections = getServerConnections();
			int copyServerId = ((int)(Math.random() * Integer.MAX_VALUE)) % serverConnections.size();
			String serverName = (String) serverConnections.keySet().toArray()[copyServerId];
			Address a = getAddress(serverName);
			sendMessage(new InitServerMessage(this, a, serverName));
		}
	}
	
	public void callPulse() {
		while(state != State.Exit) {
			Map<String, ClientConnection> cs = getClientConnections();
			for(Entry<String, ClientConnection> e: cs.entrySet()) {
				e.getValue().canMove(true);
				sendMessage(new PulseMessage(this, e.getValue().getAddress(), e.getKey() ));
			}
			try { Thread.sleep(PULSE); } catch (InterruptedException e1) { }
		}
	}
	
	public void receiveActionMessage(ActionMessage m) {
		if(getClientConnections().get(m.getFrom_id()).canMove() && trailingStates[0].isPossible(m.getAction())) {
			getClientConnections().get(m.getFrom_id()).canMove(false);
			// Create linked StateCommands
			StateCommand[] commands = new StateCommand[TSS_DELAYS.length];
			for(int i = 0; i < TSS_DELAYS.length; i++) 
				commands[i] = new StateCommand(commands, i, m);
			Data d = trailingStates[0].receive(commands[0]);
			for(int i = 1; i < TSS_DELAYS.length; i++)
				trailingStates[i].receive(commands[i]);
			int dataId = getClientConnections().get(m.getFrom_id()).incrementLastDataMessageSentID();
			sendMessage(new DataMessage(this, getAddress(m.getFrom_id()), m.getFrom_id(), d , m.getID(), dataId)) ;
		} else {
			sendMessage(new DenyMessage(this, getAddress(m.getFrom_id()), m.getFrom_id(), m.getID()));
		}
	}
	
	public void receiveRetransmitMessage(RetransmitMessage m) {
		//TODO how to store messages, and for how long? What when a retransmitmessage is received for a message that is expired?
	}
	
	public void receivePingMessage(PingMessage m) {
		if(m.getFrom_id().startsWith("Client"))
			sendMessage(new PulseMessage(this, getAddress(m.getFrom_id()), m.getFrom_id()));
		else
			sendMessage(new ServerUpdateMessage(this, getAddress(m.getFrom_id()), m.getFrom_id()));
	}
	
	public void receiveConnectMessage(ConnectMessage m) {
		//TODO return address of least loaded server
		sendMessage(new RedirectMessage(this, getAddress(m.getFrom_id()), m.getFrom_id(), this.id));
	}
	
	public void receiveInitMessage(InitMessage m) {
		ClientConnection c = (ClientConnection) getConnections().get(m.getFrom_id());
		c.setLastMessageSentID(0);
		c.setLastDataMessageSentID(0);
		// Create new player
		int playerId = trailingStates[0].getNextUnitId();
		receiveActionMessage(new ActionMessage(m, playerId));
		
		/*// Gather data in data object from trailingStates[0]
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
		sendMessage(new DataMessage(this, getAddress(m.getFrom_id()), m.getFrom_id(), d, 0, 0));*/
	}
	
	public void receiveServerUpdateMessage(ServerUpdateMessage m) {
		getServerConnections().get(m.getFrom_id()).setServerLoad(m.getServerLoad());		
	}
	
	public void receiveNewServerMessage(NewServerMessage m) {
		ServerConnection c = (ServerConnection) getConnections().get(m.getFrom_id());
		c.setServerLoad(0);
		c.setLastMessageSentID(0);
		c.resetAndAddAck(m.getID());
	}
	
	public void receiveInitServerMessage(InitServerMessage initServerMessage) {
		// send data to server		
	}

	@Override
	public void receiveMessage(Message m) throws RemoteException {
		//TODO for client messages receive in order of sending (so with messages in tail received first)
		if(m.getFrom_id().startsWith("Server")) {
			if(!(m instanceof NewServerMessage))
				updateTimer(m);
			ServerConnection c = getServerConnections().get(m.getFrom_id());
			if(c == null)
				c = (ServerConnection) getConnections().put(m.getFrom_id(), new ServerConnection( m.getFromAddress()) );
			c.addAck(m.getID());
		} if(m.getFrom_id().startsWith("Client")) {
			m.setTimestamp(getTime());
			if(!getConnections().containsKey(m.getFrom_id()))
				getConnections().put(m.getFrom_id(), new ClientConnection( m.getFromAddress()) );
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
	
	private Address getAddress(String id) {
		return getConnections().get(id).getAddress();
	}
	
	public Map<String, Connection> getConnections() {
		return connections;
	}
	
	public Map<String, ClientConnection> getClientConnections() {
		return connections.entrySet()
				.parallelStream()
				.filter(e -> { return e.getValue() instanceof ClientConnection; } )
				.collect(Collectors.toMap(e -> { return (String) e.getKey(); }, e -> { return (ClientConnection) e.getValue(); } ) 	);
	}
	
	public Map<String, ServerConnection> getServerConnections() {
		return connections.entrySet()
				.parallelStream()
				.filter(e -> { return e.getValue() instanceof ServerConnection; } )
				.collect(Collectors.toMap(e -> { return (String) e.getKey(); }, e -> { return (ServerConnection) e.getValue(); } ) 	);
	}
	
	public synchronized void sendMessage(Message m) {
		Connection c = getConnections().get(m.getReceiver_id());
		int m_id = (c == null ? 0 : c.getLastMessageSentID()); 
		m.setID(m_id);
		if(c != null) {
			c.setLastMessageSentID(m_id + 1);
			if(m.getReceiver_id().startsWith("Server")) {
				unacknowledgedMessages.add(m);
				m.setAcks(c.getAndResetAcks());
			}
		}
		m.send();
	}
}
