package ResMidImpl;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.Iterator;
import java.util.Vector;

import LockManager.*;
import ResInterface.ResourceManager;
//import ResMidImpl.ResourceManagerMiddlewareImpl.InvolvedRM;
import ResMidImpl.ResourceManagerMiddlewareImpl;

public class RMIHandler implements InvocationHandler {
	private Object realObj;

	public RMIHandler(Object realObj) {
		this.realObj = realObj;
	}

	public static ResourceManager newProxy(Object obj) {
		return (ResourceManager) java.lang.reflect.Proxy
				.newProxyInstance(
						ResourceManagerMiddlewareImpl.class.getClassLoader(),
						new Class<?>[] { ResourceManager.class },
						new RMIHandler(obj));
	}

	@Override
	public Object invoke(Object proxy, Method method, Object[] args)
			throws Throwable {

		int tid = -1;
		if (args != null && args.length > 0) {
			tid = (int) args[0];
			if (!TransactionManager.TimerTable.containsKey(tid)) {
				if (TransactionManager.abortedTable.contains(tid))
					throw new ResInterface.TransactionAbortedException(
							"Aborted Transaction.");
				else
					throw new ResInterface.InvalidTransactionException(
							"Invalid Transaction.");
			}
		}

		// acquire locks
		boolean granted = false;
		String methodName = method.getName();
		if (methodName.equals("itinerary")) {
			granted = requestLocksItinerary(tid, methodName, args);
		} else {
			granted = requestLocksNonItinerary(tid, methodName, args);
		}

		// Lock request denied

		if (!granted) {
			if (method.getReturnType() == Void.TYPE)
				return null;
			else if (method.getReturnType() == Integer.TYPE)
				return -1;
			else if (method.getReturnType() == Boolean.TYPE)
				return false;
			else if (method.getReturnType() == String.class)
				return "Lock Request Failed";
		}

		// Lock granted

		if (tid != -1)
			TransactionManager.TimerTable.get(tid).reset();

		// invoke method in Middleware
		Object returnObj = method.invoke(realObj, args);

		if (methodName.equals("newCustomer") && args.length == 1) {
			try {
				System.out.println("RMHandler is trying to assign lock to TM!");
				TransactionManager.Lock(
						ResourceManagerMiddlewareImpl.ICUSTOMER, tid,
						String.valueOf(returnObj), LockManager.WRITE);
			} catch (DeadlockException e) {
				return false;
			}
		}

		return returnObj;
	}

	// *************************************special

	private boolean requestLocksItinerary(int tid, String methodName,
			Object[] args) {

		// int customerId = (int) args[1];
		int customerId = ((Number) args[1]).intValue();
		Vector flightNumbers = (Vector) args[2];
		String location = (String) args[3];
		// boolean Car = (boolean) args[4];
		// boolean Room = (boolean) args[5];
		boolean requestCar = ((Boolean) args[4]).booleanValue();
		boolean requestRoom = ((Boolean) args[5]).booleanValue();

		try {
			// customer lock
			TransactionManager.Lock(ResourceManagerMiddlewareImpl.ICUSTOMER,
					tid, String.valueOf(customerId), LockManager.WRITE);

			// flight lock
			Iterator<Integer> flightIterator = flightNumbers.iterator();
			while (flightIterator.hasNext()) {
				TransactionManager.Lock(ResourceManagerMiddlewareImpl.IFLIGHT,
						tid, String.valueOf(flightIterator.next()),
						LockManager.WRITE);
			}
			// car lock
			if (requestCar)
				TransactionManager.Lock(ResourceManagerMiddlewareImpl.ICAR,
						tid, location, LockManager.WRITE);

			// room lock
			if (requestRoom)
				TransactionManager.Lock(ResourceManagerMiddlewareImpl.IHOTEL,
						tid, location, LockManager.WRITE);

		} catch (DeadlockException e) {
			return false;
		}

		return true;
	}

	private boolean requestLocksNonItinerary(int tid, String methodName,
			Object[] args) {
		int serverType = -1, lockType = -1;
		String key = "";
		boolean isReserve = false;

		// lock type
		if (methodName.startsWith("query")) {
			lockType = LockManager.READ;
		} else if (methodName.startsWith("add")
				|| methodName.startsWith("delete")
				|| methodName.startsWith("new")) {
			lockType = LockManager.WRITE;
		} else if (methodName.startsWith("reserve")) {
			lockType = LockManager.WRITE;
			isReserve = true;
		}

		// select server
		if (methodName.contains("Car")) {
			serverType = ResourceManagerMiddlewareImpl.ICAR;
		} else if (methodName.contains("Flight")) {
			serverType = ResourceManagerMiddlewareImpl.IFLIGHT;
		} else if (methodName.contains("Room")) {
			serverType = ResourceManagerMiddlewareImpl.IHOTEL;
		} else if (methodName.contains("Customer")) {
			serverType = ResourceManagerMiddlewareImpl.ICUSTOMER;
		}
		// invoke method
		if (serverType != -1 && lockType != -1 && args.length >= 2) {
			key = String.valueOf(args[1]);
			try {
				if (isReserve) {
					String keyCustomer = key;
					key = String.valueOf(args[2]);
					TransactionManager.Lock(
							ResourceManagerMiddlewareImpl.ICUSTOMER, tid,
							keyCustomer, lockType);
				}
				System.out.println("RMHandler is trying to assign lock to TM!");
				TransactionManager.Lock(serverType, tid, key, lockType);
			} catch (DeadlockException e) {
				return false;
			}
		}
		return true;
	}
}
