package das.action;

import das.Unit;

/**
 * Action for creating a new player on the battlefield
 */
public class NewPlayer extends Action {
	private static final long serialVersionUID = 3969153630095408957L;
	private volatile Unit newUnit;
	
	public NewPlayer(int executer_id) {
		super(executer_id);
	}

	public Unit getNewUnit() {
		if(newUnit != null)
			return newUnit.clone();
		return null;
	}

	public void setNewUnit(Unit newUnit) {
		this.newUnit = newUnit;
	}
	
	@Override
	public String toString() {
		return "[NewPlayer [executer_id=" + getExecuterId() +"]]";
	}
}
