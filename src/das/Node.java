package das;

import java.rmi.NoSuchObjectException;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;

public abstract class Node extends UnicastRemoteObject implements Node_RMI, Runnable {
	private static final long serialVersionUID = -3510258906366116527L;
	
	private int id;
	private String name;
	
	protected Node(int id, String name) throws RemoteException {
		super();
		this.id = id;
		this.name = name;
		Main.registerObject(this);
	}
	
	protected void close() {
		try {
			UnicastRemoteObject.unexportObject(this, true);
		} catch (NoSuchObjectException e) {
			e.printStackTrace();
		}
	}
	
	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}	
}
