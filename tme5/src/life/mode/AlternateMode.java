package life.mode;

import java.util.concurrent.atomic.AtomicInteger;

import life.LifeModel;
import life.LifeModelBlock;
import life.ui.LifePanel;

public final class AlternateMode implements LifeMode {
	@Override
	public String getName() {
		return "alternate";
	}

	@Override
	public LifeModel createModel(int rows, int cols) {
		return new LifeModelBlock(rows, cols);
	}

	@Override
	public void startSimulation(LifeModel model, LifePanel panel, AtomicInteger updateDelayMs, AtomicInteger refreshDelayMs,
			int workers) {
		Thread updater = new Thread(() -> {
			try {
				while (true) {
					model.updateNext(0, model.getRows());
				}
			} catch (Exception e) {
				e.printStackTrace();
				System.err.println("Thread " + Thread.currentThread().getName() + " quitting");
			}
		}, "updater");
		updater.start();

		Thread refresher = new Thread(new NaiveMode.Refresher(model, refreshDelayMs, panel), "refresher");
		refresher.start();
	}
}
