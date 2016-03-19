package das.message;
import das.Client;
import das.Node_RMI;
import das.Server;


public class DataMessage extends Message {
	private static final long serialVersionUID = -8814316082059816216L;
	private Data data;
	private int datamessage_id;
	private int actionMessage_id;

	public DataMessage(Server from, String to_id, Data d, int actionMessage_id, int datamessage_id) {
		super(from, to_id);
		this.setData(d);
		this.actionMessage_id = actionMessage_id;
		this.datamessage_id = datamessage_id;
	}

	@Override
	public void receive(Node_RMI node) {
		((Client) node).receiveData(this);

	}

	public int getDatamessage_id() {
		return datamessage_id;
	}

	public void setDatamessage_id(int datamessage_id) {
		this.datamessage_id = datamessage_id;
	}

	public Data getData() {
		return data;
	}

	public void setData(Data data) {
		this.data = data;
	}

	public int getActionMessage_id() {
		return actionMessage_id;
	}

	public void setActionMessage_id(int actionMessage_id) {
		this.actionMessage_id = actionMessage_id;
	}

}
