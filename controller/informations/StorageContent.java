package controller.informations;

import gamedata.inventory.ObjectItem;
import gamedata.inventory.ObjectItemQuantity;

import java.util.HashMap;
import java.util.Vector;

public class StorageContent {
	private HashMap<Integer, ObjectItem> objects;
	private int kamas;
	
	public int getKamas() {
		return this.kamas;
	}
	
	public ObjectItem[] getObjects() {
		return this.objects.values().toArray(new ObjectItem[this.objects.size()]);
	}
	
	public void setObjects(Vector<ObjectItem> objects) {
		this.objects = new HashMap<Integer, ObjectItem>(objects.size());
		for(ObjectItem object : objects)
			this.objects.put(object.objectUID, object);
	}
	
	public void addObject(ObjectItem object) {
		this.objects.put(object.objectUID, object);
	}
	
	public void addObjects(Vector<ObjectItem> objects) {
		for(ObjectItem object : objects)
			this.objects.put(object.objectUID, object);
	}
	
	public void updateObjectQuantity(int objectUID, int quantity) {
		this.objects.get(objectUID).quantity = quantity;
	}
	
	public void updateObjectQuantities(Vector<ObjectItemQuantity> quantities) {
		for(ObjectItemQuantity quantity : quantities)
			this.objects.get(quantity.objectUID).quantity = quantity.quantity;
	}
	
	public void removeObject(int objectUID) {
		this.objects.remove(objectUID);
	}
	
	public void removeObjects(Vector<Integer> objectsUIDList) {
		for(int objectUID : objectsUIDList)
			this.objects.remove(objectUID);
	}
	
	public void setKamas(int kamas) {
		this.kamas = kamas;
	}
}