package SocImpl;

import java.net.*;
import java.io.*;
import java.util.*;
import java.lang.reflect.*;

public class SocResponder extends Thread {
	private Socket clientSocket;
	private Object realObj;
	private BufferedReader buffReader;
	private BufferedOutputStream buffWriter;

	public SocResponder(Socket aClientSocket, Object realObj) {
		try {
			clientSocket = aClientSocket;
			this.realObj = realObj;

			buffReader = new BufferedReader(new InputStreamReader(
					clientSocket.getInputStream()));
			buffWriter = new BufferedOutputStream(new DataOutputStream(
					clientSocket.getOutputStream()));

			this.start(); // start

		} catch (IOException e) {
			System.out.println("Connection:" + e.getMessage());
		}
	}

	public void run() {
		try {
			String serializedString = "";
			String line = null;

			try {
				line = buffReader.readLine();
				while (!line.trim().equals("END")) {
					serializedString += line + "\n";
					line = buffReader.readLine();
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
			serializedString += "END\n";

			Callback mc = Parser.deserializeObjects("SMIREQUEST",
					serializedString);
			Object result = invokeMethod(mc.methodName, mc.arguments);
			serializedString = Parser.serializeObjects("SMIREPLY", null,
					new Object[] { result });
			try {
				buffWriter.write(serializedString.getBytes());
				buffWriter.flush();
			} catch (IOException e) {
				System.out.println(e.getMessage());
			}
		} finally {
			try {
				clientSocket.close();
			} catch (IOException e) {
				System.out.println(e.getMessage());
			}
		}
	}

	private Object invokeMethod(String methodName, Object[] args) {
		try {
			Class<?> c = realObj.getClass();
			List<Class> paramTypeList = new ArrayList<Class>();
			for (Object obj : args) {
				if (obj instanceof Integer) {
					paramTypeList.add(int.class);
				} else if (obj instanceof String) {
					paramTypeList.add(String.class);
				} else if (obj instanceof Boolean) {
					paramTypeList.add(boolean.class);
				} else if (obj instanceof java.util.Vector) {
					paramTypeList.add(java.util.Vector.class);
				}
			}
			Class[] paramClass = new Class[paramTypeList.size()];
			paramTypeList.toArray(paramClass);
			Method m = c.getDeclaredMethod(methodName, paramClass);
			return m.invoke(realObj, args);
		} catch (Exception e1) {
			e1.printStackTrace();
		}
		return null;
	}
}
