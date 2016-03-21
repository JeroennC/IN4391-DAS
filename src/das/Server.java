package das;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;

import das.message.Message;
import das.message.NewServerMessage;


public class Server extends Node {
	private static final long serialVersionUID = -7107765751618924352L;
	public static final String[] addresses = {"localhost", "localhost", "localhost", "localhost", "localhost"};
	public static final int PULSE = 1000;//ms
	private static final int[] TSS_DELAYS = {0, 200, 500, 1000, 10000};
	
	private int id;
	private long deltaTime;
	
	private ServerState[] trailingStates;
	
	public Server(int id) throws RemoteException {
		super(id, "Server_"+id);
		trailingStates = new ServerState[TSS_DELAYS.length];
		for(int i=0; i<trailingStates.length;i++) {
			trailingStates[i] = new ServerState(this, TSS_DELAYS[i]);
			if (i>0) trailingStates[i].setFasterState(trailingStates[i-1]);
			if (i>0) trailingStates[i-1].setSlowerState(trailingStates[i]);
		}
	}

	@Override
	public void run() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void receiveMessage(Message m) throws RemoteException {
		if(m.getFrom_id().startsWith("Server") && !(m instanceof NewServerMessage)) 
			updateTimer(m);
		if(m.getFrom_id().startsWith("Client")) {
			m.setTimestamp(getTime());
		}
		
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

}
