package das.server;
import java.rmi.RemoteException;
import java.rmi.server.ServerNotActiveException;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import das.Battlefield;
import das.Main;
import das.Node;
import das.Unit;
import das.action.Hit;
import das.log.LogEntry;
import das.log.LogSender;
import das.message.ActionMessage;
import das.message.Address;
import das.message.ConnectMessage;
import das.message.Data;
import das.message.DataMessage;
import das.message.DenyMessage;
import das.message.InitMessage;
import das.message.InitServerMessage;
import das.message.LogMessage;
import das.message.Message;
import das.message.NewServerMessage;
import das.message.PingMessage;
import das.message.PulseMessage;
import das.message.RedirectMessage;
import das.message.RetransmitMessage;
import das.message.ServerStartDataMessage;
import das.message.ServerUpdateMessage;


public class Server extends Node {
	private static final long serialVersionUID = -7107765751618924352L;
	public static final Address[] ADDRESSES = {
		new Address("localhost", Main.port),
		new Address("52.58.28.19", Main.port),
		new Address("localhost", Main.port),
		new Address("localhost", Main.port),
		new Address("localhost", Main.port),
		}; //TODO temporal solution, Addresses can also be read from a file
	public static final int PULSE = 1000;//ms
	public static final String LOG_DIR = "log";
	private static final int[] TSS_DELAYS = {0, 200, 500, 1000, 10000};
	
	/* Server positioning */
	private Object dragonControl;
	private boolean canMove;
	private int serversRunning;
	private int position;
	
	private long deltaTime;
	private Map<String, Connection> connections;
	private List<Message> unacknowledgedMessages;
	private Thread pulse;
	
	private ServerState[] trailingStates;
	private Queue<Message> inbox;
	
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
		inbox = new PriorityQueue<Message>(1, (Message m1, Message m2) -> (int) (m1.getTimestamp() - m2.getTimestamp()));
		
		dragonControl = new Object();
		logSender = new LogSender(this);
		new Thread(logSender, this.getId() + "_LogSender").start();
		
		canMove = false;
		
