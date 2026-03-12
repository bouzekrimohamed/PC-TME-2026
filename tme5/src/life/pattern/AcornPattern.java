package life.pattern;

import life.LifeModel;

public final class AcornPattern implements IPattern {
	// Anchor chosen near the middle of the classic acorn to keep rotation stable.
	private static final int[][] OFFSETS = new int[][] {
			{ -1, -2 },
			{ 0, 0 },
			{ 1, -3 }, { 1, -2 }, { 1, 1 }, { 1, 2 }, { 1, 3 }
	};

	@Override
	public String name() {
		return "Acorn";
	}

	@Override
	public void stamp(LifeModel model, int row, int col, int orientation) {
		int o = Math.floorMod(orientation, 4);
		for (int[] d : OFFSETS) {
			int[] rot = PatternUtil.rotateOffset(d[0], d[1], o);
			PatternUtil.set(model, row + rot[0], col + rot[1], true);
		}
	}
}
