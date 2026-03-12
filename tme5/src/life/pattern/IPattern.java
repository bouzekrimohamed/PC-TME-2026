package life.pattern;

import life.LifeModel;

public interface IPattern {
	String name();

	/**
	 * Stamps the pattern into the model's current state.
	 *
	 * @param model model to mutate
	 * @param row anchor row (pattern-defined; generally "center-ish")
	 * @param col anchor col
	 * @param orientation 0..3 (rotations of 90 degrees)
	 */
	void stamp(LifeModel model, int row, int col, int orientation);
}
