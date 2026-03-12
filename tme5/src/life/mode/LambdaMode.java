package life.mode;

import java.util.concurrent.atomic.AtomicInteger;

import life.LifeModel;
import life.ui.LifePanel;

public final class LambdaMode implements LifeMode {
	@Override
	public String getName() {
		return "lambda";
	}
	
	@Override
	public LifeModel createModel(int rows, int cols) {
		return new LifeModel(rows, cols);
	}

	@Override
	public void startSimulation(LifeModel model, LifePanel panel, AtomicInteger updateDelayMs, AtomicInteger refreshDelayMs,
			int workers) {
		Thread updater = new Thread(() -> {
			try {
				while (true) {
					model.updateNext(0, model.getRows());
					int d = updateDelayMs.get();
					if (d > 0) {
						Thread.sleep(d);
					}
				}
			} catch (Exception e) {
				e.printStackTrace();
				System.err.println("Thread " + Thread.currentThread().getName() + " quitting");
			}
		}, "updater");
		updater.start();

		Thread refresher = new Thread(() -> {
			try {
				while (true) {
					model.refreshCurrent();
					panel.repaint();
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
