package das.message;
import das.Action;
import das.Client;
import das.Node_RMI;
import das.Server;


public class ActionMessage extends Message {
	private static final long serialVersionUID = 3743633525735683045L;
	private Action action;
	
	public ActionMessage(Client from, int to, Action a) {
		super(from, to);
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
