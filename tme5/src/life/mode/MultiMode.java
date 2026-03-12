package life.mode;

import java.util.concurrent.atomic.AtomicInteger;

import life.LifeModel;
import life.ui.LifePanel;

public final class MultiMode implements LifeMode {
	@Override
	public String getName() {
		return "multi";
	}

	@Override
	public LifeModel createModel(int rows, int cols) {
		return new LifeModel(rows, cols);
	}
	
	@Override
	public void startSimulation(LifeModel model, LifePanel panel, AtomicInteger updateDelayMs, AtomicInteger refreshDelayMs,
			int n) {
		int rows = model.getRows();

		for (int i = 0; i < n; i++) {
			int startRow = (i * rows) / n;
			int endRow = ((i + 1) * rows) / n;
			Thread updater = new Thread(new NaiveMode.Updater(model, startRow, endRow, updateDelayMs), "updater-" + i);
			updater.start();
		}

		Thread refresher = new Thread(new NaiveMode.Refresher(model, refreshDelayMs, panel), "refresher");
		refresher.start();
	}

}
