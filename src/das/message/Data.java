package das.message;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import das.Unit;


public class Data implements Serializable {
	private static final long serialVersionUID = -3503781888964931670L;
	
	private List<Unit> updatedUnits;
	private List<Integer> deletedUnits;
	private Unit player;

	public Data() {
		player = null;
		updatedUnits = new ArrayList<Unit>();
		deletedUnits = new ArrayList<Integer>();
	}
	
	public void deleteUnit(int id) {
		if(!deletedUnits.contains(id)) 
			deletedUnits.add(id);
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

	@Override
	public String toString() {
		return "Data [updatedUnits=" + updatedUnits + ", deletedUnits="
				+ deletedUnits + ", player=" + player + "]";
	}

	public Unit getPlayer() {
		return player;
	}

	public void setPlayer(Unit player) {
		this.player = player;
	}

	public List<Integer> getDeletedUnits() {
		return deletedUnits;
	}

	public void setDeletedUnits(List<Integer> deletedUnits) {
		this.deletedUnits = deletedUnits;
	}
	
	public Data clone() {
		Data d = new Data();
		d.player = this.player;
		d.deletedUnits = new ArrayList<Integer>(this.deletedUnits);
		d.updatedUnits = new ArrayList<Unit>();
		for(Unit u: this.updatedUnits)
			d.updatedUnits.add(u.clone());
		return d;
	}
}
