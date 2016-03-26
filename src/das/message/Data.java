package das.message;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import das.Unit;


public class Data implements Serializable {
	private static final long serialVersionUID = -3503781888964931670L;
	
	private List<Unit> updatedUnits;
	private Unit player;

	public Data() {
		player = null;
		updatedUnits = new ArrayList<Unit>();
	}
	
	public void updateUnit(Unit u) {
		if(updatedUnits.contains(u)) {
			updatedUnits.remove(u);
		}
		updatedUnits.add(u);
	}
	
	public List<Unit> getUpdatedUnits() {
		return updatedUnits;
	}

	public void setUpdatedUnits(List<Unit> updatedUnits) {
		this.updatedUnits = updatedUnits;
	}

	public Unit getPlayer() {
		return player;
	}

	public void setPlayer(Unit player) {
		this.player = player;
	}
}
