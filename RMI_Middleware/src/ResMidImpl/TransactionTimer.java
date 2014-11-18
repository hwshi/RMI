package ResMidImpl;

import ResInterface.*;

public class TransactionTimer extends Thread {

	private static final int TIMEOUT = 1000000;
	private int tid;
	private boolean stopped = false;

	public TransactionTimer(int tid) {
		this.tid = tid;
		this.start();
	}
	
	public TransactionTimer(int tid, boolean deadtime) {
		this.tid = tid;
	}

	public void reset() {
		this.interrupt();
	}

	public void stopTimer() {
		stopped = true;
		this.interrupt();
	}

	@Override
	public void run() {
		boolean interrupted = false;
		do {
			interrupted = false;
			try {
				// System.out.println("before sleep");
				this.sleep(TIMEOUT);
				// System.out.println("after sleep");
			} catch (InterruptedException e) {
				interrupted = true;
				// System.out.println("waken up");
			}
		} while (interrupted && !stopped);

		if (!stopped) {
			try {
				TransactionManager.abort(tid);
			} catch (InvalidTransactionException e) {
				// e.printStackTrace();
			}
		}
	}
}
