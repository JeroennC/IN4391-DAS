package das;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;

import das.message.Message;


public class Server extends UnicastRemoteObject implements Node_RMI, Runnable {
	private static final long serialVersionUID = -7107765751618924352L;
	public static final String[] addresses = {"localhost", "localhost", "localhost", "localhost", "localhost"};
	public static final int PULSE = 1000;//ms
	private long deltaTime;
	
	private int id;
	
	protected Server() throws RemoteException {
		super();
	}

	@Override
	public void run() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void receiveMessage(Message m) throws RemoteException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public int getID() {
		return id;
	}
	
	@Override
	public String getName() {
		return "Server_"+id;
	}

}
