package das.message;
import java.io.Serializable;
import java.net.MalformedURLException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.util.List;

import das.Client;
import das.Node;
import das.Node_RMI;
import das.server.Server;

public abstract class Message implements Serializable {
	private static final long serialVersionUID = 5970408991964088527L;
	public static final int FWD_COUNT = 3;
	
	private int message_id;
	private transient final Node from;
	private Address fromAddress;
	private final Node_RMI receiver;
	private final String from_id;
	private final String receiver_id;
	private Message[] messageTail;
	private long timestamp;
	private List<Integer> acks;	
	
	public Message(Client from, Address to, int to_id) {
		this(from, getComponent(to, "Server_"+to_id), "Server_"+to_id);
	}
	
	public Message(Node from, Address to, String to_id)  {
		this(from, getComponent(to, to_id), to_id);
	}
	
	public Message(Node from, Node_RMI to, String id) {
		super();
		this.from = from;
		this.receiver = to;
		this.from_id = from.getName();
		this.fromAddress = from.getAddress();
		this.receiver_id = id;		
	}
		
	public void send() {
		if(from instanceof Server)
			setTimestamp(((Server) from).getTime());
		if(receiver == null)
			return;
		from.Print("Sendmessage "+this);
		new Thread() {
			  public void run() { 
				  try {
					receiver.receiveMessage(Message.this);
				} catch (RemoteException e) { 
					e.printStackTrace();
				} //Do nothing (simulate Time-out)
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
		messageTail = (Message[]) list.toArray(new Message[list.size()]);
	}
	
	public abstract void receive(Node_RMI node);
	
	public static Node_RMI getComponent(Address address, String name){
		try {
			return (Node_RMI) java.rmi.Naming.lookup("rmi://"+address.getAddress()+":"+address.getPort()+"/"+name);
		} catch (MalformedURLException | RemoteException | NotBoundException e) {
			return null;
		}
	}

	public long getTimestamp() {
		return timestamp;
	}

	public void setTimestamp(long timestamp) {
		this.timestamp = timestamp;
	}

	public String getFrom_id() {
		return from_id;
	}
	
	public String getReceiver_id() {
		return receiver_id;
	}
	
	public Node getFrom() {
		return from;
	}

	public Address getFromAddress() {
		return fromAddress;
	}

	public List<Integer> getAcks() {
		return acks;
	}

	public void setAcks(List<Integer> acks) {
		this.acks = acks;
	}

}
