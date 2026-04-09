package life;

public class LifeModelBlock extends LifeModel {
	private boolean updateTurn = true;

	public LifeModelBlock(int rows, int cols) {
		super(rows, cols);
	}

	@Override
	public synchronized void updateNext(int startRow, int endRow) {
		while (!updateTurn) {
			try {
				wait();
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				return;
			}
		}
		super.updateNext(startRow, endRow);
		updateTurn = false;
		notifyAll();
	}

	@Override
	public synchronized void refreshCurrent() {
		while (updateTurn) {
			try {
				wait();
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				return;
			}
		}
		super.refreshCurrent();
		updateTurn = true;
		notifyAll();
	}

	@Override
	public synchronized boolean isAlive(int r, int c) {
		return super.isAlive(r, c);
	}

	@Override
	public synchronized void setAlive(int r, int c, boolean alive) {
		super.setAlive(r, c, alive);
	}

	@Override
	public synchronized void clear() {
		super.clear();
		updateTurn = true;
		notifyAll();
	}

	@Override
	public synchronized void updateFrom(LifeModel mcopy) {
		super.updateFrom(mcopy);
		updateTurn = true;
		notifyAll();
	}
}
