package ResMidImpl;

import java.rmi.RemoteException;
import java.io.*;
import java.util.*;

import LockManager.*;
import ResInterface.*;
import ResMidImpl.ResourceManagerMiddlewareImpl;

public class TransactionManager {
	private static int tid = 0;
	private static LockManager[] lockManagers = { new LockManager(),
			new LockManager(), new LockManager(), new LockManager(), };
	private static Hashtable<Integer, HashSet<Integer>> LockingTable = new Hashtable<Integer, HashSet<Integer>>();
	public static Hashtable<Integer, TransactionTimer> TimerTable = new Hashtable<Integer, TransactionTimer>();
	public static HashSet<Integer> activeTable = new HashSet<Integer>();
	public static HashSet<Integer> abortedTable = new HashSet<Integer>();

	public static int start() {
		TimerTable.put(tid, new TransactionTimer(tid));
		LockingTable.put(tid, new HashSet<Integer>());
		activeTable.add(tid);
		System.out.println("Transaction " + tid + " is created.");
		return tid++;
	}

	public static boolean commit(int transactionID)
			throws TransactionAbortedException, InvalidTransactionException {
		System.out.println("Transaction " + transactionID + " is committed.");
		HashSet<Integer> hs = LockingTable.get(transactionID);
		if (hs != null) {
			try {
				Iterator<Integer> it = hs.iterator();
				while (it.hasNext()) {
					ResourceManagerMiddlewareImpl.remoteManagers[it.next()]
							.commit(transactionID);
				}
			} catch (RemoteException e) {
				e.printStackTrace();
			}
		}
		return releaseResources(transactionID);
	}

	public static void abort(int transactionID)
			throws InvalidTransactionException {

		HashSet<Integer> hs = LockingTable.get(transactionID);
		if (hs != null) {
			try {
				Iterator<Integer> it = hs.iterator();
				while (it.hasNext()) {
					ResourceManagerMiddlewareImpl.remoteManagers[it.next()]
							.abort(transactionID);
				}
			} catch (RemoteException e) {
				e.printStackTrace();
			}
		}

		releaseResources(transactionID);
		System.out.println("Transaction " + transactionID + " aborted.");
	}

	private static boolean releaseResources(int tid) {
		TimerTable.get(tid).stopTimer();

		// release all locks
		HashSet<Integer> hs = LockingTable.get(tid);
		if (hs == null) {
			return false;
		} else {
			boolean result = true;
			Iterator<Integer> it = hs.iterator();
			while (it.hasNext()) {
				result = result && lockManagers[it.next()].UnlockAll(tid);
			}

			// clean up tLMTable and tTimerTable
			if (result) {
				LockingTable.remove(tid);
				TimerTable.remove(tid);
				activeTable.remove(tid);
				// abortedTable.add(tid);
			}
			return result;
		}
	}

	public static void Lock(int objType, int tid, String key, int lockType)
			throws DeadlockException {
		lockManagers[objType].Lock(tid, key, lockType);
		// enlist
		LockingTable.get(tid).add(objType);
	}

