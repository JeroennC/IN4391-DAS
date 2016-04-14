package das.server;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.rmi.RemoteException;
import java.rmi.server.ServerNotActiveException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import das.Battlefield;
import das.Main;
import das.Node;
import das.Unit;
import das.action.DeletePlayer;
import das.action.Hit;
import das.log.LogEntry;
import das.log.LogSender;
import das.message.ActionMessage;
import das.message.Address;
import das.message.ConnectMessage;
import das.message.Data;
import das.message.DataMessage;
import das.message.InitMessage;
import das.message.InitServerMessage;
import das.message.LogMessage;
import das.message.Message;
import das.message.NewServerMessage;
import das.message.PingMessage;
import das.message.PulseMessage;
import das.message.RedirectMessage;
import das.message.RefreshMessage;
import das.message.RetransmitMessage;
import das.message.ServerStartDataMessage;
import das.message.ServerUpdateMessage;


public class Server extends Node {
	private static final long serialVersionUID = -7107765751618924352L;
	public static final int PULSE = 1000;//ms
	public static final String LOG_DIR = "log";
	private static final int[] TSS_DELAYS = {0, 200, 500, 1000, 10000};
	private static final int ACK_WAIT = 4000;//ms
	
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
	private List<Message> inbox;
	
	static int responseTimeI = 0;
	
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
		inbox = new ArrayList<Message>();
		
