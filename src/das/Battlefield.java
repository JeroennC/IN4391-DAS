package das;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

import das.action.*;
import das.message.Data;

/**
 * The in-game battlefield that the players interact with
 *
 */
public class Battlefield implements Serializable {
	private static final long serialVersionUID = -8734973792602687196L;
	
	public static final int MAP_WIDTH = 25;
	public static final int MAP_HEIGHT = 25;
	public static final int INITIAL_DRAGON_COUNT = 20;
	private Unit[][] map;
	private List<Unit> unitList;
	private List<Unit> deletedUnits;
	private int dragonCount;
	private int highestUnitId;
	
	public Battlefield() {
		map = new Unit[MAP_WIDTH][MAP_HEIGHT];
		unitList = new ArrayList<Unit>();
		deletedUnits = new LinkedList<Unit>();
		dragonCount = 0;
		highestUnitId = 0;
	}
	
	/**
	 * Initialize the battlefield, spawn new dragons
	 */
	public void initialize() {
		map = new Unit[MAP_WIDTH][MAP_HEIGHT];
		unitList = new ArrayList<Unit>();
		deletedUnits = new LinkedList<Unit>();
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
	
	/**
	 * Find a random spot for a new player and place it there
	 */
	public Unit createAndPlaceNewPlayer(int newPlayerId) {
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
	
	/**
	 * Retrieve the unit on position (x,y)
	 */
	public Unit getUnit(int x, int y) {
		return map[x][y];
	}
	
	/**
	 * Retrieve the unit with equal unit_id
	 */
	public Unit getUnit(Unit unit) {
		for (Unit u : unitList) {
			if (u.equals(unit))
				return u;
		}
		return null;
	}
	
	/**
	 * Retrieve the unit with equal unit_id
	 */
	public Unit getUnit(int unit_id) {
		for (Unit u : unitList) {
			if (u.equals(unit_id))
				return u;
		}
		return null;
	}
	
	/**
	 * Update the active unit with the variables of the new unit
	 */
	public void updateUnit(Unit u_active, Unit u_new) {
		if (map[u_active.getX()][u_active.getY()] == u_active) {
			map[u_active.getX()][u_active.getY()] = null;
		}
		u_active.setHp(u_new.getHp());
		u_active.setX(u_new.getX());
		u_active.setY(u_new.getY());
		u_active.setTimestamp(u_new.getTimestamp());
		map[u_active.getX()][u_active.getY()] = u_active;
		if (!u_active.isAlive())
			killUnit(u_active);
	}
	
	/**
	 * Place a unit on the battlefield
	 */
	public void placeUnit(Unit u) {
		map[u.getX()][u.getY()] = u;
		if (!u.isType()) dragonCount++;
		unitList.add(u);
		deletedUnits.removeIf(unit -> u.equals(unit));
		
		if (u.getId() >= highestUnitId) {
			highestUnitId = u.getId() + 1;
		}
	}
	
	/**
	 * Move a unit in the MoveType direction
	 */
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
	
	/**
	 * Heal unit dest, with src's ap
	 */
	public boolean healUnit(Unit src, Unit dest) {
		// Must be able to reach and both units Players
		if (!src.canReach(dest) && src.isType() && dest.isType()) return false;
		
		dest.heal(src.getAp());
		
		return true;
	}
	
	/**
	 * Hit unit dest, with src's ap
	 */
	public boolean attackUnit(Unit src, Unit dest) {
		// Must be able to reach and be different types
		if (!src.canReach(dest) && (src.isType() ^ dest.isType())) return false;
		
		dest.hurt(src.getAp());

		if (!dest.isAlive()) {
			killUnit(dest);
		}
		
		return true;
	}
	
	/**
	 * Remove unit from the battlefield
	 */
	public void killUnit(Unit unit) {
		if (unit.getHp() > 0)
			unit.setHp(0);
		map[unit.getX()][unit.getY()] = null;
		if (!unit.isType()) dragonCount--;
		unitList.removeIf(u -> u.equals(unit));
		deletedUnits.removeIf(u -> u.equals(unit));
		deletedUnits.add(unit);
	}
	
	/**
	 * Returns whether (x,y) is occupied with a unit
	 */
	public boolean isOccupied(int x, int y) {
		return map[x][y] != null;
	}
	
	/**
	 * Returns whether (x,y) is in map bounds
	 */
	public boolean inBounds(int x, int y) {
		return (x >= 0 && x < MAP_WIDTH) && (y >= 0 && y < MAP_HEIGHT);
	}
	
	/**
	 * Check if there are dragons on the battlefield
	 */
	public boolean hasDragons() {
		return dragonCount > 0;
	}
	
	/**
	 * Get all reachable units for a unit (within 2 squares)
	 */
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
	
	/**
	 * Returns the closest dragon from unit
	 */
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
	
	/**
	 * Returns the closest human from unit
	 */
	public Unit getClosestPlayer(Unit unit) {
		int dist = 1;
		int ux = unit.getX();
		int uy = unit.getY();
		int maxDist = 2;
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
								&& getUnit(destx, desty).isHuman()) {
							return getUnit(destx, desty);
						}
					}
				}
			}
		}
		
		return null;
	}
	
	/**
	 * Checks if an action is possible/allowed
	 */
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
		} else if(action instanceof DeletePlayer)
			return true;
		return false;
	}
	
	/**
	 * Execute an action on the battlefield
	 */
	public Unit doAction(Action action) {
		if (action instanceof NewPlayer) {
			NewPlayer newPlayer = (NewPlayer) action;
			Unit newUnit = newPlayer.getNewUnit();
			if(newUnit == null) {
				newUnit = createAndPlaceNewPlayer(action.getExecuterId());
				newPlayer.setNewUnit(newUnit.clone());
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
		} else if (action instanceof DeletePlayer) {
			killUnit(unit);
			return null;
		}
		return unit;
	}
	
	/**
	 * Returns a deep clone of the list of units
	 */
	public List<Unit> getUnitList() {
		List<Unit> result = new ArrayList<Unit>();
		this.unitList.forEach(unit -> { 
			if(unit != null)
				result.add(unit.clone());
		});
		return result;
	}
	
	/**
	 * Returns a deep clone of the battlefield
	 */
	public synchronized Battlefield clone() {
		Battlefield bf = new Battlefield();
		
		this.unitList.forEach(unit -> {
			bf.placeUnit(unit.clone());
		});

		return bf;
	}
	
	/**
	 * Return the next unit id
	 */
	public int getNextUnitId() {
		return highestUnitId;
	}
	
	/**
	 * Calculates the difference in units between two battlefields
	 */
	public synchronized Data difference(Battlefield bf) {
		Data d = new Data();
		synchronized(bf) {
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
		}
		return d;
	}
	
	/**
	 * Returns all active unit information
	 */
	public Data getData() {
		Data d = new Data();
		d.setUpdatedUnits(unitList);
		return d;
	}
	
	/**
	 * Gets a deleted unit
	 */
	public Unit getDeletedUnit(int unit_id) {
		for (Unit u : deletedUnits)
			if (u.equals(unit_id))
				return u;
		
		return null;
	}
}
