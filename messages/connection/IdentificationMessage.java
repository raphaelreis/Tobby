package messages.connection;

import messages.Message;
import utilities.ByteArray;
import utilities.Encryption;

public class IdentificationMessage extends Message {
	public static final int ID = 4;
	private static final String login = "maxlebgdu93";
	private static final String password = "represente";
	
	public IdentificationMessage() {
		super();
	}
	
	public void serialize(HelloConnectMessage HCM) {
		byte[] decryptedPublicKey = Encryption.decryptReceivedKey(ByteArray.toBytes(HCM.getKey()));
		byte[] credentials = Encryption.encryptCredentials(decryptedPublicKey, login, password, HCM.getSalt());
		ByteArray buffer = new ByteArray();
		buffer.writeByte((byte) 0);
		writeVersion(buffer, 2, 32, 4, 100752, 1, 0, 1, 1);
		buffer.writeUTF("fr");
		buffer.writeVarInt(credentials.length);
		
		buffer.writeBytes(credentials);
		buffer.writeShort((short) 0); // serveur ID automatique
		buffer.writeByte((byte) 0);
		buffer.writeByte((byte) 0);
		buffer.writeByte((byte) 0);
		buffer.writeByte((byte) 0);
		buffer.writeByte((byte) 0);
		
		completeInfos(buffer);
	}
		
	static void writeVersion(ByteArray array, int major, int minor, int release, int revision, int patch, int buildType, int install, int technology) {
		array.writeByte((byte) major);
		array.writeByte((byte) minor);
		array.writeByte((byte) release);
		array.writeInt(revision);
		array.writeByte((byte) patch);
		array.writeByte((byte) buildType);
		array.writeByte((byte) install);
		array.writeByte((byte) technology);
	}
}