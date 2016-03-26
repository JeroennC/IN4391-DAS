package das.message;
import server.Server;
import das.Client;
import das.Node_RMI;
import das.action.Action;


public class ActionMessage extends Message {
	private static final long serialVersionUID = 3743633525735683045L;
	private Action action;
	
	public ActionMessage(Client from, Address to, int to_id, Action a) {
		super(from, to, to_id);
		this.action = a;
	}

	@Override
	public void receive(Node_RMI node) {
		((Server) node).receiveActionMessage(this);
	}

	public Action getAction() {
		return action;
	}

}
