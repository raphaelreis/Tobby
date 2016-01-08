package main;

import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.ByteBuffer;

import messages.Message;

public class Sender implements Runnable {
	private static Sender sender = null;
	private Socket socket;
	private byte[] toSend;
	
	private Sender(Socket socket) {
		this.socket = socket;
	}
	
	public static void create(Socket socket) {			
		sender = new Sender(socket);
	}

	public static Sender getInstance() {
		return sender;
	}
	
	public void send(Message msg) {
		byte[] header = makeHeader(msg);
		toSend = new byte[header.length + msg.getSize()];
		for(int i = 0; i < header.length; ++i)
			toSend[i] = header[i];
		byte[] content = msg.getContent();
		for(int i = header.length; i < toSend.length; ++i)
			toSend[i] = content[i];
		run();
	}
	
	public void run() {
		try {
			OutputStream os = this.socket.getOutputStream();
			os.write(this.toSend);
			os.flush();
			this.toSend = null;
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
