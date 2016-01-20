package messages.synchronisation;

import utilities.ByteArray;
import messages.Message;

public class SequenceNumberMessage extends Message {
	private static int number;
	
	public SequenceNumberMessage() {
		super();
	}
	
	public void serialize() {
		ByteArray buffer = new ByteArray();
		buffer.writeShort((short) number++);
		
		this.size = 2;
		this.lenofsize = 1;
		this.content = buffer.bytes();
	}
}
