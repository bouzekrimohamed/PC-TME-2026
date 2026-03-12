package life;

import java.util.Arrays;
import java.util.concurrent.atomic.AtomicInteger;

import life.mode.*;
import life.pattern.Scenes;
import life.ui.GameOfLifeApp;
import life.ui.LifePanel;

public class LifeGame {
	private static final int DEFAULT_ROWS = 80;
	private static final int DEFAULT_COLS = 120;

	private static final int DEFAULT_UPDATE_DELAY_MS = 8;
	private static final int DEFAULT_REFRESH_DELAY_MS = 20;

	public static void main(String[] args) {
		// Workers / modes.
		LifeMode[] modes = new LifeMode[] { 
				// Modes avec 1 updater pour 1 refresher
				new NaiveMode(), // fourni, utilise sleep et aucune protection
				new LambdaMode(), // fourni, naive mais en lambda
// TODO				new MtSafeMode(), // a partir de Naive, protéger LifeModel des data race.
// TODO				new AlternateMode(), // a partir de MtSafe, forcer une alternance avec wait/notify
// TODO				new ExternalMode(), // a partir de Naive, forcer une alternance avec une classe de synchro
				// Modes avec plusieurs updater pour 1 refresher
				new MultiMode(), // base avec N updater, sans protection
//TODO				new TwoSemaphoreMode(), // avec 2 sémaphores (un pour les updaters, un pour le refresher)
//TODO				new SemaphoreMode(), // avec N+1 sémaphores (un par updater, un pour le refresher)
		};

		String mode = "naive";
		if (args.length > 0) {
			mode = args[0];
		}
		int workers = 4;
		if (args.length > 1) {
			try {
				workers = Math.max(1, Integer.parseInt(args[1]));
			} catch (NumberFormatException e) {
				System.err.println("Invalid worker count argument: " + args[1]);
				return;
			}
		}

		LifeMode selectedMode = null;
		for (LifeMode m : modes) {
			if (m.getName().equals(mode)) {
				selectedMode = m;
				break;
			}
		}
		if (selectedMode == null) {
			System.err.println("Unknown mode: " + mode);
			System.err.println(
					"Available modes: " + String.join(", ", Arrays.stream(modes).map(LifeMode::getName).toList()));
			return;
		}
		System.out.println("Starting Game of Life with mode=" + mode + " and workers=" + workers);

		// Core simulation state. Non protégé; mais un mode peut choisir une implantation synchronisée par exemple.
		LifeModel model = selectedMode.createModel(DEFAULT_ROWS, DEFAULT_COLS);
		// pose quelques objets dans la scene.
		Scenes.seedDemoScene(model);

		// Timing knobs shared across threads (updated by Swing sliders).
		// Transmis par référence au GUI swing qui met à jour via sliders, 
		// et au mode qui peut l'utiliser pour faire varier la durée des sleeps
		AtomicInteger updateDelayMs = new AtomicInteger(DEFAULT_UPDATE_DELAY_MS);
		AtomicInteger refreshDelayMs = new AtomicInteger(DEFAULT_REFRESH_DELAY_MS);

		// UI (Swing) created separately; we only keep the panel reference.
		LifePanel panel = GameOfLifeApp.createAndShow(model, updateDelayMs, refreshDelayMs, selectedMode.getName()).panel();

		// Doit : créer les threads de simulation (updaters + refresher) et les démarrer.
		selectedMode.startSimulation(model, panel, updateDelayMs, refreshDelayMs, workers);

		// No explicit interrupt/join of threads.
		// "exit" on closing main window => stops the JVM, hence all threads.
	}
}
