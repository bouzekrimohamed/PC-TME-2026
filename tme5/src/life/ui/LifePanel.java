package life.ui;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import javax.swing.BorderFactory;
import javax.swing.JPanel;

import life.LifeModel;
import life.pattern.IPattern;

public final class LifePanel extends JPanel {
	private final LifeModel model;
	private final int fallbackCellSize;
	private final AtomicReference<IPattern> selectedPattern;
	private final AtomicInteger selectedOrientation;
	private final AtomicLong lastPaintNs = new AtomicLong(0);
	private final AtomicLong avgFrameNs = new AtomicLong(0);

	public LifePanel(
			LifeModel model,
			int cellSize,
			AtomicReference<IPattern> selectedPattern,
			AtomicInteger selectedOrientation) {
		this.model = model;
		this.fallbackCellSize = Math.max(1, cellSize);
		this.selectedPattern = selectedPattern;
		this.selectedOrientation = selectedOrientation;
		setBackground(Color.WHITE);
		setBorder(BorderFactory.createLineBorder(Color.BLACK));

		addMouseListener(new MouseAdapter() {
			@Override
			public void mousePressed(MouseEvent e) {
				Point p = e.getPoint();
				int cellSize = computeCellSize();
				int offsetX = computeOffsetX(cellSize);
				int offsetY = computeOffsetY(cellSize);
				int x = p.x - offsetX;
				int y = p.y - offsetY;
				if (x < 0 || y < 0) {
					return;
				}
				int c = x / cellSize;
				int r = y / cellSize;
				if (r < 0 || r >= model.getRows() || c < 0 || c >= model.getCols()) {
					return;
				}
				IPattern pattern = LifePanel.this.selectedPattern.get();
				if (pattern == null) {
					return;
				}
				pattern.stamp(model, r, c, LifePanel.this.selectedOrientation.get());
				repaint();
			}
		});
	}

	private int computeCellSize() {
		int w = getWidth();
		int h = getHeight();
		if (w <= 0 || h <= 0) {
			return fallbackCellSize;
		}
		int byWidth = Math.max(1, w / model.getCols());
		int byHeight = Math.max(1, h / model.getRows());
		return Math.max(1, Math.min(byWidth, byHeight));
	}

	private int computeOffsetX(int cellSize) {
		int gridW = model.getCols() * cellSize;
		return Math.max(0, (getWidth() - gridW) / 2);
	}

	private int computeOffsetY(int cellSize) {
		int gridH = model.getRows() * cellSize;
		return Math.max(0, (getHeight() - gridH) / 2);
	}

	@Override
	protected void paintComponent(Graphics g) {
		long t0 = System.nanoTime();
		long prev = lastPaintNs.getAndSet(t0);
		if (prev != 0) {
			long frameNs = t0 - prev;
			if (frameNs > 0) {
				while (true) {
					long old = avgFrameNs.get();
					long next = (old == 0) ? frameNs : ((old + frameNs) / 2);
					if (avgFrameNs.compareAndSet(old, next)) {
						break;
					}
				}
			}
		}
		super.paintComponent(g);
		Graphics2D g2 = (Graphics2D) g;
		int cellSize = computeCellSize();
		int offsetX = computeOffsetX(cellSize);
		int offsetY = computeOffsetY(cellSize);

		g2.setColor(Color.BLACK);
		for (int r = 0; r < model.getRows(); r++) {
			for (int c = 0; c < model.getCols(); c++) {
				if (model.isAlive(r, c)) {
					g2.fillRect(offsetX + c * cellSize, offsetY + r * cellSize, cellSize, cellSize);
				}
			}
		}
		g2.drawRect(offsetX, offsetY, model.getCols() * cellSize, model.getRows() * cellSize);
	}

	/** Smoothed frames-per-second based on actual paints (0 if not enough data yet). */
	public double avgFps() {
		long frameNs = avgFrameNs.get();
		if (frameNs <= 0) {
			return 0.0;
		}
		return 1_000_000_000.0 / frameNs;
	}
}
