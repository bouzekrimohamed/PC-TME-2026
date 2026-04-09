package life;

public class LifeModelSync extends LifeModel {
	public LifeModelSync(int rows, int cols) {
		super(rows, cols);
	}

	@Override
	public synchronized void updateNext(int startRow, int endRow) {
		super.updateNext(startRow, endRow);
	}

	@Override
	public synchronized void refreshCurrent() {
		super.refreshCurrent();
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
	}

	@Override
	public synchronized void updateFrom(LifeModel mcopy) {
		super.updateFrom(mcopy);
	}
}