	public static byte[][] outboundSync() {
		byte[][] data = new byte[16][];
		synchronized (new Object()) {
			try {
				for (int i = 0; i < data.length; i++) {
					ByteArrayOutputStream baos = new ByteArrayOutputStream();
					ObjectOutputStream oos = new ObjectOutputStream(baos);
					switch (i) {
					case 0:
						oos.writeObject(tid);
						break;
					case 1:
						oos.writeObject(LockingTable);
						break;
					case 2:
						oos.writeObject(activeTable);
						break;
					case 3:
						oos.writeObject(abortedTable);
						break;
					case 4:
					case 5:
					case 6:
					case 7:
						oos.writeObject(lockManagers[i - 4].lockTable);
						Vector v = lockManagers[i - 4].lockTable.allElements();
						System.out.println(i);
						System.out.println(v.toString());
						break;
					case 8:
					case 9:
					case 10:
					case 11:
						oos.writeObject(lockManagers[i - 8].stampTable);
						break;
					case 12:
					case 13:
					case 14:
					case 15:
						oos.writeObject(lockManagers[i - 12].waitTable);
						break;
					}
					data[i] = baos.toByteArray();
					System.out.println(i + ", " + data[i]);
				}
				// ByteArrayOutputStream baos0 = new ByteArrayOutputStream();
				// ObjectOutputStream oos0 = new ObjectOutputStream(baos0);
				// oos0.writeObject(LMs[0].lockTable);
				// data[0] = baos0.toByteArray();
				//
				// ByteArrayInputStream bais0 = new
				// ByteArrayInputStream(data[0]);
				// ObjectInputStream ois0 = new ObjectInputStream(bais0);
				// try {
				// TPHashTable rmhtclone = (TPHashTable) ois0.readObject();
				// Vector v = rmhtclone.allElements();
				//
				// System.out.println(v.toString());
				// } catch (ClassNotFoundException e) {
				// e.printStackTrace();
				// }
				//
				// ByteArrayOutputStream baos1 = new ByteArrayOutputStream();
				// ObjectOutputStream oos1 = new ObjectOutputStream(baos1);
				// oos1.writeObject(LMs[0].stampTable);
				// data[1] = baos1.toByteArray();
				//
				// ByteArrayInputStream bais1 = new
				// ByteArrayInputStream(data[1]);
				// ObjectInputStream ois1 = new ObjectInputStream(bais1);
				// try {
				// TPHashTable rmhtclone = (TPHashTable) ois1.readObject();
				// Vector v = rmhtclone.allElements();
				//
				// System.out.println(v.toString());
				// } catch (ClassNotFoundException e) {
				// e.printStackTrace();
				// }
				//
				// ByteArrayOutputStream baos3 = new ByteArrayOutputStream();
				// ObjectOutputStream oos3 = new ObjectOutputStream(baos3);
				// oos3.writeObject(LMs[0].waitTable);
				// data[2] = baos3.toByteArray();
				//
				// ByteArrayInputStream bais2 = new
				// ByteArrayInputStream(data[2]);
				// ObjectInputStream ois2 = new ObjectInputStream(bais2);
				// try {
				// TPHashTable rmhtclone = (TPHashTable) ois2.readObject();
				// Vector v = rmhtclone.allElements();
				//
				// System.out.println(v.toString());
				// } catch (ClassNotFoundException e) {
				// e.printStackTrace();
				// }

			} catch (IOException e) {
				e.printStackTrace();
			}

			return data;
		}
	}

	public static void inboundSync(byte[][] data) {
		System.out.println("wocao" + data.length);
		try {
			for (int i = 0; i < data.length; i++) {
				ObjectInputStream ois = new ObjectInputStream(
						new ByteArrayInputStream(data[i]));

				switch (i) {
				case 0:
					tid = (Integer) ois.readObject();
					break;
				case 1:
					LockingTable = (Hashtable<Integer, HashSet<Integer>>) ois
							.readObject();
					break;
				case 2:
					activeTable = (HashSet<Integer>) ois.readObject();
					break;
				case 3:
					abortedTable = (HashSet<Integer>) ois.readObject();
					break;
				case 4:
				case 5:
				case 6:
				case 7:
					lockManagers[i - 4].lockTable = (TPHashTable) ois
							.readObject();
					Vector v = lockManagers[i - 4].lockTable.allElements();
					System.out.println(v.toString());
					break;
				case 8:
				case 9:
				case 10:
				case 11:
					lockManagers[i - 8].stampTable = (TPHashTable) ois
							.readObject();
					Vector vv = lockManagers[i - 8].stampTable.allElements();
					System.out.println(vv.toString());
					break;
				case 12:
				case 13:
				case 14:
				case 15:
					lockManagers[i - 12].waitTable = (TPHashTable) ois
							.readObject();
					Vector vvv = lockManagers[i - 12].waitTable.allElements();
					System.out.println(vvv.toString());
					break;
				}

			}

			Iterator it = activeTable.iterator();
			while (it.hasNext()) {
				Integer i = (Integer) it.next();
				TimerTable.put(i, new TransactionTimer(i, true)); // dead timer
			}

			// ObjectInputStream ois0 = new ObjectInputStream(new
			// ByteArrayInputStream(data[0]));
			// try {
			// LMs[0].lockTable = (TPHashTable) ois0.readObject();
			// Vector v = LMs[0].lockTable.allElements();
			//
			// System.out.println(v.toString());
			// } catch (ClassNotFoundException e) {
			// e.printStackTrace();
			// }
			//
			// ObjectInputStream ois1 = new ObjectInputStream(new
			// ByteArrayInputStream(data[1]));
			// try {
			// tLMTable = (Hashtable<Integer, HashSet<Integer>>)
			// ois1.readObject();
			// } catch (ClassNotFoundException e) {
			// e.printStackTrace();
			// }
			//
			// ObjectInputStream ois3 = new ObjectInputStream(new
			// ByteArrayInputStream(data[3]));
			// try {
			// abortedTable = (HashSet<Integer>) ois3.readObject();
			// } catch (ClassNotFoundException e) {
			// e.printStackTrace();
			// }

		} catch (IOException | ClassNotFoundException e) {
			e.printStackTrace();
		}
	}
}
