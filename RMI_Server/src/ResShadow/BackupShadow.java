package ResShadow;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.rmi.registry.Registry;
import java.rmi.registry.LocateRegistry;
import java.rmi.server.UnicastRemoteObject;
import java.rmi.AccessException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.util.HashMap;

import ResImpl.ResourceManagerImpl;
import ResInterface.ResourceManager;

public class BackupShadow implements InvocationHandler {
	private boolean isPrimary;
	private String rmName;
	private Object realObj;
	private Object secondaryObj;
	private Registry registry;
	private ResourceManager rm;
	private HashMap<Integer, Object> returnObjCache = new HashMap<Integer, Object>();

	public BackupShadow(Registry registry, String rmName, Object realObj,
			boolean isPrimary) {
		this.registry = registry;
		this.rmName = rmName;
		this.realObj = realObj;
		this.isPrimary = isPrimary;
		Shadow.isPrimary = isPrimary;
	}

	public static void CreateShadow(Registry registry, String rmName, int port,
			Object realObj) {
		boolean isPrimary = false;

		try {
			registry.lookup(rmName);
		} catch (NotBoundException nbe) {
			System.out.println("Detecting an unbound rmName.");
			isPrimary = true;
		} catch (RemoteException re) {
			try {
				registry = LocateRegistry.createRegistry(port);
				isPrimary = true;
			} catch (RemoteException e) {
				e.printStackTrace();
			}
		}

		if (!isPrimary) {
			System.out.println("Synchroizing RM Server...");

			try {
				ResourceManager primaryRM = (ResourceManager) registry
						.lookup(rmName);
				byte[][] data = null;
				try {
					data = primaryRM.outboundSync();
				} catch (RemoteException re) {
					// if sync from primaryRM fails, then we promote the
					// existing secondary server and then sync
					try {
						primaryRM = (ResourceManager) registry.lookup(rmName
								+ "_SEC");
						primaryRM.promote();
						data = primaryRM.outboundSync();
					} catch (RemoteException ce2) {
						System.err.println("Synchronization Failed. ");
						isPrimary = true;
					} catch (NotBoundException nbe2) {
						System.err.println("Synchronization Failed. ");
						isPrimary = true;
					}
				}

				// if primary or secondary found
				if (!isPrimary) {
					((ResourceManager) realObj).inboundSync(data);
					System.out.println("Synchronization Finished. ");
				}
			} catch (NotBoundException nbe) {
				System.err.println("Primary Server Not Found.");
			} catch (RemoteException re) {
				System.err.println("Primary Server Crashes During Sync.");
			}
		}

		BackupShadow shadowHandler = new BackupShadow(registry, rmName,
				realObj, isPrimary);
		ResourceManager shadowObj = (ResourceManager) java.lang.reflect.Proxy
				.newProxyInstance(ResourceManagerImpl.class.getClassLoader(),
						new Class<?>[] { ResourceManager.class }, shadowHandler);

		try {
			ResourceManager rm = (ResourceManager) UnicastRemoteObject
					.exportObject(shadowObj, 0);
			shadowHandler.rm = rm;
			registry.rebind(rmName + (isPrimary ? "" : "_SEC"), rm);
		} catch (RemoteException e) {
			e.printStackTrace();
		}
	}

	@Override
	public Object invoke(Object proxy, Method method, Object[] args)
			throws Throwable {

		if (method.getName().equals("promote")) {
			promoteShadow();
			return null;
		}

		int toid = -1;
		if (args != null && args.length > 0 && args[0] instanceof Integer) {
			toid = ((Number) args[0]).intValue();
		}

		Object returnObj = null;

		// methods without toid
		if (toid == -1) {
			returnObj = method.invoke(realObj, args);
			if (isPrimary) {
				if (!method.getName().contains("Sync")) {
					invokeSecondary(method, args);
				}

			}

			return returnObj;
		}

		// replace toid with tid in args
		int tid = Shadow.getTID(toid);
		Shadow.backwardTOIDTable.put(tid, toid);
		args[0] = tid;

		synchronized (returnObjCache) {
			if (!returnObjCache.containsKey(toid)) {
				returnObj = method.invoke(realObj, args);
				returnObjCache.put(toid, returnObj);

				// cleanup cache for committed tid
				if (method.getName().equals("commit")) {
					cleanupReturnObjCache(tid);
					returnObjCache.put(toid, returnObj);
				}
			}

			if (isPrimary) {
				args[0] = toid;
				invokeSecondary(method, args);
			}

			return returnObjCache.get(toid);
		}
	}

	private void promoteShadow() throws AccessException, RemoteException {
		Shadow.isPrimary = true;
		try {
			registry.unbind(rmName + "_SEC");
		} catch (NotBoundException e) {

		}
		registry.rebind(rmName, this.rm);
		System.out.println("Secondary RM promoted.");
	}

	private void cleanupReturnObjCache(int tid) {
		Object[] keySet = returnObjCache.keySet().toArray();
		for (int i = 0; i < keySet.length; i++) {
			int toid = (Integer) keySet[i];
			if (Shadow.getTID(toid) == tid) {
				returnObjCache.remove(toid);
			}
		}
	}

	private void invokeSecondary(Method method, Object[] args) {

		if (secondaryObj == null) {
			try {
				secondaryObj = registry.lookup(rmName + "_SEC");
			} catch (NotBoundException | RemoteException e) {
				// secondary RM does not exist
				return;
			}
		}

		try {
			method.invoke(((ResourceManager) secondaryObj), args);
		} catch (Exception e) {

			try {
				Object prevSecondaryObjRef = secondaryObj;
				secondaryObj = registry.lookup(rmName + "_SEC");
				if (secondaryObj.equals(prevSecondaryObjRef)) {
					secondaryObj = null;
					registry.unbind(rmName + "_SEC");
				} else {
					method.invoke(((ResourceManager) secondaryObj), args);
				}
			} catch (Exception ee) {
				// secondary RM does not exist
				secondaryObj = null;
				return;
			}
			return;
		}
	}
}
