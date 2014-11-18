package SocImpl;

import java.net.*;
import java.io.*;

public class SocListener extends Thread {

	private Object realObj = null;
	private String serverAddress = null;

	public SocListener(Object realObj, String serverAddress) {
		this.realObj = realObj;
		this.serverAddress = serverAddress;
		this.start();
	}

	public void run() {
		// get server IP and port
		InetAddress serverIP = null;
		String[] args = serverAddress.split(":");
		try {
			serverIP = InetAddress.getByName(args[0]); // the server IP
		} catch (UnknownHostException e) {
			System.out.println(e.getMessage());
		}
		int serverPort = Integer.parseInt(args[1]); // the server port

		try {
			ServerSocket listenSocket = new ServerSocket(serverPort, 0,
					serverIP);
			while (true) {
				// waiting for new client
				Socket clientSocket = listenSocket.accept();
				SocResponder responder = new SocResponder(clientSocket, realObj);
			}
		} catch (IOException e) {
			System.out.println(e.getMessage());
		}
	}
}
