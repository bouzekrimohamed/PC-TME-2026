package life;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Deliberately UNSYNCHRONIZED Game of Life state.
 *
 * This is meant as a lab scaffold: updaters compute into {@code next} while a
 * refresher copies {@code next -> current} concurrently, causing tearing.
 */
public class LifeModel {
	private final int rows;
	private final int cols;

	private final boolean[][] current;
	private final boolean[][] next;

	private final AtomicInteger updateCount = new AtomicInteger(0);
	private final AtomicInteger refreshCount = new AtomicInteger(0);
	private volatile boolean [][] resetTo = null;

	public LifeModel(int rows, int cols) {
		if (rows <= 0 || cols <= 0) {
			throw new IllegalArgumentException("rows/cols must be > 0");
		}
		this.rows = rows;
		this.cols = cols;
		this.current = new boolean[rows][cols];
		this.next = new boolean[rows][cols];
	}

	/**
	 * Computes and updates the "next" state.
	 * Works on a row range [startRow, endRow).
	 *
	 */
	public void updateNext(int startRow, int endRow) {		
		int sr = Math.max(0, startRow);
		int er = Math.min(rows, endRow);
		// for every cell in range
		for (int r = sr; r < er; r++) {
			for (int c = 0; c < cols; c++) {
				int n = countNeighborsToroidal(r, c);
				boolean alive = current[r][c];
				// Apply rules of Game of Life.
				if (alive) {
					if (n < 2 || n > 3) {
						// Underpopulation: alive -> dead OR overpopulation: alive -> dead.
						next[r][c] = false;
					} else {
						// 1 or 2 neighbors: lives on to the next generation.
						next[r][c] = true;
					}
				} else {
					if (n == 3) {
						// Reproduction: dead -> alive.
						next[r][c] = true;
					} else {
						// Remains dead.
						next[r][c] = false;
					}
				}
			}
		}
		// for ui : count number of calls
		updateCount.incrementAndGet();
	}

	/**
	 * Copies next over current.
	 * Current state becomes one step further in simulation.
	 */
	public void refreshCurrent() {
		if (resetTo != null) {
			// If resetTo is set, copy from it instead of next.
			// This allows us to reset the simulation to a known state (e.g. for testing).
			for (int r = 0; r < rows; r++) {
				for (int c = 0; c < cols; c++) {
					current[r][c] = resetTo[r][c];
				}
			}
			resetTo = null;
			refreshCount.set(0);
			updateCount.set(0);
		} else {
			// Normal case: copy from next.
			for (int r = 0; r < rows; r++) {
				for (int c = 0; c < cols; c++) {
					current[r][c] = next[r][c];
				}
			}
			// for ui : count number of calls
			refreshCount.incrementAndGet();
		}
	}

	// Accessors to query and set/modify state.
	public int getRows() {
		return rows;
	}

	public int getCols() {
		return cols;
	}

	public boolean isAlive(int r, int c) {
		return current[r][c];
	}

	/** Sets a cell in the current state. Intended for scene/pattern seeding. */
	public void setAlive(int r, int c, boolean alive) {
		if (r < 0 || r >= rows || c < 0 || c >= cols) {
			return;
		}
		current[r][c] = alive;
		next[r][c] = alive;
	}

	public void clear() {
		for (int r = 0; r < rows; r++) {
			for (int c = 0; c < cols; c++) {
				current[r][c] = false;
				next[r][c] = false;
			}
		}
		updateCount.set(0);
		refreshCount.set(0);
	}
	
	public AtomicInteger getUpdateCount() {
		return updateCount;
	}
	public AtomicInteger getRefreshCount() {
		return refreshCount;
	}
	
	/** For updater : cell index -> number of live neighbors.*/
	private int countNeighborsToroidal(int r, int c) {
		int count = 0;
		for (int dr = -1; dr <= 1; dr++) {
			for (int dc = -1; dc <= 1; dc++) {
				if (dr == 0 && dc == 0) {
					continue;
				}
				int rr = (r + dr + rows) % rows;
				int cc = (c + dc + cols) % cols;
				if (current[rr][cc]) {
					count++;
				}
			}
		}
		return count;
	}

	public void updateFrom(LifeModel mcopy) {
		resetTo = mcopy.current.clone();
	}
	
}
