package ResShadow;

import ResInterface.*;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.rmi.registry.Registry;

import ResInterface.ResourceManager;

public class RequestingShadow implements InvocationHandler {
	private Registry registry;
	private String rmName;
	private Object realObj;

	public RequestingShadow(Registry registry, String rmName, Object realObj) {
		this.registry = registry;
		this.rmName = rmName;
		this.realObj = realObj;
	}

	public static ResourceManager CreateShadow(Registry registry,
			String rmName, Object realObj) {
		return (ResourceManager) java.lang.reflect.Proxy.newProxyInstance(
				ResourceManager.class.getClassLoader(),
				new Class[] { ResourceManager.class }, new RequestingShadow(
						registry, rmName, realObj));
	}

	@Override
	public Object invoke(Object proxy, Method method, Object[] args)
			throws Throwable {
		Object prevObjRef = realObj;

		if (args != null && args.length > 0 && args[0] instanceof Integer) {
			int tid = ((Number) args[0]).intValue();

			Integer toid = Shadow.backwardTOIDTable.get(tid);

			if (Shadow.forwardTOIDTable.containsKey(toid)) {
				Integer tooid = Shadow.forwardTOIDTable.get(toid);
				tooid++;
				Shadow.forwardTOIDTable.put(toid, tooid);
			} else {
				Shadow.forwardTOIDTable.put(toid, Shadow.createTOID(toid));
			}

			// replace tid with tooid in args
			args[0] = Shadow.forwardTOIDTable.get(toid);
		}

		try {
			return method.invoke(realObj, args);
		} catch (Exception e) {
			Exception innerException = (Exception) e.getCause();
			if (innerException instanceof java.rmi.ConnectException
					|| innerException instanceof java.rmi.UnmarshalException) {

				synchronized (rmName) {
					// redo lookup to check if any server has been promoted by a
					// new secondary server
					realObj = registry.lookup(rmName);

					if (realObj.equals(prevObjRef)) {
						try {
							realObj = registry.lookup(rmName + "_SEC");
							((ResourceManager) realObj).promote();
						} catch (java.rmi.NotBoundException nbe) {
							System.out.println("Secondary " + rmName
									+ " does not exist.");
							throw nbe;
						}
					}
					return method.invoke(realObj, args);
				}
			}
		}

		return null;
	}
}
