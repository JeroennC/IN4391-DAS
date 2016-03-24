package das;
import java.rmi.RemoteException;
import java.util.HashMap;
import java.util.Map;

import das.message.*;


public class Server extends Node {
	private static final long serialVersionUID = -7107765751618924352L;
	public static final String[] addresses = {"localhost", "localhost", "localhost", "localhost", "localhost"};
	public static final int PULSE = 1000;//ms
	private static final int[] TSS_DELAYS = {0, 200, 500, 1000, 10000};
	
	private int id;
	private long deltaTime;
	private Map<String, Integer> lastMessageSentID;
	private Map<String, Integer> lastDataMessageSentID;
	private Map<String, Integer> serverLoad;
	
	private enum State { Disconnected, Initialization, Running, Inconsistent, Exit };
	private volatile State state;
	
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
		serverLoad.put(getName(), 0);
	}

	@Override
	public void run() {
		
		while(state != State.Exit) {
			
		}
		
	}
	
	public void receiveActionMessage(ActionMessage m) {
		if(trailingStates[0].isPossible(m.getAction())) {
			for(ServerState ss: trailingStates)
				ss.receive(m);
			//TODO send DataMessage back to Client. Here or from first tss?
		} else {
			sendMessage(new DenyMessage(this, m.getFrom_id(), m.getID()));
		}
	}
	
	public void receiveRetransmitMessage(RetransmitMessage m) {
		//TODO how to store messages, and for how long? What when a retransmitmessage is received for a message that is expired?
	}
	
	public void receivePingMessage(PingMessage m) {
		sendMessage(new PulseMessage(this, m.getFrom_id()));
	}
	
	public void receiveConnectMessage(ConnectMessage m) {
		//TODO return address of least loaded server
		sendMessage(new RedirectMessage(this, m.getFrom_id(), this.id));
	}
	
	public void receiveInitMessage(InitMessage m) {
		lastMessageSentID.put(m.getFrom_id(), 0);
		lastDataMessageSentID.put(m.getFrom_id(), 0);
		//TODO gather data in data object from trailingStates[0]
		Data d = new Data();
		sendMessage(new DataMessage(this, m.getFrom_id(), d, 0, 0));
	}
	
	public void receiveServerUpdateMessage(ServerUpdateMessage m) {
		serverLoad.put(m.getFrom_id(), m.getServerLoad());		
	}
	
	public void receiveNewServerMessage(NewServerMessage m) {
		serverLoad.put(m.getFrom_id(), 0);
		lastMessageSentID.put(m.getFrom_id(), 0);
		//TODO How to guide the server initiliaztion flow? Contact all servers, or contact one server? And load balancing? Directly or smoothly?
	}

	@Override
	public void receiveMessage(Message m) throws RemoteException {
		//TODO for client messages receive in order of sending (so with messages in tail received first)
		if(m.getFrom_id().startsWith("Server") && !(m instanceof NewServerMessage)) 
			updateTimer(m);
		if(m.getFrom_id().startsWith("Client")) {
			m.setTimestamp(getTime());
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
		int m_id = lastMessageSentID.get(m.getReceiver_id()); 
		m.setID(m_id);
		lastMessageSentID.put(m.getReceiver_id() + 1, m_id);
		m.send();
	}
}
