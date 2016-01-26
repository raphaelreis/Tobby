package movement;

import movement.ankama.Map;
import utilities.ByteArray;

public class Cell {
	public int id;
	public int speed;
	public int mapChangeData;
	public int moveZone;
	private int _losmov = 3;
	private int _floor;
	private Map _map;
	private int _arrow = 0;
	private boolean _mov;
	private boolean _los;
	private boolean _nonWalkableDuringFight;
	private boolean _red;
	private boolean _blue;
	private boolean _farmCell;
	private boolean _visible;
	private boolean _nonWalkableDuringRP;
	
	protected int x;
	protected int y;
	protected boolean visited;
	
	public Cell(Map map, int id) {
		this._map = map;
		this.id = id;
		this.visited = false;
		this.x = id % Map.WIDTH;
		this.y = id / Map.WIDTH;
	}
	
	public Map getMap() {
		return this._map;
	}
	
	public boolean getMov() {
		return this._mov;
	}
	
	public boolean getLos() {
		return this._los;
	}
	
	public boolean getNonWalkableDuringFight() {
		return this._nonWalkableDuringFight;
	}
	
	public boolean getRed() {
		return this._red;
	}
	
	public boolean getBlue() {
		return this._blue;
	}
	
	public boolean getFarmCell() {
		return this._farmCell;
	}
	
	public boolean getVisible() {
		return this._visible;
	}
	
	public boolean getNonWalkableDuringRP() {
		return this._nonWalkableDuringRP;
	}
	
	public int getFloor() {
		return this._floor;
	}
	
	public boolean getUseTopArrow() {
		return !((this._arrow & 1) == 0);
	}
	
	public boolean getUseBottomArrow() {
		return !((this._arrow & 2) == 0);
	}
	
	public boolean getUseRightArrow() {
		return !((this._arrow & 4) == 0);
	}
	
	public boolean getUseLeftArrow() {
		return !((this._arrow & 8) == 0);
	}
	
	public void fromRaw(ByteArray raw) {
		this._floor = raw.readByte() * 10;
		if(this._floor == -1280)
			return;
		this._losmov = raw.readByte();
		this.speed = raw.readByte();
		this.mapChangeData = raw.readByte();
		if(this._map.mapVersion > 5)
			this.moveZone = raw.readByte();
		if(this._map.mapVersion > 7) {
			this._arrow = 15 & raw.readByte();
			if(this.getUseTopArrow())
				this._map.topArrowCell.add(this.id);
			if(this.getUseBottomArrow())
				this._map.bottomArrowCell.add(this.id);
			if(this.getUseLeftArrow())
				this._map.leftArrowCell.add(this.id);
			if(this.getUseRightArrow())
				this._map.rightArrowCell.add(this.id);
		}
		this._los = ((this._losmov & 2) >> 1) == 1;
		this._mov = (this._losmov & 1) == 1;
		this._visible = ((this._losmov & 64) >> 6) == 1;
		this._farmCell = ((this._losmov & 32) >> 5) == 1;
	    this._blue = ((this._losmov & 16) >> 4) == 1;
	    this._red = ((this._losmov & 8) >> 3) == 1;
	    this._nonWalkableDuringRP = ((this._losmov & 128) >> 7) == 1;
	    this._nonWalkableDuringFight = ((this._losmov & 4) >> 2) == 1;	
	}
	
	public boolean equals(Cell cell) {
		return this.x == cell.x && this.y == cell.y;
	}
	
	public boolean check() { // to do
		boolean isVisited = this.visited;
		this.visited = true;
		return !isVisited && !this._nonWalkableDuringRP && _mov;
	}
	
	public String toString() {
		return "[" + this.x + ", " + this.y + "]";
	}
}
