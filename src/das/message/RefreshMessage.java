package das.message;

import java.util.List;

import das.Client;
import das.Node_RMI;
import das.Unit;
import das.server.Server;

public class RefreshMessage extends Message {
	private static final long serialVersionUID = -1988286774922872124L;
	private List<Unit> requestedUnits;
	
	public RefreshMessage(Client from, Address to, int to_id, List<Unit> requestedUnits) {
		super(from, to, to_id);
		this.requestedUnits = requestedUnits;
	}
	
	public List<Unit> getRequestedUnits() {
		return requestedUnits;
	}

	@Override
	public void receive(Node_RMI node) {
		((Server) node).receiveRefreshMessage(this);
	}
}
