package das;

import java.rmi.NoSuchObjectException;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;

import das.Node.State;

public abstract class Node extends UnicastRemoteObject implements Node_RMI, Runnable {
	private static final long serialVersionUID = -3510258906366116527L;
	
	private int id;
	private String name;
	
	protected enum State { Disconnected, Initialization, Running, Inconsistent, Exit };
	protected volatile State state;
	
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
	
	public void changeState(State newState) {
		if (state != State.Exit) {
			state = newState;
		}
	}
	
	public void stop() {
		changeState(State.Exit);
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
