package ResShadow;

import java.lang.reflect.Method;
import java.rmi.registry.Registry;

import ResInterface.ResourceManager;

public class ClientRequestingShadow extends RequestingShadow {
	public ClientRequestingShadow(Registry registry, String rmName, Object realObj) {
		super(registry, rmName, realObj);
		
	}
	
	public static ResourceManager newShadow(Registry registry, String bindingName, Object realObj){
		return (ResourceManager) java.lang.reflect.Proxy.newProxyInstance(ResourceManager.class.getClassLoader(),
				new Class[] {ResourceManager.class},
				new ClientRequestingShadow(registry, bindingName, realObj));
	}
	
	@Override
	public Object invoke(Object proxy, Method method, Object[] args)
			throws Throwable {
		if (args != null && args.length > 0 && args[0] instanceof Integer){
			int tid = ((Number)args[0]).intValue();
			
			// create toid
//			if(ShadowData.TOIDTable.containsKey(tid)){
//				Integer toid = ShadowData.TOIDTable.get(tid);
//				toid++;
//				ShadowData.TOIDTable.put(tid, toid);
//			}else{
//				ShadowData.TOIDTable.put(tid, ShadowData.createTOID(tid));
//			}
			
			Shadow.backwardTOIDTable.put(tid, tid);  // prepare for ForwardShadow read
			
		}
		return super.invoke(proxy, method, args);
	}
	
}
