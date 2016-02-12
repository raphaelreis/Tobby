package roleplay.d2o;

import java.lang.reflect.Field;
import java.util.Vector;

import utilities.ByteArray;

public class GameDataClassDefinition {
	private Class<?> _class;
	private Vector<GameDataField> _fields;
	
	public GameDataClassDefinition(String str1, String str2) {
		try {
			this._class = Class.forName(getClass().getPackage().getName() + ".modules." + str2);
		} catch(Exception e) {
			e.printStackTrace();
		}
        this._fields = new Vector<GameDataField>();
	}
	
	public Vector<GameDataField> getFields() {
		return this._fields;
	}
	
	public Object read(String str, ByteArray array) {
		Object o = null;
		try {
			o = this._class.newInstance();
			for(GameDataField field : this._fields) {
				Field f = getField(o.getClass(), field.name);
				field.readData.setAccessible(true);
				f.set(o, field.readData.invoke(field, str, array, 0));
			}
			if(o instanceof IPostInit)
				((IPostInit) o).postInit();
		} catch(Exception e) {
			e.printStackTrace();
			System.exit(1);
		}
		return o;
	}
	
	public void addField(String str, ByteArray array) {
		GameDataField field = new GameDataField(str);
		field.readType(array);
		this._fields.add(field);
	}
	
	// fonction perso
	public Field getField(Class<?> c, String fieldName) {
		try {
			return c.getDeclaredField(fieldName);
		} catch (Exception e) {
			Class<?> superClass = c.getSuperclass();
			if(superClass != null)
				return getField(superClass, fieldName);
			e.printStackTrace();
			System.exit(1);
		}
		return null; // dead code
	}
}