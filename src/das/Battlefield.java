package das;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

import das.action.*;
import das.message.Data;

public class Battlefield implements Serializable {
	private static final long serialVersionUID = -8734973792602687196L;
	
	public static final int MAP_WIDTH = 25;
	public static final int MAP_HEIGHT = 25;
	public static final int INITIAL_DRAGON_COUNT = 20;
	private Unit[][] map;
	private List<Unit> unitList;
	private int dragonCount;
	private int highestUnitId;
	
	public Battlefield() {
		map = new Unit[MAP_WIDTH][MAP_HEIGHT];
		unitList = new ArrayList<Unit>();
		dragonCount = 0;
		highestUnitId = 0;
	}
	
	public void initialize() {
		map = new Unit[MAP_WIDTH][MAP_HEIGHT];
		unitList = new ArrayList<Unit>();
		dragonCount = 0;
		highestUnitId = 0;
		
		// Place dragons
		Random rand = new Random(System.nanoTime());
		boolean placed;
		for (int i = 0; i < INITIAL_DRAGON_COUNT; i++) {
			int hp = rand.nextInt(51) + 50;
			int ap = rand.nextInt(16) + 5;
			placed = false;
			// 20 tries of placement
			for (int j = 0; j < 20; j++ ){
				int x = rand.nextInt(MAP_WIDTH);
				int y = rand.nextInt(MAP_HEIGHT);
				if (isOccupied(x, y)) continue;
				
				placeUnit(new Unit(highestUnitId++, x, y, hp, ap, false));
				placed = true;
				break;
			}
			if (placed) continue;
			// If still not placed, put it in the first available from rand point
			int x = rand.nextInt(MAP_WIDTH);
			int y = rand.nextInt(MAP_HEIGHT);
			while (true) {
				if (!isOccupied(x,y)) {
					placeUnit(new Unit(highestUnitId++, x, y, hp, ap, false));
					break;
				}
				x++;
				if (x > MAP_WIDTH - 1) {
					x = 0;
					y++;
					if (y > MAP_HEIGHT - 1)
						y = 0;
				}
			}
		}
	}
	
	public Unit createAndPlaceNewPlayer(int newPlayerId) {
		// TODO Check if board is not full?
		Random rand = new Random(System.nanoTime());
		Unit u;
		int hp = rand.nextInt(11) + 10;
		int ap = rand.nextInt(10) + 1;
		// 20 tries of placement
		for (int j = 0; j < 20; j++ ){
			int x = rand.nextInt(MAP_WIDTH);
			int y = rand.nextInt(MAP_HEIGHT);
			if (isOccupied(x, y)) continue;
			
			u = new Unit(newPlayerId, x, y, hp, ap, true);
			highestUnitId = Math.max(highestUnitId, newPlayerId);
			placeUnit(u);
			highestUnitId++;
			return u;
		}
		// If still not placed, put it in the first available from rand point
		int x = rand.nextInt(MAP_WIDTH);
		int y = rand.nextInt(MAP_HEIGHT);
		while (true) {
			if (!isOccupied(x,y)) {
				u = new Unit(newPlayerId, x, y, hp, ap, true);
				highestUnitId = Math.max(highestUnitId, newPlayerId+1);
				placeUnit(u);
				break;
			}
			x++;
			if (x > MAP_WIDTH - 1) {
				x = 0;
				y++;
				if (y > MAP_HEIGHT - 1)
					y = 0;
			}
		}
		highestUnitId++;
		return u;
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
		
		if (u.getId() >= highestUnitId) {
			highestUnitId = u.getId() + 1;
		}
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
		map[unit.getX()][unit.getY()] = null;
		unit.setPosition(destX, destY);
		map[destX][destY] = unit;
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
		if (action instanceof NewPlayer) {
			return getUnit(action.getExecuterId()) == null;
		}
		// Get unit
		Unit unit = getUnit(action.getExecuterId());
		if (unit == null || !unit.isAlive()) return false;
		
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
			if (dest != null && dest.isAlive() && dest.isType() && unit.canReach(dest)) return true;
			return false;
		} else if (action instanceof Hit) {
			Hit hit = (Hit)action;
			Unit dest = getUnit(hit.getReceiverId());
			// Must be opposite types, not dead
			if (dest != null && dest.isAlive() && (dest.isType() ^ unit.isType())) return true;
			return false;
		}
		return false;
	}
	
	public Unit doAction(Action action) {
		if (action instanceof NewPlayer) {
			NewPlayer newPlayer = (NewPlayer) action;
			Unit newUnit = newPlayer.getNewUnit();
			if(newUnit == null) {
				newUnit = createAndPlaceNewPlayer(action.getExecuterId());
				newPlayer.setNewUnit(newUnit);
			} else
					placeUnit(newUnit);
			return newUnit;
		}
		Unit unit = getUnit(action.getExecuterId());		
		if (action instanceof Move) {	
			moveUnit(unit, ((Move) action).getMoveType());
		} else if (action instanceof Heal) {
			healUnit(unit, getUnit(((Heal) action).getReceiverId()));
		} else if (action instanceof Hit) {
			attackUnit(unit, getUnit(((Hit) action).getReceiverId()));
		} 
		return unit;
	}
	
	public List<Unit> getUnitList() {
		List<Unit> result = new ArrayList<Unit>();
		this.unitList.forEach(unit -> {
			result.add(unit.clone());
		});
		return result;
	}
	
	public synchronized Battlefield clone() {
		Battlefield bf = new Battlefield();
		
		this.unitList.forEach(unit -> {
			bf.placeUnit(unit.clone());
		});
		//bf.dragonCount = this.dragonCount;
		//bf.highestUnitId = this.highestUnitId;
		return bf;
	}
	
	public int getNextUnitId() {
		return highestUnitId;
	}
	
	public Data difference(Battlefield bf) {
		Data d = new Data();
		for(Unit u1: unitList) {
			Unit u2 = bf.getUnit(u1);
			if(u2 == null) {
				d.deleteUnit(u1.getId());
				continue;
			}
			if(u1.getX() != u2.getX() || u1.getY() != u2.getY() || u1.getHp() != u2.getHp())
				d.updateUnit(u1);
		}
		for(Unit u2: bf.getUnitList()) {
			Unit u1 = bf.getUnit(u2);
			if(u1 == null)
				d.updateUnit(u2);
		}
		return d;
	}
	
	public Data getData() {
		Data d = new Data();
		d.setUpdatedUnits(unitList);
		return d;
	}
}
