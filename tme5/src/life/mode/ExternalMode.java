package life.mode;

import java.util.concurrent.atomic.AtomicInteger;

import life.LifeModel;
import life.sync.Turn;
import life.ui.LifePanel;

public final class ExternalMode implements LifeMode {
	@Override
	public String getName() {
		return "external";
	}

	@Override
	public LifeModel createModel(int rows, int cols) {
		return new LifeModel(rows, cols);
	}

	@Override
	public void startSimulation(LifeModel model, LifePanel panel, AtomicInteger updateDelayMs, AtomicInteger refreshDelayMs,
			int workers) {
		Turn turn = new Turn();

		Thread updater = new Thread(() -> {
			try {
				while (true) {
					turn.startTurn(true);
					model.updateNext(0, model.getRows());
					turn.endTurn();
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
					turn.startTurn(false);
					model.refreshCurrent();
					panel.repaint();
					turn.endTurn();
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
