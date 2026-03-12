package life.pattern;

import life.LifeModel;

public final class BeaconPattern implements IPattern {
	private static final int[][] OFFSETS = new int[][] {
			{ -1, -1 }, { -1, 0 }, { 0, -1 }, { 0, 0 },
			{ 1, 1 }, { 1, 2 }, { 2, 1 }, { 2, 2 }
	};

	@Override
	public String name() {
		return "Beacon";
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
