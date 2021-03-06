package gamedata.d2o.modules;

import gamedata.d2i.I18n;
import gamedata.d2o.GameData;
import gamedata.d2o.GameDataFileAccessor;

import java.util.Arrays;
import java.util.Vector;

public class WorldMap {
	public static final String MODULE = "WorldMaps";
	
    static {
    	GameDataFileAccessor.getInstance().init(MODULE);
    }

	public int id;
	public int nameId;
	public int origineX;
	public int origineY;
	public double mapWidth;
	public double mapHeight;
	public int horizontalChunck;
	public int verticalChunck;
	public boolean viewableEverywhere;
	public double minScale;
	public double maxScale;
	public double startScale;
	public int centerX;
	public int centerY;
	public int totalWidth;
	public int totalHeight;
	public Vector<String> zoom;
	private String _name;

	public static WorldMap getWorldMapById(int id) {
		return (WorldMap) GameData.getObject(MODULE, id);
	}

	public WorldMap[] getAllWorldMaps() {
		Object[] objArray = GameData.getObjects(MODULE);
		return Arrays.copyOf(objArray, objArray.length, WorldMap[].class);
	}

	public String getName() {
		if(this._name == null)
			this._name = I18n.getText(this.nameId);
		return this._name;
	}
}