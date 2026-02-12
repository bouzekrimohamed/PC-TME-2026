package pc.philo;
import java.util.concurrent.locks.ReentrantLock;
public class Fork {
    private final ReentrantLock lock = new ReentrantLock();
	public void acquire () throws InterruptedException {
        lock.lockInterruptibly();
    }
	
	
	public void release () {
		if (lock.isHeldByCurrentThread()) {
            lock.unlock();
        }
	}
}
