package das;
import java.io.Serializable;


public class Unit implements Serializable {
	private static final long serialVersionUID = 1128476239780761247L;
	private static final int reachDistance = 2;
	
	private int id;
	private volatile int x;
	private volatile int y;
	private volatile int hp;
	private int ap;
	private int maxHp;
	private volatile boolean alive = true;
	
	// true if human, false if dragon
	private boolean type;
	
	public Unit() { }
	
	public Unit(int id, int x, int y, int hp, int ap, boolean type) {
		this.id = id;
		this.x = x;
		this.y = y;
		this.hp = hp;
		this.ap = ap;
		this.maxHp = hp;
		this.type = type;
	}
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + id;
		return result;
	}
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (obj instanceof Integer)
			return id == (int)obj;
		if (getClass() != obj.getClass())
			return false;
		Unit other = (Unit) obj;
		if (id != other.id)
			return false;
		return true;
	}
	
	public int getX() {
		return x;
	}
	public void setX(int x) {
		this.x = x;
	}
	public int getY() {
		return y;
	}
	public void setY(int y) {
		this.y = y;
	}
	public void setPosition(int x, int y) {
		setX(x);
		setY(y);
	}
	
	@Override
	public String toString() {
		return "Unit [id=" + id + ", x=" + x + ", y=" + y + ", hp=" + hp
				+ ", ap=" + ap + ", maxHp=" + maxHp + ", alive=" + alive
				+ ", type=" + type + "]";
	}

	public int getHp() {
		return hp;
	}
	public void setHp(int hp) {
		this.hp = hp;
	}
	public int getAp() {
		return ap;
	}
	public void setAp(int ap) {
		this.ap = ap;
	}
	public int getMaxHp() {
		return maxHp;
	}
	public void setMaxHp(int hp) {
		this.maxHp = hp;
	}
	public boolean isType() {
		return type;
	}
	public boolean isHuman() {
		return type;
	}
	public boolean isDragon() {
		return !type;
	}
	public void setType(boolean type) {
		this.type = type;
	}
	public void setHuman(boolean human) {
		this.type = human;
	}
	public void setDragon(boolean dragon) {
		this.type = !dragon;
	}
	public boolean isAlive() {
		return alive;
	}
	public int getId() {
		return id;
	}
	public void setId(int id) {
		this.id = id;
	}
	public int distanceTo(int x, int y) {
		return Math.abs(this.x - x) + Math.abs(this.y - y);
	}
	public boolean canReach(Unit other) {
		return distanceTo(other.x, other.y) <= reachDistance;
	}
	public boolean needsHealing() {
		return hp * 1.0 / maxHp < 0.5;
	}
	public void heal(int hp) {
		this.hp += hp;
		hp = hp > maxHp ? maxHp : hp;
	}
	public void hurt(int ap) {
		this.hp -= ap;
		if (this.hp <= 0) {
			this.hp = 0;
			alive = false;
		}
	}
	public Unit clone() {
		Unit u = new Unit();
		
		u.alive = this.alive;
		u.ap = this.ap;
		u.hp = this.hp;
		u.id = this.id;
		u.maxHp = this.maxHp;
		u.type = this.type;
		u.x = this.x;
		u.y = this.y;
		
		return u;
	}
}
