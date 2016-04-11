package das.message;
import java.util.List;

import das.Client;
import das.Node;
import das.Node_RMI;
import das.action.Action;
import das.action.NewPlayer;
import das.server.Server;


public class ActionMessage extends Message {
	private static final long serialVersionUID = 3743633525735683045L;
	private Action action;
	
	public ActionMessage(InitMessage m, int newPlayerId) {
		super(m);
		action = new NewPlayer(newPlayerId);
	}
	
	public ActionMessage(Client from, Address to, int to_id, Action a) {
		super(from, to, to_id);
		this.action = a;
	}
	
	public ActionMessage(Server from, Action a) {
		super(from);
		this.action = a;
	}

	public ActionMessage(Server from, Address to, String to_id, Action a) {
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
