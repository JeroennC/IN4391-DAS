package das;
import java.io.Serializable;


public class Unit implements Serializable {
	private static final long serialVersionUID = 1128476239780761247L;
	private static final int reachDistance = 2;
	
	private int id;
	private int x;
	private int y;
	private int hp;
	private int ap;
	private int maxHp;
	private boolean alive = true;
	
	//TODO subclasses for dragon and player
	private boolean type;
	
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
		this.x = x;
		this.y = y;
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
	public void setType(boolean type) {
		this.type = type;
	}
	public boolean isAlive() {
		return alive;
	}
	public int getId() {
		return id;
	}
	public boolean canReach(Unit other) {
		return Math.abs(this.x - other.x) + Math.abs(this.y - other.y) 
			<= reachDistance;
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
}
