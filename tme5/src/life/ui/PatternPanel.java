package life.ui;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;

import life.pattern.IPattern;

/**
 * Right-side panel that keeps pattern name, preview and controls together.
 */
public final class PatternPanel extends JPanel {
	private final JLabel patternLabel;
	private final PatternPreviewPanel preview;

	public PatternPanel(
			AtomicReference<IPattern> selectedPattern,
			AtomicInteger selectedOrientation,
			AtomicInteger selectedPatternIndex,
			IPattern[] patterns
	) {
		super();
		setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
		setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));

		this.preview = new PatternPreviewPanel(selectedPattern, selectedOrientation);
		this.preview.setAlignmentX(Component.CENTER_ALIGNMENT);
		this.patternLabel = new JLabel();
		this.patternLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
		updatePatternLabel(selectedPattern.get());

		JButton changePattern = new JButton("Change pattern");
		changePattern.addActionListener(e -> {
			int next = selectedPatternIndex.updateAndGet(i -> (i + 1) % patterns.length);
			selectedPattern.set(patterns[next]);
			updatePatternLabel(selectedPattern.get());
			preview.repaint();
		});

		JButton rotatePattern = new JButton("Rotate");
		rotatePattern.addActionListener(e -> {
			selectedOrientation.updateAndGet(o -> (o + 1) & 3);
			updatePatternLabel(selectedPattern.get());
			preview.repaint();
		});

		JPanel buttons = new JPanel(new GridLayout(2, 1, 6, 6));
		buttons.add(changePattern);
		buttons.add(rotatePattern);
		buttons.setAlignmentX(Component.CENTER_ALIGNMENT);
		buttons.setMaximumSize(new Dimension(Integer.MAX_VALUE, buttons.getPreferredSize().height));

		add(preview);
		add(Box.createVerticalStrut(6));
		add(patternLabel);
		add(Box.createVerticalStrut(8));
		add(buttons);
		add(Box.createVerticalGlue());
	}

	private void updatePatternLabel(IPattern pattern) {
		patternLabel.setText("Pattern: " + (pattern == null ? "(none)" : pattern.name()));
	}

	public PatternPreviewPanel preview() {
		return preview;
	}
}
