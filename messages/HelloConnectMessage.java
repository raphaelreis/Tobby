package messages;

import utilities.ByteArray;

public class HelloConnectMessage extends Message {
	public static final int ID = 3;
	private String salt;
	private int[] key;
	
	public HelloConnectMessage(ReceivedMessage msg) {
		super(ID, msg.getLenOfSize(), msg.getSize(), msg.getContent());
		
		deserialize();
	}
	
	private void deserialize() {
		ByteArray buffer = new ByteArray(this.content);
		this.salt = buffer.readUTF();
		int keySize = buffer.readVarInt();
		this.key = new int[keySize];
		for(int i = 0; i < keySize; ++i)
			this.key[i] = buffer.readByte();
	}
	
	public void serialize() { // pour �mulation
		ByteArray buffer = new ByteArray();
		buffer.writeUTF(this.salt);
		int len = this.key.length;
		buffer.writeVarInt(len);
		for(int i = 0; i < len; ++i)
			buffer.writeByte((byte) this.key[i]);
		
		this.size = buffer.getSize();
		this.lenofsize = computeLenOfSize(this.size);
		this.content = buffer.bytes();
	}
	
	public String getSalt() {
		return this.salt;
	}
	
	public int[] getKey() {
		return this.key;
	}
}