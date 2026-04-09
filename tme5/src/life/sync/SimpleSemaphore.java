package life.sync;

public class SimpleSemaphore {
	private int permits;

	public SimpleSemaphore(int permits) {
		if (permits < 0) {
			throw new IllegalArgumentException("permits must be >= 0");
		}
		this.permits = permits;
	}

	public synchronized void acquire(int n) throws InterruptedException {
		if (n < 0) {
			throw new IllegalArgumentException("n must be >= 0");
		}
		while (permits < n) {
			wait();
		}
		permits -= n;
	}

	public synchronized void release(int n) {
		if (n < 0) {
			throw new IllegalArgumentException("n must be >= 0");
		}
		permits += n;
		notifyAll();
	}

	public void acquire() throws InterruptedException {
		acquire(1);
	}

	public void release() {
		release(1);
	}
}
