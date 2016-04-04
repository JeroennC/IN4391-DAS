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
	private Address receiverAddress;
	private final Node_RMI receiver;
	private final String from_id;
	private final String receiver_id;
	private Message[] messageTail;
	private long timestamp = 0;
	private List<Integer> acks;	
	
	public Message(Client from, Address to, int to_id) {
		this(from, getComponent(to, "Server_"+to_id), "Server_"+to_id);
		setReceiverAddress(to);
	}
	
	public Message(Node from, Address to, String to_id)  {
		this(from, getComponent(to, to_id), to_id);
		setReceiverAddress(to);
	}
	
	private Message(Node from, Node_RMI to, String id) {
		super();
		this.from = from;
		this.receiver = to;
		this.from_id = from.getName();
		this.fromAddress = from.getAddress();
		this.receiver_id = id;		
	}
	
	protected Message(Message m) {
		message_id = m.getID();
		fromAddress = m.getFromAddress();
		receiverAddress = m.getReceiverAddress();
		receiver = m.receiver;
		from_id = m.getFrom_id();
		from = m.getFrom();
		receiver_id = m.getReceiver_id();
		messageTail = m.messageTail;
		timestamp = m.getTimestamp();
		acks = m.getAcks();
	}
		
	public void send() {
		if(from instanceof Server && timestamp == 0)
			setTimestamp(((Server) from).getTime());
		from.Print("Sendmessage "+this);
		if(receiver == null)
			return;
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

	public Address getReceiverAddress() {
		return receiverAddress;
	}

	public void setReceiverAddress(Address receiverAddress) {
		this.receiverAddress = receiverAddress;
	}
	
	@Override
	public String toString() {
		return getClass().getName()+ "["+message_id+", " + from_id + " > " +receiver_id + "]";
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((from_id == null) ? 0 : from_id.hashCode());
		result = prime * result + message_id;
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Message other = (Message) obj;
		if (from_id == null) {
			if (other.from_id != null)
				return false;
		} else if (!from_id.equals(other.from_id))
			return false;
		if (message_id != other.message_id)
			return false;
		return true;
	}

}
