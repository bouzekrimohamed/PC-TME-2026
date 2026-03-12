package life.ui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.text.DecimalFormat;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.WindowConstants;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import life.LifeModel;
import life.pattern.AcornPattern;
import life.pattern.BeaconPattern;
import life.pattern.BlinkerPattern;
import life.pattern.GliderPattern;
import life.pattern.IPattern;
import life.pattern.RPentominoPattern;
import life.pattern.Scenes;
import life.pattern.ToadPattern;

public final class GameOfLifeApp {
	private static String TITLE = "Game of Life";

	private static final int DEFAULT_CELL_SIZE = 7;

	private static final int SLIDER_MIN_MS = 0;
	private static final int SLIDER_MAX_MS = 1000;
	private static final int SLIDER_DEFAULT_ALIVE_PCT = 18;

	public static final class UiHandle {
		private final JFrame frame;
		private final LifePanel panel;

		private UiHandle(JFrame frame, LifePanel panel) {
			this.frame = frame;
			this.panel = panel;
			// kill all threads and JVM ! violent.
			frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
		}

		public JFrame frame() {
			return frame;
		}

		public LifePanel panel() {
			return panel;
		}
	}

	/**
	 * Builds and shows the Swing UI on the EDT.
	 *
	 * This method does NOT start worker threads.
	 */
	public static UiHandle createAndShow(
			LifeModel model,
			AtomicInteger updateDelayMs,
			AtomicInteger refreshDelayMs,
			String title) {
		Objects.requireNonNull(model, "model");
		Objects.requireNonNull(updateDelayMs, "updateDelayMs");
		Objects.requireNonNull(refreshDelayMs, "refreshDelayMs");
		TITLE += " - " + title;
		if (!SwingUtilities.isEventDispatchThread()) {
			final UiHandle[] handle = new UiHandle[1];
			try {
				SwingUtilities.invokeAndWait(() -> handle[0] = createAndShowOnEdt(
						model,
						updateDelayMs,
						refreshDelayMs));
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
			return handle[0];
		}
		return createAndShowOnEdt(model, updateDelayMs, refreshDelayMs);
	}

	private static UiHandle createAndShowOnEdt(
			LifeModel model,
			AtomicInteger updateDelayMs,
			AtomicInteger refreshDelayMs) {
		// Sensible defaults if main didn't set them.
		AtomicInteger aliveProbabilityPct = new AtomicInteger(SLIDER_DEFAULT_ALIVE_PCT);

		IPattern[] patterns = new IPattern[] {
				new RPentominoPattern(),
				new GliderPattern(),
				new BlinkerPattern(),
				new ToadPattern(),
				new BeaconPattern(),
				new AcornPattern()
		};
		AtomicInteger selectedPatternIndex = new AtomicInteger(0);
		AtomicReference<IPattern> selectedPattern = new AtomicReference<>(patterns[0]);
		AtomicInteger selectedOrientation = new AtomicInteger(0);

		LifePanel panel = new LifePanel(model, DEFAULT_CELL_SIZE, selectedPattern, selectedOrientation);
		PatternPanel patternPanel = new PatternPanel(selectedPattern, selectedOrientation, selectedPatternIndex, patterns);

		JLabel updateLabel = new JLabel();
		JLabel refreshLabel = new JLabel();
		JLabel aliveLabel = new JLabel();
		updateLabel.setText("Update delay: " + updateDelayMs.get() + " ms");
		refreshLabel.setText("Refresh delay: " + refreshDelayMs.get() + " ms");
		aliveLabel.setText("Random alive: " + aliveProbabilityPct.get() + "%");

		JTextField fpsField = new JTextField(10);
		fpsField.setEditable(false);
		JTextField refreshCountField = new JTextField(10);
		refreshCountField.setEditable(false);
		JTextField updateCountField = new JTextField(10);
		updateCountField.setEditable(false);
		JTextField ratioField = new JTextField(8);
		ratioField.setEditable(false);

		JSlider updateSlider = new JSlider(SLIDER_MIN_MS, SLIDER_MAX_MS, updateDelayMs.get());
		JSlider refreshSlider = new JSlider(SLIDER_MIN_MS, SLIDER_MAX_MS, refreshDelayMs.get());
		JSlider aliveSlider = new JSlider(0, 100, SLIDER_DEFAULT_ALIVE_PCT);

		JButton resetRandom = new JButton("Randomize");
		resetRandom.addActionListener(e -> {
			double p = aliveProbabilityPct.get() / 100.0;
			// Work in a clean copy to avoid interleaving with threads.
			LifeModel mcopy = new LifeModel(model.getRows(), model.getCols());
			Scenes.seedRandom(mcopy, p);
			model.updateFrom(mcopy);
			panel.repaint();
		});


		ChangeListener listener = new ChangeListener() {
			@Override
			public void stateChanged(ChangeEvent e) {
				updateDelayMs.set(updateSlider.getValue());
				refreshDelayMs.set(refreshSlider.getValue());
				aliveProbabilityPct.set(aliveSlider.getValue());
				updateLabel.setText("Grid Update delay: " + updateDelayMs.get() + " ms (" + delayFpsLabel(updateDelayMs.get()) + ")");
				refreshLabel.setText("Screen Refresh delay: " + refreshDelayMs.get() + " ms (" + delayFpsLabel(refreshDelayMs.get()) + ")");
				resetRandom.setText("Randomize (alive: " + aliveProbabilityPct.get() + "%)");
			}
		};
		updateSlider.addChangeListener(listener);
		refreshSlider.addChangeListener(listener);
		aliveSlider.addChangeListener(listener);
		listener.stateChanged(null); // initialize labels

		JButton resetScene = new JButton("Reset default");
		resetScene.addActionListener(e -> {
			LifeModel mcopy = new LifeModel(model.getRows(), model.getCols());
			Scenes.seedDemoScene(mcopy);
			model.updateFrom(mcopy);
			panel.repaint();
		});


		JPanel controls = new JPanel(new GridLayout(4, 2, 10, 6));
		controls.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
		controls.add(updateLabel);
		controls.add(updateSlider);
		controls.add(refreshLabel);
		controls.add(refreshSlider);
		// controls.add(aliveLabel);
		controls.add(resetScene);
		JPanel p = new JPanel(new FlowLayout());
		p.add(resetRandom);
		p.add(aliveSlider);
		controls.add(p);
		// controls.add(aliveSlider);


		JFrame frame = new JFrame(TITLE);
		frame.setPreferredSize(new Dimension(1024, 768));
		frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
		frame.setLayout(new BorderLayout());

		JPanel vizu = new JPanel(new BorderLayout());
		vizu.add(panel, BorderLayout.CENTER);
		JPanel fpsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
		fpsPanel.setBorder(BorderFactory.createEmptyBorder(6, 8, 6, 8));
		fpsPanel.add(new JLabel("FPS (actual):"));
		fpsPanel.add(fpsField);
		fpsPanel.add(new JLabel("refresh:"));
		fpsPanel.add(refreshCountField);
		fpsPanel.add(new JLabel("update:"));
		fpsPanel.add(updateCountField);
		fpsPanel.add(new JLabel("u/r:"));
		fpsPanel.add(ratioField);
		vizu.add(fpsPanel, BorderLayout.SOUTH);
		frame.add(vizu, BorderLayout.CENTER);

		JPanel right = new JPanel(new BorderLayout(8, 8));
		right.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
		right.add(patternPanel, BorderLayout.CENTER);
		frame.add(right, BorderLayout.EAST);

		frame.add(controls, BorderLayout.SOUTH);
		frame.pack();
		frame.setLocationByPlatform(true);
		frame.setVisible(true);

		DecimalFormat fpsFmt = new DecimalFormat("0.0");
		DecimalFormat ratioFmt = new DecimalFormat("0.0000");
		Timer perfTimer = new Timer(200, e -> {
			double actualFps = panel.avgFps();
			fpsField.setText((actualFps > 0) ? (fpsFmt.format(actualFps) + " fps") : "(warmup)");
			int r = model.getRefreshCount().get();
			int u = model.getUpdateCount().get();
			refreshCountField.setText(Integer.toString(r));
			updateCountField.setText(Integer.toString(u));
			ratioField.setText((r > 0) ? ratioFmt.format(((double) u) / r) : "-");
		});
		perfTimer.start();

		return new UiHandle(frame, panel);
	}

	private static String delayFpsLabel(int delayMs) {
		if (delayMs <= 0) {
			return "no sleep";
		}
		double fps = 1000.0 / delayMs;
		return "~" + new DecimalFormat("0.0").format(fps) + " fps";
	}

}
