package life.ui;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import javax.swing.BorderFactory;
import javax.swing.JPanel;

import life.LifeModel;
import life.pattern.IPattern;

/**
 * Swing-only preview of the currently selected pattern/orientation.
 */
public final class PatternPreviewPanel extends JPanel {
	private static final int PREVIEW_ROWS = 7;
	private static final int PREVIEW_COLS = 7;

	private final AtomicReference<IPattern> selectedPattern;
	private final AtomicInteger selectedOrientation;
	private final LifeModel previewModel;

	public PatternPreviewPanel(AtomicReference<IPattern> selectedPattern, AtomicInteger selectedOrientation) {
		this.selectedPattern = selectedPattern;
		this.selectedOrientation = selectedOrientation;
		this.previewModel = new LifeModel(PREVIEW_ROWS, PREVIEW_COLS);

		setBackground(Color.WHITE);
		setBorder(BorderFactory.createLineBorder(Color.BLACK));
		setPreferredSize(new java.awt.Dimension(161, 161));
	}

	@Override
	protected void paintComponent(Graphics g) {
		super.paintComponent(g);
		Graphics2D g2 = (Graphics2D) g;

		IPattern pattern = selectedPattern.get();
		int orientation = selectedOrientation.get();

		previewModel.clear();
		if (pattern != null) {
			pattern.stamp(previewModel, PREVIEW_ROWS / 2, PREVIEW_COLS / 2, orientation);
		}

		int cellSize = computeCellSize();
		int offsetX = computeOffsetX(cellSize);
		int offsetY = computeOffsetY(cellSize);

		g2.setColor(Color.BLACK);
		for (int r = 0; r < previewModel.getRows(); r++) {
			for (int c = 0; c < previewModel.getCols(); c++) {
				if (previewModel.isAlive(r, c)) {
					g2.fillRect(offsetX + c * cellSize, offsetY + r * cellSize, cellSize, cellSize);
				}
			}
		}
		g2.drawRect(offsetX, offsetY, previewModel.getCols() * cellSize, previewModel.getRows() * cellSize);
	}

	private int computeCellSize() {
		int w = getWidth();
		int h = getHeight();
		if (w <= 0 || h <= 0) {
			return 8;
		}
		int byWidth = Math.max(1, w / previewModel.getCols());
		int byHeight = Math.max(1, h / previewModel.getRows());
		return Math.max(1, Math.min(byWidth, byHeight));
	}

	private int computeOffsetX(int cellSize) {
		int gridW = previewModel.getCols() * cellSize;
		return Math.max(0, (getWidth() - gridW) / 2);
	}

	private int computeOffsetY(int cellSize) {
		int gridH = previewModel.getRows() * cellSize;
		return Math.max(0, (getHeight() - gridH) / 2);
	}
}
