package life.pattern;

import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

import life.LifeModel;

public final class Scenes {
	private Scenes() {
	}

	public static void seedRandom(LifeModel model, long seed, double aliveProbability) {
		if (aliveProbability < 0.0 || aliveProbability > 1.0) {
			throw new IllegalArgumentException("aliveProbability must be in [0,1]");
		}
		model.clear();
		Random random = new Random(seed);
		for (int r = 0; r < model.getRows(); r++) {
			for (int c = 0; c < model.getCols(); c++) {
				model.setAlive(r, c, random.nextDouble() < aliveProbability);
			}
		}
	}

	/**
	 * Random scene without any explicit seeding (uses {@link ThreadLocalRandom}).
	 */
	public static void seedRandom(LifeModel model, double aliveProbability) {
		if (aliveProbability < 0.0 || aliveProbability > 1.0) {
			throw new IllegalArgumentException("aliveProbability must be in [0,1]");
		}
		model.clear();
		ThreadLocalRandom random = ThreadLocalRandom.current();
		for (int r = 0; r < model.getRows(); r++) {
			for (int c = 0; c < model.getCols(); c++) {
				model.setAlive(r, c, random.nextDouble() < aliveProbability);
			}
		}
	}

	/** Not random: denser, more structured scene for visible tearing. */
	public static void seedDemoScene(LifeModel model) {
		model.clear();

		IPattern glider = new GliderPattern();
		IPattern blinker = new BlinkerPattern();
		IPattern toad = new ToadPattern();
		IPattern beacon = new BeaconPattern();
		IPattern rpent = new RPentominoPattern();
		IPattern acorn = new AcornPattern();

		// A few gliders, spaced.
		glider.stamp(model, 12, 12, 0);
		glider.stamp(model, 12, 32, 0);
		glider.stamp(model, 12, 52, 0);

		//glider.stamp(model, 66, 48, 3);
		glider.stamp(model, 22, 85, 1);
		glider.stamp(model, 42, 105, 2);

		// Oscillators, spaced.
		blinker.stamp(model, 14, 62, 0);
		blinker.stamp(model, 26, 66, 1);
		toad.stamp(model, 52, 18, 0);
		beacon.stamp(model, 62, 72, 0);
		beacon.stamp(model, 22, 42, 0);

		// Unstable patterns.
//		rpent.stamp(model, 32, 58, 0);
//		acorn.stamp(model, 66, 28, 0);
	}
}
