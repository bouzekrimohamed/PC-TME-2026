package life.mode;

import java.util.concurrent.atomic.AtomicInteger;

import life.LifeModel;
import life.ui.LifePanel;

/**
 * Strategy interface used to start a Game of Life simulation.
 * <p>
 * A mode typically (1) creates an appropriate {@link LifeModel} implementation and (2) starts the background threads
 * responsible for updating the model and refreshing the UI.
 */
public interface LifeMode {
	/**
	 * @return a short, stable identifier for this mode (used for selection from the command line).
	 */
	String getName();

	/**
	 * Creates the model instance to be used by this mode.
	 *
	 * @param rows number of rows in the grid
	 * @param cols number of columns in the grid
	 * @return a model instance (may be a {@link LifeModel} subclass embedding synchronization)
	 */
	LifeModel createModel(int rows, int cols);

	/**
	 * Starts the simulation.
	 * <p>
	 * Implementations are expected to spawn the required threads and return.
	 *
	 * @param model the shared model instance to read/write
	 * @param panel the UI component to refresh (typically via {@link LifePanel#repaint()})
	 * @param updateDelayMs shared, mutable delay in milliseconds used by updater thread(s);
	 *                     implementations may ignore it (e.g. if updaters should never sleep)
	 * @param refreshDelayMs shared, mutable delay in milliseconds used by the refresher thread;
	 *                      implementations may ignore it (e.g. if refresh is externally paced)
	 * @param workers requested number of updater threads; ignored by modes that only use a single updater
	 */
	void startSimulation(LifeModel model, LifePanel panel, AtomicInteger updateDelayMs, AtomicInteger refreshDelayMs, int workers);
}