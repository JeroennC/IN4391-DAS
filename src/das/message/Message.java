package das.message;
import java.io.Serializable;
import java.net.MalformedURLException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.List;
import java.util.Queue;

import das.Client;
import das.Node;
import das.Node_RMI;
import das.Server;

public abstract class Message extends UnicastRemoteObject implements Serializable {
	private static final long serialVersionUID = 5970408991964088527L;
	public static final int FWD_COUNT = 3;
	public static final int port = 1003;
	
	private int message_id;
	private final Node_RMI from;
	private final Node_RMI receiver;
	private final String from_id;
	private final String receiver_id;
	private Message[] messageTail;
	
	
	public Message(Client from, int to_id) throws RemoteException {
		this(from, getComponent("Server_"+to_id), "Server_"+to_id);
	}
	
	public Message(Server from, String to_id) throws RemoteException {
		this(from, getComponent(to_id), to_id);
	}
	
	public Message(Node from, Node_RMI to, String id) throws RemoteException {
		super();
		this.from = from;
		this.receiver = to;
		this.from_id = from.getName();
		this.receiver_id = id;		
	}
	
	public void send() {
		new Thread() {
			  public void run() { 
				  try {
					receiver.receiveMessage(Message.this);
				} catch (RemoteException e) {
					e.printStackTrace();
				}
			  }
		}.start();
	}
	
	public void setID(int id) {
		message_id = id;
	}
	
	public int getID() {
		return message_id;
	}
	
	public void addTail(List<Message> list) {
		messageTail = (Message[]) list.toArray();
	}
	
	public abstract void receive(Node_RMI node);
	
	public static Node_RMI getComponent(String name){
		try {
			//TODO take into account that not every node is at localhost
			return (Node_RMI) java.rmi.Naming.lookup("rmi://localhost:"+port+"/"+name);
		} catch (MalformedURLException | RemoteException | NotBoundException e) {
			e.printStackTrace();
			return null;
		}
	}

}
