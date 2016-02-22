package messages.character;

import messages.Message;
import utilities.ByteArray;

public class StatsUpgradeRequestMessage extends Message{
	public static final int protocolId = 5610;

	public boolean useAdditionnal = false;

	public int statId = 11;

	public int boostPoint = 0;

	public StatsUpgradeRequestMessage()
	{
		super();
	}

	public void serialize(int statId, int point) 
	{
		ByteArray buffer=new ByteArray();
		buffer.writeBoolean(this.useAdditionnal);
		buffer.writeByte(statId);
		buffer.writeVarShort(point);
		this.completeInfos(buffer);
	}

}