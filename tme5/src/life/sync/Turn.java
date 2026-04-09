package life.sync;

public class Turn {
	private boolean isPlayerOneTurn = true;

	public synchronized void startTurn(boolean isPlayerOne) throws InterruptedException {
		while (isPlayerOneTurn != isPlayerOne) {
			wait();
		}
	}

	public synchronized void endTurn() {
		isPlayerOneTurn = !isPlayerOneTurn;
		notifyAll();
	}
}
