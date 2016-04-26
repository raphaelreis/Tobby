package main;

import gui.Controller;

import java.net.SocketTimeoutException;
import java.util.LinkedList;

import messages.Message;
import messages.synchronisation.BasicPingMessage;
import utilities.ByteArray;

public class NetworkInterface extends Thread {
	private Instance instance;
	private Reader reader;
	private Connection.Client serverCo;
	private String gameServerIP;
	protected Latency latency;
	protected Sender sender;
	
	public NetworkInterface(Instance instance, String login) {
		super(login + "/receiver");
		this.instance = instance;
		this.reader = new Reader();
		this.sender = new Sender(login);
		this.latency = new Latency();
	}
	
	public void run() {
		this.instance.log.p("Connection to authentification server, waiting response...");
		connectionToServer(Main.AUTH_SERVER_IP, Main.SERVER_PORT);
		if(!isInterrupted()) {
			if(this.gameServerIP == null)
				throw new FatalError("Deconnected from authentification server for unknown reason.");
			this.instance.log.p("Deconnected from authentification server.");
			this.instance.log.p("Connection to game server, waiting response...");
			connectionToServer(this.gameServerIP, Main.SERVER_PORT);
			if(!isInterrupted())
				throw new FatalError("Deconnected from game server for unknown reason.");
			this.instance.log.p("Deconnected from game server.");
		}
		Log.info("Thread receiver of instance with id = " + instance.id + " terminated.");
		Controller.getInstance().threadTerminated();
	}
	
	private void connectionToServer(String IP, int port) {
		boolean canPing = true;
		byte[] buffer = new byte[Main.BUFFER_DEFAULT_SIZE];
		int bytesReceived = 0;
		this.serverCo = new Connection.Client(IP, port);
		while(!isInterrupted()) {
			try {
				//this.instance.log.p("DEBUG : Waiting for reception.");
				if((bytesReceived = this.serverCo.receive(buffer)) == -1)
					break;
				canPing = false;
			} catch(SocketTimeoutException e) {
				this.instance.log.p("DEBUG : SocketTimeoutException");
				if(canPing) {
					BasicPingMessage BPM = new BasicPingMessage();
					BPM.quiet = true;
					instance.outPush(BPM);
					this.instance.log.p("Sending a ping request to server.");
					canPing = false;
				}
				continue;
			} catch(Exception e) {
				if(isInterrupted()) // si la connexion a �t� coup�e c�t� client
					break;
				throw new FatalError(e); // si la connexion a �t� coup�e c�t� serveur
			}
			//this.instance.log.p("DEBUG : " + bytesReceived + " bytes received from server.");
			processMsgStack(reader.processBuffer(new ByteArray(buffer, bytesReceived)));
		}
		this.serverCo.close();
	}
	
	private void processMsgStack(LinkedList<Message> msgStack) {
		Message msg;
		while((msg = msgStack.poll()) != null) {
			latency.updateLatency();
			this.instance.log.p("r", msg);
			instance.inPush(msg);
		}
	}
	
	protected void setGameServerIP(String gameServerIP) {
		this.gameServerIP = gameServerIP;
	}
	
	// on coupe la connexion c�t� client
	protected void closeReceiver() {
		interrupt();
		this.serverCo.close();
	}
	
	class Sender extends Thread {
		private Sender(String login) {
			super(login + "/sender");
		}
		
		public synchronized void run() {
			Message msg;
			while (!isInterrupted()) {
				if((msg = instance.outPull()) != null) {
					latency.setLatestSent();
					serverCo.send(msg.pack());
					instance.log.p("s", msg);
				}
				else
					try {
						//instance.log.p("DEBUG : Waiting for sending.");
						wait();
					} catch(Exception e) {
						interrupt();
					}
			}
			Log.info("Thread sender of instance with id = " + instance.id + " terminated.");
			Controller.getInstance().threadTerminated();
		}
		
		public synchronized void wakeUp() {
			notify();
		}
	}
}