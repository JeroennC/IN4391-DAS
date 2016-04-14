package das.message;
import das.Client;
import das.Node_RMI;
import das.server.Server;


public class DataMessage extends Message {
	private static final long serialVersionUID = -8814316082059816216L;
	private Data data;
	private int datamessage_id;
	private int actionMessage_id;

	public DataMessage(Server from, Address to, String to_id, Data d, int actionMessage_id, int datamessage_id) {
		super(from, to, to_id);
		this.setData(d);
		this.actionMessage_id = actionMessage_id;
		this.datamessage_id = datamessage_id;
	}

	@Override
	public void receive(Node_RMI node) {
		((Client) node).receiveData(this);

	}

	public synchronized int getDatamessage_id() {
		return datamessage_id;
	}

	public synchronized void setDatamessage_id(int datamessage_id) {
		this.datamessage_id = datamessage_id;
	}

	public synchronized Data getData() {
		return data;
	}

	public synchronized void setData(Data data) {
		this.data = data;
	}

	public synchronized int getActionMessage_id() {
		return actionMessage_id;
	}

	public synchronized void setActionMessage_id(int actionMessage_id) {
		this.actionMessage_id = actionMessage_id;
	}
	
	@Override
	public synchronized String toString() {
		return super.toString() + "{"+getDatamessage_id()+"}";
	}
}
