package messages.interactions;

import messages.Message;

public class NpcGenericActionRequestMessage extends Message {
	public int npcId = 0;
	public int npcActionId = 0;
	public int npcMapId = 0;
	
	@Override
	public void serialize() {
		this.content.writeInt(this.npcId);
		this.content.writeByte(this.npcActionId);
		this.content.writeInt(this.npcMapId);
	}
	
	@Override
	public void deserialize() {
		// not implemented yet
	}
}