		dragonControl = new Object();
		InitLog();
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
		Log("S|Server initializing");
		connect();
		while(state != State.Exit) {
			// Dragon control
			if (canMove) {
				List<ActionMessage> messages = new LinkedList<ActionMessage>();
				synchronized(dragonControl) {
					Battlefield bf = trailingStates[0].getBattlefield();
					synchronized(bf) {
						for(Unit dragon: bf.getUnitList()) { 
							if( ! ( dragon.isDragon() && dragon.getId() % serversRunning == position))
								continue;
							Unit target = bf.getClosestPlayer(dragon);
							long lastTime = 0;
							long newTime;
							if (target != null) {
								// Attack
								Printf("Dragon %d attacking player %d", dragon.getId(), target.getId());
								ActionMessage am = new ActionMessage(this, new Hit(dragon.getId(), target.getId()));
								newTime = this.getTime();
								if (newTime <= lastTime)
									newTime = lastTime + 1;
								lastTime = newTime;
								am.setTimestamp(newTime);
								messages.add(am);
							}
						}
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
		
		Log("S|Shutting down");
		close();
		pulse.interrupt();
		Main.removeThread(this.getName());
	}
	
	private void connect() {
		do {
			for(int i=0;i<Main.ADDRESSES.size();i++)
				if(id != i && Main.ADDRESSES.get(i) != null)
					sendMessage(new PingMessage(this, Main.ADDRESSES.get(i), "Server_"+i));
			
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
				for(int i = 0; i < trailingStates.length; i++) {
					new Thread(trailingStates[i], this.getName() + "_ts_" + i).start();
					while(trailingStates[i].getState() != ServerState.State.Running);
				}
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
		int it = 0;
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
					ServerConnection sc = (ServerConnection) c; 
					if(sc.getLastServerStatusTime() + 20*PULSE < getTime()) {
						sendMessage(new ServerUpdateMessage(this, c.getAddress(), e.getKey(), serverLoad));
						sc.setLastServerStatusTime(getTime());
					}					
				}
			}
			// Resend unacknowledged messages if necessary
			List<Message> currentuaM = new LinkedList<Message>(unacknowledgedMessages);
			for (Message m : currentuaM) {
				// Get receiver
				ServerConnection sc = getServerConnections().get(m.getReceiver_id());
				if (sc == null) {
					// If server doesn't exist anymore
					unacknowledgedMessages.remove(m);
					continue;
				}

				if (this.getTime() - ACK_WAIT > m.getTimestamp()) {
					// Resend message
					m.setTimestamp(this.getTime());
					Log("S|Message was not acknowledged, resending " + m.toString());
					resendMessage(m);
				}
			}

			// Log load balance
			if (it++ == 0) {
				Log("L|" + serverLoad);
			}
			if (it > 5)
				it = 0;
			
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
			if(player == null && e.getValue().getLastDataMessageSentID() == 0)
				continue; //Do not send this data, wait for the init message
			int dataId = e.getValue().incrementLastDataMessageSentID();
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
	
	public synchronized void receiveActionMessage(ActionMessage m) {
		boolean fromClient = m.getFrom_id().startsWith("Client");
		boolean fromMyself = m.getFrom_id().equals(this.getName());
		if(fromClient) {
			responseTimeI++;
			if(responseTimeI > getClientConnections().size() * 5) {
				Log("E|"+m.getFrom_id()+"|"+m.getID()+"|"+m.getResponseTime());
				responseTimeI = 0;
			}
		}
		//boolean first = (m.getAction() instanceof NewPlayer);
		if(/*first && */(!fromClient || (getClientConnections().get(m.getFrom_id()).canMove() && trailingStates[0].isPossible(m.getAction())))) {
			if(fromClient){
				ClientConnection cc = getClientConnections().get(m.getFrom_id());
				cc.canMove(false);
				cc.setInvalidMoves(0);
			}
			// Create linked StateCommands
			StateCommand[] commands = new StateCommand[TSS_DELAYS.length];
			for(int i = 0; i < TSS_DELAYS.length; i++) 
				commands[i] = new StateCommand(commands, i, m);
			Data data = trailingStates[0].receive(commands[0]);
			for(int i = 1; i < TSS_DELAYS.length; i++)
				trailingStates[i].receive(commands[i]);
			if(data != null) {
				Unit player = data.getPlayer();
				if(player != null && fromClient)
					getClientConnections().get(m.getFrom_id()).setUnitId(player.getId());
				sendData(data, m.getFrom_id(), m.getID());
			}
			if(fromClient || fromMyself) {
				for(Entry<String, ServerConnection> e: getServerConnections().entrySet()) {
					ActionMessage am = new ActionMessage(this, e.getValue().getAddress(), e.getKey(), m.getAction());
					am.setTimestamp(m.getTimestamp());
					sendMessage(am);
				}
			}
		} else if(fromClient) {
			ClientConnection cc = getClientConnections().get(m.getFrom_id());
			cc.incrementInvalidMoves();
			if(cc.getInvalidMoves() > 20) {
				Print("Delete user "+m.getFrom_id() + " with user id "+cc.getUnitId());
				ActionMessage am = new ActionMessage(this, new DeletePlayer(cc.getUnitId()));
				am.setTimestamp(getTime());
				getConnections().remove(m.getFrom_id());
				receiveActionMessage(am);
				return;
			}
			Data d = cc.getInvalidMoves() > 5 ? trailingStates[0].getData() : trailingStates[0].getDeniedData(m.getAction());
			int dm_id = cc.incrementLastDataMessageSentID();
			sendMessage(new DataMessage(this, m.getFromAddress(), m.getFrom_id(), d, m.getID(),  dm_id));
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
		String name = this.getName();
		int least = getClientConnections().size();
		for(Entry<String, ServerConnection> e: getServerConnections().entrySet()) {
			if(e.getValue().getServerLoad() < least) {
				least = e.getValue().getServerLoad();
				name = e.getKey();
				sid = Integer.valueOf(name.replace("Server_", ""));
			}
		}
		if(sid == id) {
			ClientConnection cc = getClientConnections().get(m.getFrom_id());
			cc.setLastMessageSentID(0);
			cc.setLastDataMessageSentID(0);
			cc.canMove(true);
		}
		sendMessage(new RedirectMessage(this, getAddress(m.getFrom_id()), m.getFrom_id(), sid));
		if(sid != id) {
			getConnections().remove(m.getFrom_id());
			getServerConnections().get(name).setServerLoad(least + 1);
		} 
	}
	
	public void receiveInitMessage(InitMessage m) {
		ClientConnection c = (ClientConnection) getConnections().get(m.getFrom_id());
		c.setLastMessageSentID(0);
		c.setLastDataMessageSentID(0);
		if(m.getPlayerId() == -1) {
			// Create new player
			int playerId = trailingStates[0].getNextUnitId() + Main.ADDRESSES.size() * position;
			receiveActionMessage(new ActionMessage(m, playerId));
			
			// Log
			Log("S|Initializing client: " + m.getFrom_id());
		} else {
			int dmid = c.incrementLastDataMessageSentID();
			DataMessage dm = new DataMessage(this, m.getFromAddress(), m.getFrom_id(), 
					trailingStates[0].getData(), -1, dmid);
			sendMessage(dm);
		}
	}
	
	public void receiveServerUpdateMessage(ServerUpdateMessage m) {
		ServerConnection sc = getServerConnections().get(m.getFrom_id()); 
		sc.setServerLoad(m.getServerLoad());
		int serverLoad = getClientConnections().size();
		//Client forwarding to other server
		if(sc.getServerLoad() < serverLoad - 1 && serverLoad > 0) {
			int sid = Integer.valueOf(m.getFrom_id().replace("Server_",""));
			String name = (String) getClientConnections().keySet().toArray()[0];
			sendMessage(new RedirectMessage(this, getAddress(name), name, sid ));
			getConnections().remove(name);
			sc.setServerLoad(sc.getServerLoad() + 1);
		}
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
				this, initServerMessage.getFromAddress(), initServerMessage.getFrom_id(), bf, inbox, getLogContent(), getTime());
		sendMessage(m);
	}
	
	public void receiveRefreshMessage(RefreshMessage m) {
		Data d = new Data();
		List<Unit> units = m.getRequestedUnits();
		Battlefield bf = trailingStates[0].getBattlefield();
		synchronized(bf) {
			for (Unit u : units) {
				Unit ref = bf.getUnit(u);
				if (ref == null) {
					d.deleteUnit(u.getId());
				} else {
					u.setHp(ref.getHp());
					u.setX(ref.getX());
					u.setY(ref.getY());
				}
			}
		}
		d.setUpdatedUnits(units);
		int dataId = getClientConnections().get(m.getFrom_id()).incrementLastDataMessageSentID();
		sendMessage(new DataMessage(this, getAddress(m.getFrom_id()), m.getFrom_id(), d, m.getID(), dataId));
	}
	
	public void receiveServerStartDataMessage(ServerStartDataMessage m) {
		updateDeltaTime(m.getTime() - getTime());
		updateServerPositioning();
		
		this.printLogContent(m.getLog());
		
		for(ServerState ss: trailingStates)
			ss.init(m.getBf());
		if (m.getInbox().size() > 0) {
			for(int i=TSS_DELAYS.length-1; i>=0;i--) {
				PriorityQueue<StateCommand> q = new PriorityQueue<StateCommand>(m.getInbox().size(), ServerState.comparator);
				q.addAll(m.getInbox());
				trailingStates[i].rollback(q);
			}
		}
		
		for(int i = 0; i < trailingStates.length; i++){
			new Thread(trailingStates[i], this.getName() + "_ts_" + i).start();
			while(trailingStates[i].getState() != ServerState.State.Running);
		}
		changeState(State.Running);
	}

	@Override
	public synchronized void receiveMessage(Message m) throws RemoteException {
		Print("Received message: " + m.toString());
		//TODO for client messages receive in order of sending (so with messages in tail received first)
		if(m.getFrom_id().startsWith("Server")) {
			if(!(m instanceof NewServerMessage || m instanceof PingMessage))
				updateTimer(m);
			if (m.getAcks() != null && !m.getAcks().isEmpty())
				unacknowledgedMessages.removeIf(n -> n.getReceiver_id().equals(m.getFrom_id()) && m.getAcks().contains(n.getID()));
		} if(m.getFrom_id().startsWith("Client")) {
			m.setTimestamp(getTime());
		}
		inbox.sort((Message m1, Message m2) -> (int) (m1.getTimestamp() - m2.getTimestamp()));
		for(Iterator<Message> iterator = inbox.iterator(); iterator.hasNext();) {
			Message n = iterator.next();
			if(canReceive(n)) {
				deliverMessage(n);
				iterator.remove();
			}
		}
		if(canReceive(m))
			deliverMessage(m);
		else
			inbox.add(m);
	}
	
	public synchronized void deliverMessage(Message m) {
		Address addr = m.getFromAddress();
		try {
			addr.setAddress(getClientHost());
		} catch (ServerNotActiveException e1) {
			e1.printStackTrace();
		}
		//TODO for client messages receive in order of sending (so with messages in tail received first)
		if(m.getFrom_id().startsWith("Server")) {
			ServerConnection c = getServerConnections().get(m.getFrom_id());
			if(c == null) {
				c = new ServerConnection( addr, getTime());
				getConnections().put(m.getFrom_id(), c );
				updateServerPositioning();
			}
			if (m.getID() > 0 && c.hasMessageId(m.getID())) {
				// Message has already been executed, ignore it (> 0 to not do this at startup)
				Log("S|Received already executed message: " + m.toString());
				return;
			}
			c.addReceivedMessageId(m.getID());
			c.addAck(m.getID());
		} if(m.getFrom_id().startsWith("Client")) {
			if(!getConnections().containsKey(m.getFrom_id())) {
				if(m instanceof ConnectMessage || m instanceof InitMessage)
					getConnections().put(m.getFrom_id(), new ClientConnection( addr) );
				else 
					return;
			}
		}
		getConnections().get(m.getFrom_id()).setLastConnectionTime(getTime());
		m.receive(this);
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
		boolean send_it = true;
		m.setID(m_id);
		if(c != null) {
			c.setLastMessageSentID(m_id + 1);
			c.addMessage(m);
			if(m.getReceiver_id().startsWith("Server")) {
				unacknowledgedMessages.add(m);
				m.setAcks(c.getAndResetAcks());
				/*
				if (new Random().nextInt(10) == 0) {
					Log("Not sending " + m.toString());
					send_it = false;
				}
				*/
			}
		}
		// Randomly not send messages
		if (send_it)
			m.send();
		else
			m.setTimestamp(getTime());
	}
	
	public synchronized void resendMessage(Message m) {
		m.send();
	}
	
	/* Logging */
	private LogSender logSender;
	
	public void InitLog() {
		try {
			Files.deleteIfExists(Paths.get(Server.LOG_DIR + "/" + this.getName() + ".txt"));
			new LogEntry(this.getName(), "S|Log created", this.getTime()).WriteToFile(this.getName() + ".txt");;
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
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
	
	public List<String> getLogContent() {
		List<String> result = new LinkedList<String>();
		try {
			result = Files.readAllLines(Paths.get(Server.LOG_DIR + "/" + this.getName() + ".txt"));
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return result;
	}
	
	public void printLogContent(List<String> log) {
		for (String str : log) {
			try {
				Files.write(Paths.get(Server.LOG_DIR + "/" + this.getName() + ".txt"), (str + "\n").getBytes(), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
			} catch (IOException e) {
				e.printStackTrace();
			}
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

	@Override
	public Battlefield getBattlefield() throws RemoteException {
		// TODO maybe just do getBattlefield here?
		return trailingStates[0].cloneBattlefield();
	}
}
