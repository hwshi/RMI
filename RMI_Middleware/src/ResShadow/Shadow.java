package ResShadow;

import java.util.concurrent.ConcurrentHashMap;

public class Shadow {
	public static boolean isPrimary = false;
	public static final int TIDBASE = 100;
	
	protected static ConcurrentHashMap<Integer, Integer> backwardTOIDTable = new ConcurrentHashMap<Integer, Integer>();  // TID, TOID
	protected static ConcurrentHashMap<Integer, Integer> forwardTOIDTable = new ConcurrentHashMap<Integer, Integer>();  // TOID, TOOID
	
	public static int getTID(int toid){
		int tid = toid;
		while(tid >= TIDBASE){
			tid /= TIDBASE;
		}
		return tid;
	}
	
	public static int getOID(int toid){
		return toid % TIDBASE;
	}
	
	public static int createTOID(int tid){
		return tid * TIDBASE + 1;  // starts from 1, up to TIDBASE - 1
	}
}
