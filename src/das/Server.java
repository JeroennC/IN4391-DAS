package das;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;

import das.message.Message;


public class Server extends Node {
	private static final long serialVersionUID = -7107765751618924352L;
	public static final String[] addresses = {"localhost", "localhost", "localhost", "localhost", "localhost"};
	public static final int PULSE = 1000;//ms
	private long deltaTime;
	
	private int id;
	
	public Server(int id) throws RemoteException {
		super(id, "Server_"+id);
	}

	@Override
	public void run() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void receiveMessage(Message m) throws RemoteException {
		// TODO Auto-generated method stub
		
	}

}
