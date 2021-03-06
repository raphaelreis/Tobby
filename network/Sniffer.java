package network;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.LinkedList;

import main.FatalError;
import main.Log;
import main.Main;
import messages.NetworkMessage;
import messages.connection.SelectedServerDataMessage;
import utilities.ByteArray;
import utilities.Processes;

public class Sniffer extends Thread {
	private static final String DOFUS_EXE = "Dofus.exe";
	private static Reader reader = new Reader();
	private static String gameServerAddress;
	private static ServerSocket clientCo;
	private static Client client;
	private static Client serverCo;
	private static boolean mustDeconnectClient = false;
	private static Log log;
	private static Thread serverCoThread;
	
	public Sniffer() {
		log = new Log("Sniffer");
		launch();
	}
	
	private void launch() {
		log.p("Waiting for " + DOFUS_EXE +" process...");
		while(!Processes.inProcess(DOFUS_EXE))
			try {
				Thread.sleep(1000);
			} catch (Exception e) {
				e.printStackTrace();
			}
		Processes.injectDLL("A COMPLETER", "Dofus.exe");
		
		try {
			clientCo = new ServerSocket(Main.SERVER_PORT);
			log.p("Running sniffer server. Waiting Dofus client connection...");
			client = new Client(clientCo.accept());
			log.p("Dofus client connected.");
			serverCo = new Client(Main.AUTH_SERVER_IP, Main.SERVER_PORT);
		} catch(IOException e) {
			e.printStackTrace();
			return;
		}
		log.p("Running sniffer client. Connection to Dofus server.");
		
		start();
		
		byte[] buffer = new byte[ByteArray.BUFFER_DEFAULT_SIZE];
		ByteArray array = new ByteArray();
		int bytesReceived = 0;
		try {
			while((bytesReceived = client.receive(buffer)) != -1) {
				array.setArray(buffer, bytesReceived);
				processMsgStack(reader.processBuffer(array), "s");
				serverCo.send(ByteArray.trimBuffer(buffer, bytesReceived));
			}
		} catch(IOException e) {
			e.printStackTrace();
			return;
		}
		
		log.p("Waiting client reconnection...");
		try {
			client = new Client(clientCo.accept());
		} catch(IOException e) {
			e.printStackTrace();
			return;
		}
		log.p("Dofus client reconnected.");
		synchronized(this) {
			serverCoThread.notify();
		}
		
		try {
			while((bytesReceived = client.receive(buffer)) != -1) {
				array.setArray(buffer, bytesReceived);
				processMsgStack(reader.processBuffer(array), "s");
				serverCo.send(ByteArray.trimBuffer(buffer, bytesReceived));
			}
		} catch(Exception e) {
			e.printStackTrace();
			return;
		}
		log.p("Dofus client deconnected from sniffer server.");
		client.close();
	}
	
	public void run() { // connexion au serveur officiel
		serverCoThread = this;
		byte[] buffer = new byte[ByteArray.BUFFER_DEFAULT_SIZE];
		ByteArray array = new ByteArray();
		int bytesReceived = 0;
		
		try {
			while((bytesReceived = serverCo.receive(buffer)) != -1) {
				array.setArray(buffer, bytesReceived);
				if(processMsgStack(reader.processBuffer(array), "r"))
					client.send(ByteArray.trimBuffer(buffer, bytesReceived));
				if(mustDeconnectClient)
					break;
			}
		} catch(IOException e) {
			e.printStackTrace();
			return;
		}
		client.close();
		log.p("Deconnection from Dofus client.");
		serverCo.close();
		log.p("Deconnected from authentification server.");
		
		synchronized(this) {
			try {
				wait();
			} catch(InterruptedException e) {
				e.printStackTrace();
			}
		}
		if(gameServerAddress != null) {
			log.p("Connecting to game server, waiting response...");
			try {
				serverCo = new Client(gameServerAddress, Main.SERVER_PORT);
				while((bytesReceived = serverCo.receive(buffer)) != -1) {
					array.setArray(buffer, bytesReceived);
					processMsgStack(reader.processBuffer(array), "r");
					client.send(ByteArray.trimBuffer(buffer, bytesReceived));
				}
			} catch(IOException e) {
				e.printStackTrace();
				return;
			}
			serverCo.close();
			log.p("Deconnected from game server.");
		}
	}
	
	public static boolean processMsgStack(LinkedList<NetworkMessage> msgStack, String direction) {
		NetworkMessage msg;
		while((msg = msgStack.poll()) != null) {
			log.p(direction, msg);
			//if(direction.equals("r"))
				//Reflection.displayMessageFields(msg);
			if(direction.equals("r") && msg.getId() == 42) {
				SelectedServerDataMessage SSDM = (SelectedServerDataMessage) msg;
				gameServerAddress = SSDM.address;
				mustDeconnectClient = true;
				if(msgStack.size() > 1)
					throw new FatalError("Little problem !");
				SSDM.address = Main.LOCALHOST;
				try {
					client.send(SSDM.pack(0));
				} catch(IOException e) {
					e.printStackTrace();
				}
				return false;
			}
		}
		return true;
	}
}
