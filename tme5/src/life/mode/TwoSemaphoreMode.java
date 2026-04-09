package life.mode;

import java.util.concurrent.atomic.AtomicInteger;

import life.LifeModel;
import life.sync.SimpleSemaphore;
import life.ui.LifePanel;

public final class TwoSemaphoreMode implements LifeMode {
	@Override
	public String getName() {
		return "twosem";
	}

	@Override
	public LifeModel createModel(int rows, int cols) {
		return new LifeModel(rows, cols);
	}

	@Override
	public void startSimulation(LifeModel model, LifePanel panel, AtomicInteger updateDelayMs, AtomicInteger refreshDelayMs,
			int n) {
		int rows = model.getRows();
		SimpleSemaphore ready = new SimpleSemaphore(n);
		SimpleSemaphore done = new SimpleSemaphore(0);

		for (int i = 0; i < n; i++) {
			final int startRow = (i * rows) / n;
			final int endRow = ((i + 1) * rows) / n;
			Thread updater = new Thread(() -> {
				try {
					while (true) {
						ready.acquire();
						model.updateNext(startRow, endRow);
						done.release();
						int d = updateDelayMs.get();
						if (d > 0) {
							Thread.sleep(d);
						}
					}
				} catch (Exception e) {
					e.printStackTrace();
					System.err.println("Thread " + Thread.currentThread().getName() + " quitting");
				}
			}, "updater-" + i);
			updater.start();
		}

		Thread refresher = new Thread(() -> {
			try {
				while (true) {
					done.acquire(n);
					model.refreshCurrent();
					panel.repaint();
					ready.release(n);
					int d = refreshDelayMs.get();
					if (d > 0) {
						Thread.sleep(d);
					}
				}
			} catch (Exception e) {
				e.printStackTrace();
				System.err.println("Thread " + Thread.currentThread().getName() + " quitting");
			}
		}, "refresher");
		refresher.start();
	}
}
