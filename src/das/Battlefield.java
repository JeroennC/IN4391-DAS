package das;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import das.action.*;

public class Battlefield {
	public static final int MAP_WIDTH = 25;
	public static final int MAP_HEIGHT = 25;
	private Unit[][] map;
	private List<Unit> unitList;
	private int dragonCount;
	
	public Battlefield() {
		map = new Unit[MAP_WIDTH][MAP_HEIGHT];
		unitList = new ArrayList<Unit>();
		dragonCount = 0;
	}
	
	public Unit getUnit(int x, int y) {
		return map[x][y];
	}
	
	public Unit getUnit(Unit unit) {
		for (Unit u : unitList) {
			if (u.equals(unit))
				return u;
		}
		return null;
	}
	
	public Unit getUnit(int unit_id) {
		for (Unit u : unitList) {
			if (u.equals(unit_id))
				return u;
		}
		return null;
	}
	
	public void updateUnit(Unit u_active, Unit u_new) {
		if (map[u_active.getX()][u_active.getY()] == u_active) {
			map[u_active.getX()][u_active.getY()] = null;
		}
		u_active.setHp(u_new.getHp());
		u_active.setX(u_new.getX());
		u_active.setY(u_new.getY());
		map[u_active.getX()][u_active.getY()] = u_active;
	}
	
	public void placeUnit(Unit u) {
		map[u.getX()][u.getY()] = u;
		if (!u.isType()) dragonCount++;
		unitList.add(u);
	}
	
	public boolean moveUnit(Unit unit, MoveType move) {
		int destX = unit.getX();
		int destY = unit.getY();
		switch (move) {
			case Left:
				destX--;
				break;
			case Right:
				destX++;
				break;
			case Up:
				destY++;
				break;
			case Down:
				destY--;
				break;
		}
		// Check if move allowed
		if (!inBounds(destX, destY) || isOccupied(destX, destY)) return false;
		
		unit.setPosition(destX, destY);
		return true;
	}
	
	public boolean healUnit(Unit src, Unit dest) {
		// Must be able to reach and both units Players
		if (!src.canReach(dest) && src.isType() && dest.isType()) return false;
		
		dest.heal(src.getAp());
		
		return true;
	}
	
	public boolean attackUnit(Unit src, Unit dest) {
		// Must be able to reach and be different types
		if (!src.canReach(dest) && (src.isType() ^ dest.isType())) return false;
		
		dest.hurt(src.getAp());

		if (!dest.isAlive()) {
			killUnit(dest);
		}
		
		return true;
	}
	
	public void killUnit(Unit unit) {
		map[unit.getX()][unit.getY()] = null;
		if (!unit.isType()) dragonCount--;
		unitList.removeIf(u -> u.equals(unit));
		
		// TODO how does a server get this message to the clients?
	}
	
	public boolean isOccupied(int x, int y) {
		return map[x][y] != null;
	}
	
	public boolean inBounds(int x, int y) {
		return (x >= 0 && x < MAP_WIDTH) && (y >= 0 && y < MAP_HEIGHT);
	}
	
	public boolean hasDragons() {
		return dragonCount > 0;
	}
	
	public List<Unit> getSurroundingUnits(Unit unit) {
		List<Unit> result = new LinkedList<Unit>();
		int dist, destX, destY;
		for (int y = -2; y <= 2; y++) {
			for (int x = -2; x <= 2; x++) {
				// Distance can be no more than 2
				dist = Math.abs(x) + Math.abs(y);
				destX = unit.getX() + x;
				destY = unit.getY() + y;
				if (dist > 0 && dist <= 2 
						&& inBounds(destX, destY)) {
					if(isOccupied(destX, destY))
						result.add(getUnit(destX, destY));
				}
			}
		}
		
		return result;
	}
	
	public Unit getClosestDragon(Unit unit) {
		int dist = 1;
		int ux = unit.getX();
		int uy = unit.getY();
		int maxDist = Math.max(ux, MAP_WIDTH - ux) 
				+ Math.max(uy, MAP_HEIGHT - uy);
		int[] posneg = new int[]{-1, 1};
		int destx, desty;
		
		for (; dist <= maxDist; dist++) {
			for (int xtoy = 0; xtoy <= dist; xtoy++) {
				for(int xpn : posneg) {
					for (int ypn : posneg) {
						destx = ux + xpn * (dist - xtoy);
						desty = uy + ypn * (xtoy);
						if (inBounds(destx, desty) 
								&& isOccupied(destx, desty)
								&& !getUnit(destx, desty).isType()) {
							return getUnit(destx, desty);
						}
					}
				}
			}
		}
		
		return null;
	}
	
	public boolean isActionAllowed(Action action) {
		// Get unit
		Unit unit = getUnit(action.getExecuterId());
		if (!unit.isAlive()) return false;
		
		if (action instanceof Move) {
			// Dragons can't move
			if (!unit.isType()) return false;
			
			Move move = (Move)action;
			int destX = unit.getX();
			int destY = unit.getY();
			switch (move.getMoveType()) {
				case Left:
					destX--;
					break;
				case Right:
					destX++;
					break;
				case Up:
					destY++;
					break;
				case Down:
					destY--;
					break;
			}
			// Check if move allowed
			if (!inBounds(destX, destY) || isOccupied(destX, destY)) return false;
			return true;
		} else if (action instanceof Heal) {
			// Dragons can't heal
			if (!unit.isType()) return false;
			
			Heal heal = (Heal)action;
			Unit dest = getUnit(heal.getReceiverId());
			if (dest.isAlive() && dest.isType() && unit.canReach(dest)) return true;
			return false;
		} else if (action instanceof Hit) {
			Hit hit = (Hit)action;
			Unit dest = getUnit(hit.getReceiverId());
			// Must be opposite types, not dead
			if (dest.isAlive() && (dest.isType() ^ unit.isType())) return true;
			return false;
		}
		
		return false;
	}
	
	public Battlefield clone() {
		Battlefield bf = new Battlefield();
		
		this.unitList.forEach(unit -> {
			bf.placeUnit(unit.clone());
		});
		
		return bf;
	}
}
