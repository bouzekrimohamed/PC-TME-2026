package life.pattern;

import life.LifeModel;

final class PatternUtil {
	private PatternUtil() {
	}

	static void set(LifeModel model, int r, int c, boolean alive) {
		if (r < 0 || r >= model.getRows() || c < 0 || c >= model.getCols()) {
			return;
		}
		model.setAlive(r, c, alive);
	}

	/**
	 * Rotates a (dr,dc) offset around the anchor by 90 degree steps.
	 *
	 * rotation=1 is 90° clockwise.
	 */
	static int[] rotateOffset(int dr, int dc, int rotations) {
		int r = dr;
		int c = dc;
		int rot = Math.floorMod(rotations, 4);
		for (int i = 0; i < rot; i++) {
			int newR = -c;
			int newC = r;
			r = newR;
			c = newC;
		}
		return new int[] { r, c };
	}
}
