package das.action;

/**
 * Action for hitting a player on the battlefield
 */
public class Hit extends Action {
	private static final long serialVersionUID = -5433425456504865747L;
	
	private int receiver_id;
	
	public Hit (int executer_id, int receiver_id) {
		super(executer_id);
		this.receiver_id = receiver_id;
	}
	
	public int getReceiverId() {
		return receiver_id;
	}
	
	@Override
	public String toString() {
		return "[Hit [executer_id=" + getExecuterId() + " receiver_id=" + getReceiverId() +"]]";
	}
}
