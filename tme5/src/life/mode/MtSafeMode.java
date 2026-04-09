package life.mode;

import java.util.concurrent.atomic.AtomicInteger;

import life.LifeModel;
import life.LifeModelSync;
import life.ui.LifePanel;

public final class MtSafeMode implements LifeMode {
	@Override
	public String getName() {
		return "mtsafe";
	}

	@Override
	public LifeModel createModel(int rows, int cols) {
		return new LifeModelSync(rows, cols);
	}

	@Override
	public void startSimulation(LifeModel model, LifePanel panel, AtomicInteger updateDelayMs, AtomicInteger refreshDelayMs,
			int workers) {
		Thread updater = new Thread(new NaiveMode.Updater(model, 0, model.getRows(), updateDelayMs), "updater");
		updater.start();

		Thread refresher = new Thread(new NaiveMode.Refresher(model, refreshDelayMs, panel), "refresher");
		refresher.start();
	}
}
