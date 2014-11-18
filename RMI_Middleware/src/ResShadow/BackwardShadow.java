package ResShadow;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.rmi.AccessException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.HashMap;

import ResInterface.ResourceManager;

public class BackwardShadow implements InvocationHandler {
	private Registry registry;
	private String bindingName;
	private Object realObj;
	private Object secondaryObj = null;
	private ResourceManager exportedRMObject = null;
	private HashMap<Integer, Object> returnObjCache = new HashMap<Integer, Object>();  // TOID, returnObj
	private boolean isPrimary = false;
	
	public BackwardShadow(Registry registry, String bindingName, Object realObj, boolean isPrimary) {
		this.registry = registry;
		this.bindingName = bindingName;
		this.realObj = realObj;
		this.isPrimary = isPrimary;
		Shadow.isPrimary = isPrimary;
	}
	
	public static ResourceManager newShadow(Registry registry, String bindingName, Object realObj){
		
		boolean isPrimary = false;
		
		try{
			// Bind the remote object's stub in the registry
			try{
				registry.lookup(bindingName);
				isPrimary = false;
			}catch(java.rmi.NotBoundException nbe){
				isPrimary = true;
			}catch(java.rmi.ConnectException ce){
				registry = LocateRegistry.createRegistry(registry.REGISTRY_PORT);
				//registry = LocateRegistry.getRegistry();
				isPrimary = true;
			}
			
			if(!isPrimary){
				System.err.println("Syncing RM Server...");
				
				ResourceManager primaryRM = (ResourceManager) registry.lookup(bindingName);
				byte[][] data = null;
				
				// if sync from primaryRM fails, then promote existing secondary and then sync
				try{
					data = primaryRM.outboundSync();
				}catch(java.rmi.ConnectException ce){
					try{
						primaryRM = (ResourceManager) registry.lookup(bindingName + "_SEC");
						primaryRM.promote();
						data = primaryRM.outboundSync();
					}catch(java.rmi.ConnectException | java.rmi.NotBoundException ce2){
						System.err.println("Sync failed");
						isPrimary = true;
					}
				}
				
				// if primary or secondary found
				if(!isPrimary){
					((ResourceManager) realObj).inboundSync(data);
					System.err.println("Sync finished");
				}
			}
		}catch(RemoteException | NotBoundException re){
			re.printStackTrace();
		}
		
		// create a new Server object
		//exportedRMObject = BackwardShadow.newShadow(registry, RMIBindingName, new ResourceManagerImpl(), isPrimary);
	 	
		// dynamically generate the stub (client proxy)
		//registry.rebind(RMIBindingName + (isPrimary ? "" : Shadow.SECONDARY_SUFFIX), exportedRMObject);
		
		BackwardShadow handler = new BackwardShadow(registry, bindingName, realObj, isPrimary); 
		ResourceManager shadowObj = (ResourceManager) java.lang.reflect.Proxy.newProxyInstance(ResourceManager.class.getClassLoader(),
									new Class[] {ResourceManager.class}, handler);
		try {
			ResourceManager exportedRMObject = (ResourceManager) UnicastRemoteObject.exportObject(shadowObj, 0);
			handler.setExportedRMObject(exportedRMObject);
			
			registry.rebind(bindingName + (isPrimary ? "" : "_SEC"), exportedRMObject);
		} catch (RemoteException e) {
			e.printStackTrace();
		}
		
		return shadowObj;
	}
	
	@Override
	public Object invoke(Object proxy, Method method, Object[] args)
			throws Throwable {
		
		if(method.getName().equals("promote")){
			promoteShadow();
			return null;
		}
		
		int toid = -1;
		if (args != null && args.length > 0 && args[0] instanceof Integer){
			toid = ((Number)args[0]).intValue();
		}
		
		Object returnObj = null;
		
		// methods without toid
		if(toid == -1){
			returnObj = method.invoke(realObj, args);
			if (isPrimary){
				if(!method.getName().contains("Sync")){
					invokeSecondary(method, args);
				}
				
			}
			
			return returnObj;
		}
		
		// replace toid with tid in args
		int tid = Shadow.getTID(toid);
		Shadow.backwardTOIDTable.put(tid, toid);
		args[0] = tid;
		
		synchronized(returnObjCache){
			if (!returnObjCache.containsKey(toid)){
				returnObj = method.invoke(realObj, args);
				returnObjCache.put(toid, returnObj);
				
				// cleanup cache for committed tid
				if(method.getName().equals("commit")){
					cleanupReturnObjCache(tid);
					returnObjCache.put(toid, returnObj);
				}
			}
			
			if (isPrimary){
				args[0] = toid;
				invokeSecondary(method, args);
			}
			
			return returnObjCache.get(toid);
		}
	}
	
	private void setExportedRMObject(ResourceManager exportedRMObject){
		this.exportedRMObject = exportedRMObject;
	}
	
	private void promoteShadow() throws AccessException, RemoteException{
		isPrimary = true;
		Shadow.isPrimary = isPrimary;
		try {
			registry.unbind(bindingName + "_SEC");
		} catch (NotBoundException e) {
			
		}
		registry.rebind(bindingName, exportedRMObject);
		System.err.println("RM promoted.");
	}
	
	private void cleanupReturnObjCache(int tid){
		Object[] keySet = returnObjCache.keySet().toArray();
		for(int i = 0; i < keySet.length; i++){
			int toid = (Integer) keySet[i];
			if(Shadow.getTID(toid) == tid){
				returnObjCache.remove(toid);
			}
		}
	}
	
	private void invokeSecondary(Method method, Object[] args){
		
		if(secondaryObj == null){
			try {
				secondaryObj = registry.lookup(bindingName + "_SEC");
			} catch (NotBoundException | RemoteException e) {
				// secondary RM does not exist
				return;
			}
		}
		
		try{
			method.invoke(((ResourceManager) secondaryObj), args);
		} catch (Exception e) {
			
			try {
				Object prevSecondaryObjRef = secondaryObj;
				secondaryObj = registry.lookup(bindingName + "_SEC");
				if(secondaryObj.equals(prevSecondaryObjRef)){
					secondaryObj = null;
					registry.unbind(bindingName + "_SEC");
				}else{
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
