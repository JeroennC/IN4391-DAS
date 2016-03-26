package das.action;

import das.Unit;

public class NewPlayer extends Action {
	private static final long serialVersionUID = 3969153630095408957L;
	private Unit newUnit;
	
	public NewPlayer(int executer_id) {
		super(executer_id);
	}

	public Unit getNewUnit() {
		return newUnit;
	}

	public void setNewUnit(Unit newUnit) {
		this.newUnit = newUnit;
	}

}
