package network;

import java.util.LinkedList;

import messages.NetworkMessage;
import utilities.ByteArray;

class Reader {
	private NetworkMessage incompleteMsg; // message incomplet qui attend d'�tre compl�t�
	private byte[] incompleteHeader; // header incomplet qui attend d'�tre compl�t�
	
	protected LinkedList<NetworkMessage> processBuffer(ByteArray buffer) {
		LinkedList<NetworkMessage> msgStack = new LinkedList<NetworkMessage>();
		if(this.incompleteHeader != null) {
			buffer.appendBefore(this.incompleteHeader);
			this.incompleteHeader = null;
		}
		else if(this.incompleteMsg != null) {	
			buffer.setPos(this.incompleteMsg.appendContent(buffer.bytes()));
			if(this.incompleteMsg.isComplete()) {
				msgStack.add(this.incompleteMsg);
				this.incompleteMsg = null;
			}
			else
				return msgStack; // si le message est incomplet, cela signifie qu'il n'y a plus rien � lire dans le buffer
		}
		while(!buffer.endOfArray()) {
			NetworkMessage msg = extractMsgFromBuffer(buffer.bytesFromPos());
			if(msg == null) { // header incomplet
				this.incompleteHeader = buffer.bytesFromPos();
				break;
			}
			else if(msg.isComplete()) // message complet
				msgStack.add(msg);
			else { // message incomplet
				this.incompleteMsg = msg;
				this.incompleteMsg.setPosToMax();
			}
			buffer.readBytes(msg.getTotalSize());
		}
		return msgStack;
	}
	
	private static NetworkMessage extractMsgFromBuffer(byte[] buffer) {
		if(buffer.length < 2)
			return null;
		char[] cbuffer = new char[buffer.length]; // �tant donn� que ce sont des octets sign�s bruts
		for(int i = 0; i < buffer.length; ++i)
			cbuffer[i] = (char) (buffer[i] & 0xFF);
		
		int header = cbuffer[0] << 8 | cbuffer[1];
		short id = (short) (header >> 2);
		short lenofsize = (short) (header & 3);
		if(buffer.length < 2 + lenofsize)
			return null;
		int size;
		if(lenofsize == 0)
	        size = 0;
	    else if(lenofsize == 1)
	        size = cbuffer[2];
	    else if(lenofsize == 2)
	        size = cbuffer[2] << 8 | cbuffer[3];
	    else // lenofsize = 3
	        size = (cbuffer[2] << 16 | cbuffer[3] << 8) | cbuffer[4];	
		int bytesAvailable = size > buffer.length - 2 - lenofsize ? buffer.length - 2 - lenofsize : size;
		byte[] content = new byte[bytesAvailable];
		int counter = 0;
		for(int i = 2 + lenofsize; i < bytesAvailable + 2 + lenofsize; ++i, ++counter)
			content[counter] = buffer[i];
	    return NetworkMessage.create(id, lenofsize, size, content, counter);		
	}
}