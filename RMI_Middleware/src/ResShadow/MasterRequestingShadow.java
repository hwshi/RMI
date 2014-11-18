package ResShadow;

import java.lang.reflect.Method;
import java.rmi.registry.Registry;

import ResInterface.ResourceManager;

public class MasterRequestingShadow extends RequestingShadow {
	public MasterRequestingShadow(Registry registry, String rmName, Object realObj) {
		super(registry, rmName, realObj);
		
	}
	
	public static ResourceManager CreateShadow(Registry registry, String bindingName, Object realObj){
		return (ResourceManager) java.lang.reflect.Proxy.newProxyInstance(ResourceManager.class.getClassLoader(),
				new Class[] {ResourceManager.class},
				new MasterRequestingShadow(registry, bindingName, realObj));
	}
	
	@Override
	public Object invoke(Object proxy, Method method, Object[] args)
			throws Throwable {
		return super.invoke(proxy, method, args);
		// invoke RM servers only when Master is primary
//		if (ShadowData.isPrimary){
//			return super.invoke(proxy, method, args);
//		}
//		return null;
	}
}