		pulse = new Thread() {
			public void run() { 
				callPulse();
			}
		};
		pulse.start();
	}

	@Override
	public void run() {
		Log("Initializing");
		connect();
		while(state != State.Exit) {
			// Dragon control
			if (canMove) {
				List<ActionMessage> messages = new LinkedList<ActionMessage>();
				synchronized(dragonControl) {
					Battlefield bf = trailingStates[0].getBattlefield();
					synchronized(bf) {
						bf.getUnitList()
							.parallelStream()
							.filter(u -> 
								{ return u.isDragon() && u.getId() % serversRunning == position; }
							).forEach(dragon -> {
								Unit target = bf.getClosestPlayer(dragon);
								if (target != null) {
									// Attack
									Printf("Dragon %d attacking player %d", dragon.getId(), target.getId());
									ActionMessage am = new ActionMessage(this, new Hit(dragon.getId(), target.getId()));
									am.setTimestamp(this.getTime());
									messages.add(am);
								}
							});
					}
					for (ActionMessage am : messages)
						receiveActionMessage(am);
				}
				this.canMove = false;
			}
			
			Print("Time: "+((long) getTime()/1000));

			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
		Log("Shutting down");
		close();
		pulse.interrupt();
		Main.removeThread(this.getName());
	}
	
	private void connect() {
		do {
			for(int i=0;i<ADDRESSES.length;i++)
				if(id != i)
					sendMessage(new PingMessage(this, ADDRESSES[i], "Server_"+i));
			
			for(int i=0; i<20; i++) {
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
				while(state != State.Running) {
					if(!getConnections().containsKey(serverName)) {
						changeState(State.Disconnected);
					}
					try { Thread.sleep(100); } catch (InterruptedException e) { }
				}
			}
		} while(state == State.Disconnected);
		updateServerPositioning();
	}
	
	public void callPulse() {
		while(state != State.Exit) {
			int serverLoad = getClientConnections().size();
			for(Entry<String, Connection> e: getConnections().entrySet()) {
				Connection c = e.getValue();
				//Send pingMessage when no connection, or disconnect
				if(c.getLastConnectionTime() + 4*PULSE < getTime() ) {
					getConnections().remove(e.getKey());
					continue;
					//TODO other things to do when node is disconnected?
				} else if(c.getLastConnectionTime() + 2*PULSE < getTime() ) {
					sendMessage(new PingMessage(this, c.getAddress(), e.getKey()));
				}
				
				if(c instanceof ClientConnection) {
					((ClientConnection) c).canMove(true);
					sendMessage(new PulseMessage(this, c.getAddress(), e.getKey() ));
				} else {
					if(((ServerConnection) c).getLastServerStatusTime() + 10*PULSE < getTime()) {
						sendMessage(new ServerUpdateMessage(this, c.getAddress(), e.getKey(), serverLoad));
						((ServerConnection) c).setLastServerStatusTime(getTime());
					}
				}
			}
			this.canMove = true;
			try { Thread.sleep(PULSE); } catch (InterruptedException e1) { }
		}
	}
	
	public void sendRollback(Data data) {
		sendData(data, null, -1);
	}
	
	private void sendData(Data data, String from, int mid) {
		Unit player = data.getPlayer();
		for(Entry<String, ClientConnection> e: getClientConnections().entrySet()) {
			int dataId = getClientConnections().get(e.getKey()).incrementLastDataMessageSentID();
			int am = -1;
			Data d = (from == null ?  data : data.clone());
			if(e.getKey().equals(from)) {
				am = mid;
				d.setPlayer(player);
			} else
				d.setPlayer(null);
			sendMessage(new DataMessage(this, e.getValue().getAddress(), e.getKey(), d , am, dataId)) ;
		}
	}
	
	public void receiveActionMessage(ActionMessage m) {
		boolean fromClient = m.getFrom_id().startsWith("Client");
		boolean fromMyself = m.getFrom_id().equals(this.getName());
		if(!fromClient || (getClientConnections().get(m.getFrom_id()).canMove() && trailingStates[0].isPossible(m.getAction()))) {
			if(fromClient)
				getClientConnections().get(m.getFrom_id()).canMove(false);
			// Create linked StateCommands
			StateCommand[] commands = new StateCommand[TSS_DELAYS.length];
			for(int i = 0; i < TSS_DELAYS.length; i++) 
				commands[i] = new StateCommand(commands, i, m);
			Data data = trailingStates[0].receive(commands[0]);
			if (data == null) {
				if (!fromMyself)
					sendMessage(new DenyMessage(this, getAddress(m.getFrom_id()), m.getFrom_id(), m.getID()));
				return;
			}
			for(int i = 1; i < TSS_DELAYS.length; i++)
				trailingStates[i].receive(commands[i]);
			if(data != null) {
				Unit player = data.getPlayer();
				if(player != null && fromClient)
					getClientConnections().get(m.getFrom_id()).setUnitId(player.getId());
				sendData(data, m.getFrom_id(), m.getID());
			}
			// TODO Should this not always send?
			if(fromClient) {
				for(Entry<String, ServerConnection> e: getServerConnections().entrySet()) {
					ActionMessage am = new ActionMessage(this, e.getValue().getAddress(), e.getKey(), m.getAction());
					am.setTimestamp(m.getTimestamp());
					sendMessage(am);
				}
			}
		} else if(fromClient) {
			sendMessage(new DenyMessage(this, getAddress(m.getFrom_id()), m.getFrom_id(), m.getID()));
		}
	}
	
	public void receiveRetransmitMessage(RetransmitMessage m) {
		Connection c = getConnections().get(m.getFrom_id());
		for(int i=m.getFirstMessage_id(); i<=m.getLastMessage_id(); i++) {
			Message ret = c.getSentMessage(i);
			if(ret!=null)
				resendMessage(ret);
		}
	}
	
	public void receivePingMessage(PingMessage m) {
		if(m.getFrom_id().startsWith("Client"))
			sendMessage(new PulseMessage(this, getAddress(m.getFrom_id()), m.getFrom_id()));
		else {
			sendMessage(new ServerUpdateMessage(this, getAddress(m.getFrom_id()), m.getFrom_id(), getClientConnections().size()));
			getServerConnections().get(m.getFrom_id()).setLastServerStatusTime(getTime());
		}
	}
	
	public void receiveConnectMessage(ConnectMessage m) {
		int sid = this.id;
		int least = getClientConnections().size();
		for(Entry<String, ServerConnection> e: getServerConnections().entrySet()) {
			if(e.getValue().getServerLoad() < least) {
				least = e.getValue().getServerLoad();
				sid = Integer.valueOf(e.getKey().replace("Server_", ""));
			}
		}
		sendMessage(new RedirectMessage(this, getAddress(m.getFrom_id()), m.getFrom_id(), sid));
		if(sid != id)
			getConnections().remove(m.getFrom_id());
	}
	
	public void receiveInitMessage(InitMessage m) {
		ClientConnection c = (ClientConnection) getConnections().get(m.getFrom_id());
		c.setLastMessageSentID(0);
		c.setLastDataMessageSentID(0);
		if(m.getPlayerId() == -1) {
			// Create new player
			int playerId = trailingStates[0].getNextUnitId();
			receiveActionMessage(new ActionMessage(m, playerId));
			
			// Log
			Log("Initializing client: " + m.getFrom_id());
		} else
			sendData(trailingStates[0].getData(), null, -1);
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
		Battlefield bf = trailingStates[TSS_DELAYS.length-1].cloneBattlefield();
		Queue<StateCommand> inbox = trailingStates[TSS_DELAYS.length-1].cloneInbox();
		ServerStartDataMessage m = new ServerStartDataMessage(
				this, initServerMessage.getFromAddress(), initServerMessage.getFrom_id(), bf, inbox, getTime());
		sendMessage(m);
	}
	
	public void receiveServerStartDataMessage(ServerStartDataMessage m) {
		updateDeltaTime(m.getTime() - getTime());
		updateServerPositioning();
		
		for(ServerState ss: trailingStates)
			ss.init(m.getBf());
		for(int i = 0; i < trailingStates.length; i++)
			new Thread(trailingStates[i], this.getName() + "_ts_" + i).start();
		for(int i=0;i<TSS_DELAYS.length;i++)
			for(StateCommand sc: m.getInbox())
				trailingStates[i].receive(sc);

		changeState(State.Running);
	}

	@Override
	public synchronized void receiveMessage(Message m) throws RemoteException {
		Address addr = m.getFromAddress();
		try {
			addr.setAddress(getClientHost());
		} catch (ServerNotActiveException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		Print("Received message: " + m.toString());
		//TODO for client messages receive in order of sending (so with messages in tail received first)
		if(m.getFrom_id().startsWith("Server")) {
			if(!(m instanceof NewServerMessage || m instanceof PingMessage))
				updateTimer(m);
			ServerConnection c = getServerConnections().get(m.getFrom_id());
			if(c == null) {
				c = new ServerConnection( addr, getTime());
				getConnections().put(m.getFrom_id(), c );
			}
			c.addAck(m.getID());
		} if(m.getFrom_id().startsWith("Client")) {
			m.setTimestamp(getTime());
			if(!getConnections().containsKey(m.getFrom_id()))
				getConnections().put(m.getFrom_id(), new ClientConnection( addr) );
		}
		getConnections().get(m.getFrom_id()).setLastConnectionTime(getTime());
		for(Message n: inbox)
			if(canReceive(n))
				n.receive(this);
		if(canReceive(m))
			m.receive(this);
		else
			inbox.add(m);
	}
	
	public boolean canReceive(Message m) {
		return !(  (state == State.Disconnected && !(m instanceof ServerUpdateMessage || m instanceof PingMessage)) 
				|| (state == State.Initialization && !(m instanceof ServerStartDataMessage || m instanceof PingMessage))
				|| (state == State.Inconsistent && (m instanceof ActionMessage || m instanceof InitServerMessage || m instanceof InitMessage)));
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
			c.addMessage(m);
			if(m.getReceiver_id().startsWith("Server")) {
				unacknowledgedMessages.add(m);
				m.setAcks(c.getAndResetAcks());
			}
		}
		m.send();
	}
	
	public synchronized void resendMessage(Message m) {
		m.send();
	}
	
	/* Logging */
	private LogSender logSender;
	
	public void Log(String content) {
		LogEntry e = new LogEntry(this.getName(), content, this.getTime());
		e.WriteToFile(this.getName() + ".txt");
		logSender.add(e);
	}
	
	public void receiveLogMessage(LogMessage m) {
		// Write to log
		m.getEntries().forEach(entry -> entry.WriteToFile(this.getName() + ".txt"));
	}
	
	public void sendLog(List<LogEntry> logs) {
		Map<String, ServerConnection> cs = getServerConnections();
		for(Entry<String, ServerConnection> e: cs.entrySet()) {
			sendMessage(new LogMessage(this, e.getValue().getAddress(), e.getKey(), logs));
		}
	}
	
	/* Server positioning */
	public void updateServerPositioning() {
		Map<String, ServerConnection> cs = getServerConnections();
		synchronized(dragonControl) {
			position = 0;
			serversRunning = 1 + cs.size();
			for(String key : cs.keySet()) {
				if (this.getName().compareTo(key) > 0)
					position++;
			}
		}
	}
}
