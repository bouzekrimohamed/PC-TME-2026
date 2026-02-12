package pc.philo;

public class Philosopher implements Runnable {
	private Fork left;
	private Fork right;

	public Philosopher(Fork left, Fork right) {
		this.left = left;
		this.right = right;
	}
	@Override
	public void run() {
		while (!Thread.currentThread().isInterrupted()) {
            try {
                think();

                left.acquire();
                System.out.println(Thread.currentThread().getName() + " has one fork");

                right.acquire();
                System.out.println(Thread.currentThread().getName() + " has two forks");

                eat();

                right.release();
                left.release();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                right.release();
                left.release();
            }
        }
	}

	private void eat() {
		System.out.println(Thread.currentThread().getName() + " is eating");
	}

	private void think() {
		System.out.println(Thread.currentThread().getName() + " is thinking");
	}
}
