package SocImpl;

import ResInterface.*;

import java.net.*;
import java.io.*;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;

public class SocRequester implements InvocationHandler {
	private String server = null;
	private int port = 0;

	public SocRequester(String server) {
		String[] _server = server.split(":");
		this.server = _server[0];
		this.port = Integer.valueOf(_server[1]);
	}

	public SocRequester(String server, int port) {
		this.server = server;
		this.port = port;
	}

	// proxy
	public static ResourceManager newRMInstance(String server) {
		return (ResourceManager) java.lang.reflect.Proxy
				.newProxyInstance(ResourceManager.class.getClassLoader(),
						new Class[] { ResourceManager.class },
						new SocRequester(server));
	}

	public static ResourceManager newRMInstance(String server, int port) {
		return (ResourceManager) java.lang.reflect.Proxy.newProxyInstance(
				ResourceManager.class.getClassLoader(),
				new Class[] { ResourceManager.class }, new SocRequester(server,
						port));
	}

	// blocking request
	public Object sendSocRequest(String methodName, Object[] args) {
		try {
			Socket s = new Socket(server, port);
			DataOutputStream out = new DataOutputStream(s.getOutputStream());
			BufferedReader buffReader = new BufferedReader(
					new InputStreamReader(s.getInputStream()));

			try {
				out.writeBytes(Parser.serializeObjects("SMIREQUEST",
						methodName, args));
			} catch (IOException ioe) {
				System.out.println("Could not send message to the server: "
						+ ioe.getMessage());
				ioe.printStackTrace();
			}
			String serializedString = "";
			String line = null;

			try {
				line = buffReader.readLine();
				while (!line.trim().equals("END")) {
					serializedString += line + "\n";
					line = buffReader.readLine();
				}
			} catch (IOException e) {
				System.out.println(e.getMessage());
			}
			serializedString += "END\n";

			Callback cb = Parser.deserializeObjects("SMIREPLY",
					serializedString);

			return cb.arguments[0];
		} catch (IOException e) {
			System.out.println(e.getMessage());
		}

		return null;
	}

	@Override
	public Object invoke(Object proxy, Method m, Object[] args)
			throws Throwable {
		return sendSocRequest(m.getName(), args);
	}
}