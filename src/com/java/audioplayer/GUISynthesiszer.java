package com.java.audioplayer;

import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Frame;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;
import javax.swing.Box;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JSlider;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.plaf.basic.BasicSliderUI;
import javax.swing.plaf.metal.MetalSliderUI;

import com.java.audioplayer.FLACAudioDecoder.BadFileFormatException;

/**
 * An implementation of the GUI part. use a UI slider to gain random access
 * to any sample position within audio. Therefore, there are two main tasks
 * to do: one is to implement a UI slider, the other is to locate the
 * specific sample corresponding to the progress of the slider specified by
 * user.
 */

/**
 * GUI player implementation
 * 
 * @author JoySanctuary
 *
 */
public final class GUISynthesiszer extends JFrame implements MouseListener,
		KeyListener {

	private ImageIcon startIcon;
	private ImageIcon pauseIcon;
	private ImageIcon preIcon;
	private ImageIcon nextIcon;
	private ImageIcon stopIcon;
	private ImageIcon pandaImage;
	private JButton preButton;
	private JButton playButton;
	private JButton nextButton;
	private JButton stopButton;
	private JButton pandaButton;
	private JLabel timeLabel;
	private JLabel lyricsLabelPre;
	private JLabel lyricsLabelMid;
	private JLabel lyricsLabelNext;
	private Box verticalBox;
	private Container contentPane;
	private boolean playNextSong;
	private boolean playPreviousSong;
	private boolean stopPlaying;
	private boolean playOrNot;
	private BasicSliderUI sliderUi;
	private FontMetrics fontMetrics;
	private Font monospace;
	private String alignment;
	private boolean stillHandlingLyrics;
	private static double request = -0.01;
	private static Path lastPlayed = null;
	private static String lrcExt = ".lrc";
	private static String flacExt = ".flac";
	private static SourceDataLine line = null;
	private static Object lock = new Object();
	private static long clipStartTime;
	private static long currentTimeInMicros;
	private static JSlider slider = new JSlider(SwingConstants.HORIZONTAL, 0,
			10000, 0);
	private static String songName = null;
	private static Path songToPlay = null;
	private static boolean isPlaying = false;
	private static boolean askForAChange = false;
	private static boolean filesNotBeenInitialized = true;
	private static final String[] quotes = {
			"The greater the man, the more restrained his anger.",
			"If you do not learn to think when you are young, you may never learn.",
			"You can't move so fast that you try to change the mores faster than people can accept it. That doesn't mean you don't do anything, but it means that you do the things that need to be done according to priority.",
			"I was taught that the way of progress is neither swift nor easy." };
	private static final long serialVersionUID = 3313777002045451998L;
	private static byte[] sampleInBytes = null;
	private static ArrayList<Path> songsHavePlayed;
	private static boolean wayPlaying = false;
	private static GUISynthesiszer synthesiszer = null;
	private static final String timeLabelInit = " " + '\u264f' + " " + "00:00"
			+ " " + '\u264f' + " ";
	private static final Logger logger = Logger
			.getLogger("com.java.audioplayer");

	static {
		songsHavePlayed = new ArrayList<Path>();
		songToPlay = FileExplorer.songMenu(songsHavePlayed);
		songName = FileExplorer.getSongName(songToPlay);
		try {
			if (songToPlay != null) {
				FLACAudioDecoder.makeInstance(songToPlay);
				if (!askForAChange) {
					askForAChange = !askForAChange;
				}
			} else {
				System.out.println("Cannot find a playable audio file!");
			}
			if (!songsHavePlayed.contains(songToPlay))
				songsHavePlayed.add(songToPlay);
			lastPlayed = songToPlay;
		} catch (IOException | BadFileFormatException e) {
			JOptionPane.showMessageDialog(null,
					"No matched file has been found!", "File Not Found",
					JOptionPane.ERROR_MESSAGE);
			if (songsHavePlayed.contains(songToPlay)) {
				songsHavePlayed.remove(songToPlay);
			}
			FileExplorer.refreshDirectory(flacExt);
			FileExplorer.refreshDirectory(lrcExt);
			if (songsHavePlayed.isEmpty()) {
				songToPlay = FileExplorer.songMenu(songsHavePlayed);
			} else {
				songToPlay = songsHavePlayed.get(songsHavePlayed
						.indexOf(lastPlayed));
			}
		}
	}

	private GUISynthesiszer() {
		super();
		stillHandlingLyrics = true;
		this.setResizable(false);
		playNextSong = false;
		playPreviousSong = false;
		stopPlaying = false;
		// this.setPreferredSize(new Dimension(width, height));
		monospace = new Font("Monospace", Font.TRUETYPE_FONT, 22);
		sliderUi = new MetalSliderUI();
		sliderUi.installUI(slider);
		slider.setPreferredSize(new Dimension(700, 35));
		slider.setUI(sliderUi);
		slider.setEnabled(false);
		preIcon = new ImageIcon(getClass()
				.getResource("previous_128Pixels.png"));
		startIcon = new ImageIcon(getClass().getResource("play_128Pixels.png"));
		nextIcon = new ImageIcon(getClass().getResource("next_128Pixels.png"));
		pauseIcon = new ImageIcon(getClass().getResource("pause_128Pixels.png"));
		stopIcon = new ImageIcon(getClass().getResource("stop_128Pixels.png"));
		pandaImage = new ImageIcon(getClass()
				.getResource("panda_128Pixels.png"));
		pandaButton = new JButton();
		preButton = new JButton();
		playButton = new JButton();
		nextButton = new JButton();
		stopButton = new JButton();
		pandaButton.setPreferredSize(new Dimension(128, 128));
		playButton.setPreferredSize(new Dimension(128, 128));
		stopButton.setPreferredSize(new Dimension(128, 128));
		preButton.setPreferredSize(new Dimension(128, 128));
		nextButton.setPreferredSize(new Dimension(128, 128));
		preButton.setIcon(preIcon);
		playButton.setIcon(startIcon);
		nextButton.setIcon(nextIcon);
		stopButton.setIcon(stopIcon);
		pandaButton.setIcon(pandaImage);
		// to hinder the extra area excluded in the icon from being
		// highlighted when doing mouse click.
		playButton.setContentAreaFilled(false);
		stopButton.setContentAreaFilled(false);
		preButton.setContentAreaFilled(false);
		nextButton.setContentAreaFilled(false);
		// pandaButton.setContentAreaFilled(true);
		timeLabel = new JLabel();
		alignment = "     ";
		timeLabel.setText(alignment + timeLabelInit);
		timeLabel.setPreferredSize(new Dimension(180, 128));
		timeLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
		Box verticalLyricsBox = Box.createVerticalBox();
		lyricsLabelPre = new JLabel();
		lyricsLabelMid = new JLabel();
		lyricsLabelNext = new JLabel();
		fontMetrics = lyricsLabelMid.getFontMetrics(monospace);
		Box horizontalBox = Box.createHorizontalBox();
		horizontalBox.setPreferredSize(new Dimension(690, 128));
		// horizontalBox.add(Box.createHorizontalStrut(30));
		horizontalBox.add(timeLabel);
		horizontalBox.add(preButton);
		// horizontalBox.add(Box.createHorizontalStrut(30));
		horizontalBox.add(playButton);
		// horizontalBox.add(Box.createHorizontalStrut(30));
		horizontalBox.add(nextButton);
		// horizontalBox.add(Box.createHorizontalStrut(30));
		horizontalBox.add(stopButton);
		// horizontalBox.add(Box.createHorizontalStrut(30));
		contentPane = getContentPane();
		contentPane.setBackground(Color.LIGHT_GRAY);
		playButton.setBorderPainted(false);
		playButton.setBackground(Color.LIGHT_GRAY);
		stopButton.setBorderPainted(false);
		stopButton.setBackground(Color.LIGHT_GRAY);
		preButton.setBorderPainted(false);
		preButton.setBackground(Color.LIGHT_GRAY);
		nextButton.setBorderPainted(false);
		nextButton.setBackground(Color.LIGHT_GRAY);
		timeLabel.setBackground(new Color(0x003366));
		pandaButton.setBorderPainted(false);
		// pandaButton.setBackground(Color.LIGHT_GRAY);
		slider.setBackground(Color.LIGHT_GRAY);
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		verticalBox = Box.createVerticalBox();
		pandaButton.setAlignmentX(Component.CENTER_ALIGNMENT);
		// verticalBox.add(pandaButton);
		slider.setAlignmentX(Component.CENTER_ALIGNMENT);
		timeLabel.setFont(new Font(Font.SANS_SERIF, Font.TRUETYPE_FONT, 22));
		lyricsLabelPre.setFont(monospace);
		lyricsLabelPre.setAlignmentX(Component.LEFT_ALIGNMENT);
		lyricsLabelPre.setForeground(new Color(0xFFFFFF));
		lyricsLabelMid.setFont(monospace);
		lyricsLabelMid.setAlignmentX(Component.LEFT_ALIGNMENT);
		lyricsLabelMid.setForeground(new Color(0xFFFFFF));
		lyricsLabelNext.setFont(monospace);
		lyricsLabelNext.setAlignmentX(Component.LEFT_ALIGNMENT);
		lyricsLabelNext.setForeground(new Color(0xFFFFFF));
		lyricsLabelPre.setText(quotes[(int) (quotes.length * Math.random())]);
		lyricsLabelMid.setText(quotes[(int) (quotes.length * Math.random())]);
		lyricsLabelNext.setText(quotes[(int) (quotes.length * Math.random())]);
		verticalLyricsBox.add(lyricsLabelPre);
		verticalLyricsBox.add(lyricsLabelMid);
		verticalLyricsBox.add(lyricsLabelNext);
		verticalLyricsBox.add(slider);
		verticalLyricsBox.setPreferredSize(new Dimension(564, 128));
		Box horizonLyricsBox = Box.createHorizontalBox();
		Box verticalTimeAndSearchBox = Box.createVerticalBox();
		verticalTimeAndSearchBox.add(pandaButton);
		horizonLyricsBox.add(verticalTimeAndSearchBox);
		horizonLyricsBox.add(verticalLyricsBox);
		verticalBox.add(horizonLyricsBox);
		verticalBox.add(horizontalBox);
		verticalBox.setAlignmentX(Component.CENTER_ALIGNMENT);
		contentPane.add(verticalBox);
		timeLabel.addKeyListener(this);
		pandaButton.addMouseListener(this);
		preButton.addMouseListener(this);
		playButton.addMouseListener(this);
		nextButton.addMouseListener(this);
		stopButton.addMouseListener(this);
		slider.addMouseListener(this);
		contentPane.addKeyListener(this);
		pandaButton.addKeyListener(this);
		preButton.addKeyListener(this);
		playButton.addKeyListener(this);
		nextButton.addKeyListener(this);
		stopButton.addKeyListener(this);
		slider.addKeyListener(this);
		pandaButton.setFocusable(true);
		playButton.setFocusable(true);
		preButton.setFocusable(true);
		stopButton.setFocusable(true);
		nextButton.setFocusable(true);
		pandaButton.setFocusTraversalKeysEnabled(false);
		preButton.setFocusTraversalKeysEnabled(false);
		stopButton.setFocusTraversalKeysEnabled(false);
		nextButton.setFocusTraversalKeysEnabled(false);
		isPlaying = false;
		this.pack();
		contentPane.enableInputMethods(false);
		pandaButton.enableInputMethods(false);
		playButton.enableInputMethods(false);
		stopButton.enableInputMethods(false);
		preButton.enableInputMethods(false);
		nextButton.enableInputMethods(false);
		slider.enableInputMethods(false);
		timeLabel.enableInputMethods(false);
		GraphicsDevice gd = GraphicsEnvironment.getLocalGraphicsEnvironment()
				.getDefaultScreenDevice();
		int screenWidth = gd.getDisplayMode().getWidth();
		int screenHeight = gd.getDisplayMode().getHeight();
		this.setLocation(screenWidth / 2 - 346, screenHeight / 2 - 256);
		contentPane.requestFocusInWindow();
		this.setVisible(true);
	}

	private static void setSliderPosition(final double t) {
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				if (isPlaying && !slider.getValueIsAdjusting())
					slider.setValue((int) Math.round(t * slider.getMaximum()));
			}
		});
	}

	/**
	 * make GUIGenerate a singleton class.
	 * 
	 * @param frameTitle
	 * @return
	 */
	public static GUISynthesiszer GUIsynthesiszerInitializer() {
		if (synthesiszer == null) {
			synthesiszer = new GUISynthesiszer();
			synthesiszer
					.setTitle(quotes[(int) (quotes.length * Math.random())]);
			return synthesiszer;
		} else {
			return synthesiszer;
		}
	}

	public static void main(String[] args) throws IOException {
		GUIsynthesiszerInitializer();
	}

	public static Boolean wayPlaying() {
		return wayPlaying;
	}

	@Override
	public void mouseClicked(MouseEvent e) {
		try {
			if (e.getButton() != MouseEvent.BUTTON1) {
				return;
			}
			if (e.getSource().equals(this.nextButton)) {
				if (Math.pow(e.getX() - nextButton.getHeight() / 2, 2)
						+ Math.pow(e.getY() - nextButton.getHeight() / 2, 2) > Math
							.pow(nextButton.getHeight() / 2, 2)) {
					return;
				} else {
					Thread.sleep(50);
					playNextSong = true;
					new requestHandler("playNextSong");
				}
			} else if (e.getSource().equals(this.preButton)) {
				if (Math.pow(e.getX() - preButton.getHeight() / 2, 2)
						+ Math.pow(e.getY() - preButton.getHeight() / 2, 2) > Math
							.pow(preButton.getHeight() / 2, 2)) {
					return;
				} else {
					Thread.sleep(50);
					playPreviousSong = true;
					new requestHandler("playPreviousSong");
				}
			} else if (e.getSource().equals(this.stopButton)) {
				if (Math.pow(e.getX() - stopButton.getHeight() / 2, 2)
						+ Math.pow(e.getY() - stopButton.getHeight() / 2, 2) > Math
							.pow(stopButton.getHeight() / 2, 2)) {
					return;
				} else {
					Thread.sleep(50);
					stopPlaying = true;
					new requestHandler("stopPlaying");
				}
			} else if (e.getSource().equals(playButton)) {
				if (Math.pow(e.getX() - playButton.getHeight() / 2, 2)
						+ Math.pow(e.getY() - playButton.getHeight() / 2, 2) > Math
							.pow(playButton.getHeight() / 2, 2)) {
					return;
				} else {
					Thread.sleep(50);
					if (FLACAudioDecoder.getInstance() == null) {
						askForAChange = true;
						if (isPlaying) {
							isPlaying = !isPlaying;
						}
						playOrNot = true;
						synchronized (lock) {
							new requestHandler("playOrNot");
							Thread.sleep(300);
							lock.notify();
						}
					} else {
						if (askForAChange == true)
							playOrNot = true;
						else {
							playOrNot = !isPlaying;
						}
						synchronized (lock) {
							new requestHandler("playOrNot");
							Thread.sleep(300);
							lock.notify();
						}
					}
				}
			} else if (e.getSource().equals(pandaButton)) {
				while (true) {
					String rawInput = JOptionPane.showInputDialog(pandaButton,
							"type what you want to hear...", "Song Explorer",
							JOptionPane.QUESTION_MESSAGE);
					if (rawInput == null || rawInput.equals(""))
						return;
					else {
						String searchRes = FileExplorer.fileSearcher(rawInput);
						Thread.sleep(100);
						if (searchRes == null) {
							int answer = JOptionPane
									.showConfirmDialog(
											pandaButton,
											"No matched song has been found. Want next search?",
											"Search Results",
											JOptionPane.YES_NO_OPTION,
											JOptionPane.ERROR_MESSAGE);
							if (answer == JOptionPane.YES_OPTION) {
								FileExplorer.refreshDirectory(flacExt);
								Thread.sleep(100);
								FileExplorer.refreshDirectory(lrcExt);
								Thread.sleep(10);
								continue;
							} else
								return;
						} else {
							int reply = JOptionPane
									.showConfirmDialog(
											pandaButton,
											"Matched song has been found: "
													+ FileExplorer.getSongName(Paths
															.get(searchRes))
													+ "\n \"YES\" to start play, \"NO\" to start next search, \"CANCEL\" to quit searching.",
											"Search Results",
											JOptionPane.YES_NO_CANCEL_OPTION,
											JOptionPane.QUESTION_MESSAGE);
							if (reply == JOptionPane.YES_OPTION) {
								songToPlay = Paths.get(searchRes);
								lastPlayed = songToPlay;
								if (!stopPlaying) {
									stopPlaying = !stopPlaying;
									new requestHandler("stopPlaying");
								}
								while (wayPlaying
										|| FLACAudioDecoder.waySearching()) {
								}
								askForAChange = true;
								Thread.sleep(200);
								if (!songsHavePlayed.contains(songToPlay))
									songsHavePlayed.add(songToPlay);
								if (isPlaying) {
									isPlaying = !isPlaying;
								}
								playOrNot = true;
								synchronized (lock) {
									new requestHandler("playOrNot");
									Thread.sleep(300);
									lock.notify();
								}
								break;
							} else if (reply == JOptionPane.NO_OPTION) {
								Thread.sleep(100);
								continue;
							} else
								break;
						}
					}

				}
			} else if (e.getSource().equals(slider)) {
				if (slider.isEnabled()) {
					synchronized (lock) {
						double temp = (double) sliderUi.valueForXPosition(e
								.getX()) / slider.getMaximum();
						if (temp < 0)
							request = 0;
						else if (temp >= 1.0) {
							lock.notify();
							return;
						} else
							request = temp;
						lock.notify();
					}
					logger.log(Level.INFO, "Jumping to the selected position.");
				}
			}

		} catch (InterruptedException ex) {
			ex.printStackTrace();
		}
	}

	@Override
	public void mousePressed(MouseEvent e) {
		return;
	}

	@Override
	public void mouseReleased(MouseEvent e) {
		if (e.getSource().equals(slider)) {
			if (slider.isEnabled()) {
				synchronized (lock) {
					if (isPlaying) {
						isPlaying = !isPlaying;
					}
					double temp = (double) sliderUi.valueForXPosition(e.getX())
							/ slider.getMaximum();
					if (temp < 0)
						request = 0;
					else if (temp >= 1.0) {
						lock.notify();
						return;
					} else
						request = temp;
					if (!isPlaying) {
						isPlaying = !isPlaying;
					}
					lock.notify();
				}
			}
		}
	}

	@Override
	public void mouseEntered(MouseEvent e) {
		return;
		// TODO Auto-generated method stub
	}

	@Override
	public void mouseExited(MouseEvent e) {
		return;
	}

	public class requestHandler {
		public requestHandler(String option) {
			try {
				switch (option) {
				// handling play next song
				case "playNextSong":
					logger.log(Level.INFO, "Start playing next audio.");
					while (wayPlaying || FLACAudioDecoder.waySearching()) {
						Thread.sleep(1);
					}
					askForAChange = true;
					if (stopPlaying) {
						stopPlaying = !stopPlaying;
					}
					FLACAudioDecoder.closeFile();
					Thread.sleep(200);
					lyricsLabelPre.setText(quotes[(int) (quotes.length * Math
							.random())]);
					lyricsLabelMid.setText(quotes[(int) (quotes.length * Math
							.random())]);
					lyricsLabelNext.setText(quotes[(int) (quotes.length * Math
							.random())]);
					timeLabel.setText(alignment + timeLabelInit);
					if (playNextSong) {
						playNextSong = !playNextSong;
					}
					if (songsHavePlayed.indexOf(lastPlayed) == (songsHavePlayed
							.size() - 1))
						songToPlay = FileExplorer.songMenu(songsHavePlayed);
					else
						songToPlay = songsHavePlayed.get(songsHavePlayed
								.indexOf(lastPlayed) + 1);
					if (songToPlay != null) {
						FLACAudioDecoder.makeInstance(songToPlay);
						lastPlayed = songToPlay;
					} else {
						songToPlay = lastPlayed;
						FLACAudioDecoder.makeInstance(songToPlay);
						if (!stopPlaying) {
							stopPlaying = !stopPlaying;
						}
						playButton.setIcon(startIcon);
						synthesiszer
								.setTitle(quotes[(int) (quotes.length * Math
										.random())]);
						new requestHandler("stopPlaying");
						return;
					}
					if (!songsHavePlayed.contains(songToPlay)) {
						songsHavePlayed.add(songToPlay);
					}
					Thread.sleep(50);
					if (isPlaying) {
						isPlaying = !isPlaying;
					}
					if (!isPlaying) {
						isPlaying = !isPlaying;
						playButton.setIcon(pauseIcon);
						stillHandlingLyrics = true;
						Thread audioThread = new Thread(new AudioParser());
						audioThread.setDaemon(true);
						audioThread.start();
						Thread timeThread = new Thread(new LyricsParser());
						timeThread.setDaemon(true);
						timeThread.start();
					}
					break;
				// handling play previous song
				case "playPreviousSong":
					logger.log(Level.INFO, "Start playing previous audio.");
					if (stopPlaying) {
						stopPlaying = !stopPlaying;
					}
					while (wayPlaying || FLACAudioDecoder.waySearching()) {
						if (playPreviousSong) {
							playPreviousSong = !playPreviousSong;
						}
					}
					askForAChange = true;
					FLACAudioDecoder.closeFile();
					Thread.sleep(200);
					lyricsLabelPre.setText(quotes[(int) (quotes.length * Math
							.random())]);
					lyricsLabelMid.setText(quotes[(int) (quotes.length * Math
							.random())]);
					lyricsLabelNext.setText(quotes[(int) (quotes.length * Math
							.random())]);
					timeLabel.setText(alignment + timeLabelInit);
					if (playPreviousSong) {
						playPreviousSong = !playPreviousSong;
					}
					// When the previous song is not the last played
					// song.
					if (songsHavePlayed.indexOf(lastPlayed) > 0) {
						songToPlay = songsHavePlayed.get(songsHavePlayed
								.indexOf(lastPlayed) - 1);
						lastPlayed = songToPlay;
						FLACAudioDecoder.makeInstance(songToPlay);
					} else {
						songToPlay = lastPlayed;
						FLACAudioDecoder.makeInstance(songToPlay);
						if (!stopPlaying) {
							stopPlaying = !stopPlaying;
						}
						playButton.setIcon(startIcon);
						synthesiszer
								.setTitle(quotes[(int) (quotes.length * Math
										.random())]);
						new requestHandler("stopPlaying");
						return;
					}
					Thread.sleep(50);
					if (isPlaying) {
						isPlaying = !isPlaying;
					}
					if (!isPlaying) {
						isPlaying = !isPlaying;
						playButton.setIcon(pauseIcon);
						stillHandlingLyrics = true;
						Thread audioThread = new Thread(new AudioParser());
						audioThread.setDaemon(true);
						audioThread.start();
						Thread timeThread = new Thread(new LyricsParser());
						timeThread.setDaemon(true);
						timeThread.start();
					}
					break;
				case "playOrNot":
					if (stopPlaying) {
						stopPlaying = !stopPlaying;
					}
					while (wayPlaying || FLACAudioDecoder.waySearching()) {
						Thread.sleep(1);
					}
					if (FLACAudioDecoder.getInstance() == null) {
						askForAChange = true;
						lyricsLabelPre
								.setText(quotes[(int) (quotes.length * Math
										.random())]);
						lyricsLabelMid
								.setText(quotes[(int) (quotes.length * Math
										.random())]);
						lyricsLabelNext
								.setText(quotes[(int) (quotes.length * Math
										.random())]);
						timeLabel.setText(alignment + timeLabelInit);
						FLACAudioDecoder.makeInstance(songToPlay);
						if (lastPlayed != songToPlay) {
							lastPlayed = songToPlay;
						}
						Thread.sleep(200);
						if (playOrNot) {
							logger.log(Level.INFO, "Start playing audio.");
							if (askForAChange) {
								stillHandlingLyrics = true;
								Thread audioThread = new Thread(
										new AudioParser());
								audioThread.setDaemon(true);
								audioThread.start();
								isPlaying = true;
							}
							isPlaying = playOrNot;
							Thread timeThread = new Thread(new LyricsParser());
							timeThread.setDaemon(true);
							timeThread.start();
							playButton.setIcon(pauseIcon);
						} else {
							logger.log(Level.INFO, "Pause current audio.");
							isPlaying = playOrNot;
							playButton.setIcon(startIcon);
						}
					} else {
						if (playOrNot) {
							if (askForAChange) {
								logger.log(Level.INFO, "Start playing audio.");
								lyricsLabelPre
										.setText(quotes[(int) (quotes.length * Math
												.random())]);
								lyricsLabelMid
										.setText(quotes[(int) (quotes.length * Math
												.random())]);
								lyricsLabelNext
										.setText(quotes[(int) (quotes.length * Math
												.random())]);
								timeLabel.setText(alignment + timeLabelInit);
								Thread.sleep(200);
								stillHandlingLyrics = true;
								Thread audioThread = new Thread(
										new AudioParser());
								audioThread.setDaemon(true);
								audioThread.start();
								Thread timeThread = new Thread(
										new LyricsParser());
								timeThread.setDaemon(true);
								timeThread.start();
							}
							isPlaying = playOrNot;
							playButton.setIcon(pauseIcon);
						} else {
							logger.log(Level.INFO, "Pause current audio.");
							isPlaying = playOrNot;
							playButton.setIcon(startIcon);
						}
					}
					break;
				case "stopPlaying":
					logger.log(Level.INFO, "Stop current audio.");
					while (wayPlaying || FLACAudioDecoder.waySearching()) {
						Thread.sleep(1);
					}
					askForAChange = true;
					Thread.sleep(10);
					synchronized (lock) {
						// reset GUI outlook.
						FLACAudioDecoder.closeFile();
						slider.setValue(slider.getMinimum());
						lock.notify();
					}
					slider.setEnabled(false);
					lyricsLabelPre
							.setText("When the solution is simple, God is answering.");
					lyricsLabelMid
							.setText("When the solution is simple, God is answering.");
					lyricsLabelNext
							.setText("When the solution is simple, God is answering.");
					synthesiszer.setTitle(quotes[(int) (quotes.length * Math
							.random())]);
					if (stopPlaying) {
						stopPlaying = !stopPlaying;
					}
					break;
				default:
					break;
				}
			} catch (IOException | BadFileFormatException
					| InterruptedException e) {
				e.printStackTrace();
			}
		}
	}

	private class AudioParser implements Runnable {

		public AudioParser() {
			if (!FLACAudioDecoder.ifFLACDecoderInitialized()) {
				return;
			}
			logger.log(Level.INFO, "Start decoding codec.");
			try {
				clipStartTime = 0;
				AudioFormat format = new AudioFormat(FLACAudioDecoder
						.getInstance().getSampleRateInHz(), FLACAudioDecoder
						.getInstance().getBitsPerSample(), FLACAudioDecoder
						.getInstance().getNumOfChannels(), true, false);
				line = (SourceDataLine) AudioSystem.getLine(new DataLine.Info(
						SourceDataLine.class, format));
				line.open(format);
				line.start();
				if (filesNotBeenInitialized) {
					filesNotBeenInitialized = !filesNotBeenInitialized;
				}
			} catch (IllegalArgumentException ex) {
				JOptionPane.showMessageDialog(null, "Cannot play \"" + songName
						+ "\" on your current OS.\nSample rate: "
						+ FLACAudioDecoder.getInstance().getSampleRateInHz()
						+ ", sample depth: "
						+ FLACAudioDecoder.getInstance().getBitsPerSample(),
						"Initialization Failed", JOptionPane.ERROR_MESSAGE);
				filesNotBeenInitialized = true;
			} catch (LineUnavailableException e) {
				e.printStackTrace();
			}
		}

		@Override
		public void run() {
			if (filesNotBeenInitialized) {
				stopPlaying = true;
				new requestHandler("stopPlaying");
				try {
					Thread.sleep(10);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				playButton.setIcon(startIcon);
				filesNotBeenInitialized = !filesNotBeenInitialized;
				return;
			}
			if (askForAChange) {
				askForAChange = !askForAChange;
			}
			double sampleRateReciprocal = 1e0;
			double totalSampleReciprocal = 1e0 / FLACAudioDecoder.getInstance()
					.getTotalSamplesInStream();
			if (FLACAudioDecoder.getInstance().getSampleRateInHz() == 0) {
				return;
			} else
				sampleRateReciprocal = 1e0 / FLACAudioDecoder.getInstance()
						.getSampleRateInHz();
			if (FLACAudioDecoder.getInstance().getSampleRateInHz() > 44100) {
				try {
					Thread.sleep(500);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
			int bytesPerSample = FLACAudioDecoder.getInstance()
					.getBitsPerSample() / 8;
			double oneInMillion = 1e0 / 1e6;
			while (!askForAChange) {
				try {
					while (stillHandlingLyrics) {
						Thread.sleep(1);
					}
					if (FLACAudioDecoder.getInstance() == null) {
						break;
					}
					if (playNextSong) {
						break;
					} else if (playPreviousSong) {
						break;
					} else if (stopPlaying) {
						new requestHandler("stopPlaying");
						break;
					} else if (!isPlaying) {
						Thread.sleep(1);
						continue;
					}
					long[][] samples = null;
					if (request >= 0) {
						long samplePos = Math.round(FLACAudioDecoder
								.getInstance().getTotalSamplesInStream()
								* request);
						while (FLACAudioDecoder.waySearching()) {
							wayPlaying = true;
						}
						samples = FLACAudioDecoder.getInstance()
								.findNextDecodeableBlock(samplePos);
						line.flush();
						clipStartTime = line.getMicrosecondPosition()
								- Math.round(1e6 * samplePos
										* sampleRateReciprocal);
						request = -0.01;
						if (!playButton.getIcon().equals(pauseIcon)) {
							Thread.sleep(250);
							playButton.setIcon(pauseIcon);
						}
					} else {
						if (FLACAudioDecoder.getInstance().getCurrentStream() == null)
							return;
						while (FLACAudioDecoder.waySearching()) {
							wayPlaying = true;
						}
						Object[] temp = FLACAudioDecoder.readNextBlock(false);
						if (temp != null) {
							samples = (long[][]) temp[0];
						}
					}
					if (samples == null)
						return;
					// convert samples to channel-interleaved bytes in
					// little-endian
					sampleInBytes = new byte[samples.length * samples[0].length
							* bytesPerSample];
					for (int i = 0, k = 0; i < samples[0].length; i++) {
						for (int ch = 0; ch < samples.length; ch++) {
							long cache = samples[ch][i];
							for (int j = 0; j < bytesPerSample; j++, k++)
								sampleInBytes[k] = (byte) (cache >>> (j << 3));
						}
					}

					line.write(sampleInBytes, 0, sampleInBytes.length);
					wayPlaying = false;
					currentTimeInMicros = line.getMicrosecondPosition()
							- clipStartTime;
					if (FLACAudioDecoder.getInstance() != null) {
						double t = currentTimeInMicros
								* oneInMillion
								* FLACAudioDecoder.getInstance()
										.getSampleRateInHz()
								* totalSampleReciprocal;
						setSliderPosition(t);
						if (currentTimeInMicros * oneInMillion >= FLACAudioDecoder
								.getInstance().getAudioLength() - 1.1) {
							synchronized (lock) {
								int time = 1101;
								while (time > 0) {
									if (askForAChange) {
										line.close();
										line = null;
										return;
									}
									Thread.sleep(1);
									time -= 1;
									currentTimeInMicros += 1e3;
								}
								Thread.sleep(100);
								line.close();
								line = null;
								stopPlaying = true;
								lock.notify();
							}
						}
					}
				} catch (IOException ex) {
					JOptionPane
							.showMessageDialog(synthesiszer,
									"The I/O device is not ready!",
									"I/O Error(s) occurred!",
									JOptionPane.ERROR_MESSAGE);
					ex.printStackTrace();
					new requestHandler("stopPlaying");
				} catch (InterruptedException | BadFileFormatException e) {
					e.printStackTrace();
				}
			}
		}
	}

	@Override
	public void keyTyped(KeyEvent e) {
		// TODO Auto-generated method stub

	}

	@Override
	public void keyPressed(KeyEvent e) {
		int id = e.getID();
		String keyString;
		if (id == KeyEvent.KEY_TYPED) {
			char c = e.getKeyChar();
			keyString = "Key character = '" + c + "'";
		} else {
			int keyCode = e.getKeyCode();
			keyString = "Key code = " + keyCode + "("
					+ KeyEvent.getKeyText(keyCode) + ")";
		}
		int modifierEx = e.getModifiersEx();
		String modString = "extended modifiers = " + modifierEx;
		String tmpString = KeyEvent.getModifiersExText(modifierEx);
		if (tmpString.length() > 0) {
			modString += "(" + tmpString + ")";
		} else {
			modString += "(no extended modifiers)";
		}

		String actionString = "action key?";
		if (e.isActionKey()) {
			actionString += "YES";
		} else {
			actionString += "NO";
		}

		String locationString = "key location: ";
		int location = e.getKeyLocation();
		if (location == KeyEvent.KEY_LOCATION_STANDARD) {
			locationString += "standard";
		} else if (location == KeyEvent.KEY_LOCATION_LEFT) {
			locationString += "left";
		} else if (location == KeyEvent.KEY_LOCATION_RIGHT) {
			locationString += "right";
		} else if (location == KeyEvent.KEY_LOCATION_NUMPAD) {
			locationString += "numpad";
		} else {
			locationString += "unknown";
		}
		logger.log(Level.FINE, keyString + "\t" + modString + "\t"
				+ actionString + "\t" + locationString);

		if (e.getKeyCode() == 39 && e.getModifiersEx() == 128
				|| e.getKeyCode() == 39 && e.getModifiersEx() == 512
				|| e.getKeyCode() == 39 && e.getModifiersEx() == 64) {
			nextButton.requestFocusInWindow();
			playNextSong = true;
			new requestHandler("playNextSong");
		} else if (e.getKeyCode() == 37 && e.getModifiersEx() == 128
				|| e.getKeyCode() == 37 && e.getModifiersEx() == 512
				|| e.getKeyCode() == 37 && e.getModifiersEx() == 64) {
			preButton.requestFocusInWindow();
			playPreviousSong = true;
			new requestHandler("playPreviousSong");
		} else if (e.getKeyCode() == 32) {
			playButton.requestFocusInWindow();
			if (FLACAudioDecoder.getInstance() == null) {
				askForAChange = true;
				if (isPlaying) {
					isPlaying = !isPlaying;
				}
				playOrNot = true;
				synchronized (lock) {
					new requestHandler("playOrNot");
					try {
						Thread.sleep(300);
					} catch (InterruptedException e1) {
						e1.printStackTrace();
					}
					lock.notify();
				}
			} else {
				if (askForAChange == true)
					playOrNot = true;
				else {
					playOrNot = !isPlaying;
				}
				synchronized (lock) {
					new requestHandler("playOrNot");
					try {
						Thread.sleep(300);
					} catch (InterruptedException e1) {
						e1.printStackTrace();
					}
					lock.notify();
				}
			}
		} else if (e.getKeyCode() == 27 && e.getModifiersEx() == 0) {
			if (getState() != JFrame.ICONIFIED)
				this.setState(JFrame.ICONIFIED);
		}
	}

	@Override
	public void keyReleased(KeyEvent e) {
		// TODO Auto-generated method stub
	}

	/**
	 * 1. To show instantaneous time frame in seconds. 2. To show instantaneous
	 * lyric corresponding to current time frame.
	 * 
	 * @author JoySanctuary
	 *
	 */
	private class LyricsParser implements Runnable {
		private float pennySec;
		private HashMap<Float, String> lyricDict;
		private ArrayList<Float> framePoints;
		private FontMetrics fMetrics;
		private int timer;
		private String LastTimeFrame;

		public LyricsParser() {
			songName = FileExplorer.getSongName(songToPlay);
			logger.log(Level.INFO, "Start parsing lyrics.");
			stillHandlingLyrics = true;
			LastTimeFrame = timeLabelInit;
			if (filesNotBeenInitialized) {
				logger.log(Level.SEVERE, "File has not been initialized!");
				return;
			}
			fMetrics = lyricsLabelPre.getFontMetrics(new Font("Monospace",
					Font.TRUETYPE_FONT, 11));
			if (fMetrics.stringWidth(songName) > 230) {
				synthesiszer.setTitle('\u264f' + " "
						+ songName.substring(songName.indexOf('-') + 1) + " "
						+ '\u264f');
			} else
				synthesiszer.setTitle('\u264f' + " " + songName + " "
						+ '\u264f');
			songName += "   ";
			slider.setEnabled(true);
			String lrcPathStr = FileExplorer.getLRCFilePath(songToPlay
					.toString());
			if (lrcPathStr != null) {
				lyricsLabelPre.setForeground(new Color(0x003366));
				lyricsLabelMid.setForeground(new Color(0x003366));
				lyricsLabelNext.setForeground(new Color(0x003366));
				lyricDict = FileExplorer.lrcParser(Paths.get(lrcPathStr));
				// Resort time frames in ascending order.
				Float[] frameKeys = new Float[lyricDict.keySet().size()];
				lyricDict.keySet().toArray(frameKeys);
				Arrays.sort(frameKeys);
				framePoints = new ArrayList<Float>();
				for (Float f : frameKeys)
					framePoints.add(f);
				framePoints.trimToSize();
			} else {
				timer = 0;
				lyricsLabelMid.setForeground(new Color(0x006600));
				lyricsLabelPre.setForeground(new Color(0x006600));
				lyricsLabelNext.setForeground(new Color(0x006600));
				fMetrics = lyricsLabelPre.getFontMetrics(monospace);
				lyricsLabelPre.setText(quotes[(int) (quotes.length * Math
						.random())]);
				lyricsLabelMid.setText(quotes[(int) (quotes.length * Math
						.random())]);
				lyricsLabelNext.setText(quotes[(int) (quotes.length * Math
						.random())]);
			}
			timeLabel.setText(alignment + timeLabelInit);
		}

		@Override
		public void run() {
			if (filesNotBeenInitialized) {
				return;
			}
			int unshownLenMid = 0;
			int unshownLenPre = 0;
			int unshownLenNext = 0;
			String midText = null;
			String preText = null;
			String nextText = null;
			int lenInPixelsMid = -1;
			int lenInPixelsPre = -1;
			int lenInPixelsNext = -1;
			int preIdx = 0;
			int midIdx = 0;
			int nextIdx = 0;
			if (framePoints == null) {
				midText = lyricsLabelMid.getText();
				preText = lyricsLabelPre.getText();
				nextText = lyricsLabelNext.getText();
				lenInPixelsMid = fontMetrics.stringWidth(midText);
				lenInPixelsPre = fMetrics.stringWidth(preText);
				lenInPixelsNext = fMetrics.stringWidth(nextText);
				if (lenInPixelsMid > 475) {
					unshownLenMid = 1;
					midText += " ";
					midText += midText;
				}
				if (lenInPixelsPre > 475) {
					unshownLenPre = 1;
					preText += " ";
					preText += preText;
				}
				if (lenInPixelsNext > 475) {
					unshownLenNext = 1;
					nextText += " ";
					nextText += nextText;
				}
			}
			if (askForAChange) {
				askForAChange = !askForAChange;
			}
			float currentKey = 0.0f;
			stillHandlingLyrics = false;
			while (!playNextSong && !playPreviousSong && !stopPlaying
					&& !askForAChange) {
				try {
					if (FLACAudioDecoder.getInstance() == null) {
						break;
					}
					if (getState() != Frame.ICONIFIED) {

						if (framePoints == null) {
							Thread.sleep(20);
							if (isPlaying) {
								timer += 20;
								if (timer % 200 == 0) {
									if (unshownLenMid > 0) {
										lyricsLabelMid.setText('\u266A'
												+ " "
												+ midText.substring(midIdx,
														midIdx + 50) + " "
												+ '\u266A');
										midIdx += 1;
									}
									if (unshownLenPre > 0) {
										lyricsLabelPre.setText('\u266A'
												+ " "
												+ preText.substring(preIdx,
														preIdx + 50) + " "
												+ '\u266A');
										preIdx += 1;
									}
									if (unshownLenNext > 0) {
										lyricsLabelNext.setText('\u266A'
												+ " "
												+ nextText.substring(nextIdx,
														nextIdx + 50) + " "
												+ '\u266A');
										nextIdx += 1;
									}
								}
								if (preIdx > (preText.length() - 1) / 2) {
									preText = preText.substring((preText
											.length() - 1) / 2 + 1);
									preText += preText;
									preIdx = 0;
								}
								if (midIdx > (midText.length() - 1) / 2) {
									midText = midText.substring((midText
											.length() - 1) / 2 + 1);
									midText += midText;
									midIdx = 0;
								}
								if (nextIdx > (nextText.length() - 1) / 2) {
									nextText = nextText.substring((nextText
											.length() - 1) / 2 + 1);
									nextText += nextText;
									nextIdx = 0;
								}
							}
						} else {
							if (!framePoints.isEmpty()) {
								pennySec = (float) (currentTimeInMicros / 1e4 * 0.01);
								if (pennySec < framePoints.get(0).floatValue()) {
									if (!songName.equals(" ")) {
										songName = " ";
										lyricsLabelPre.setText(songName);
										lyricsLabelMid.setText(songName);
										lyricsLabelNext.setText('\u266A'
												+ " "
												+ lyricDict.get(framePoints
														.get(0)) + " "
												+ '\u266A');
									}
								} else if (pennySec == framePoints.get(0)
										.floatValue()) {
									currentKey = framePoints.get(0);
									if (!songName.equals('\u266A' + " "
											+ lyricDict.get(framePoints.get(0))
											+ " " + '\u266A')) {
										songName = '\u266A'
												+ " "
												+ lyricDict.get(framePoints
														.get(0)) + " "
												+ '\u266A';
										lyricsLabelPre.setText(" ");
										lyricsLabelMid.setText(songName);
										lyricsLabelNext.setText('\u266A'
												+ " "
												+ lyricDict.get(framePoints
														.get(1)) + " "
												+ '\u266A');
									}
								} else if (pennySec < framePoints
										.get(framePoints.size() - 1)) {
									For_Loop: for (Float fpoint : framePoints) {
										if (pennySec < fpoint.floatValue()) {
											if (!songName
													.equals('\u266A'
															+ " "
															+ lyricDict
																	.get(framePoints
																			.get(framePoints
																					.indexOf(fpoint) - 1))
															+ " " + '\u266A')
													|| currentKey != fpoint) {
												currentKey = fpoint;
												songName = '\u266A'
														+ " "
														+ lyricDict
																.get(framePoints
																		.get(framePoints
																				.indexOf(fpoint) - 1))
														+ " " + '\u266A';
												if (framePoints
														.indexOf(currentKey) < 2) {
													lyricsLabelPre.setText(" ");
												} else
													lyricsLabelPre
															.setText('\u266A'
																	+ " "
																	+ lyricDict
																			.get(framePoints
																					.get(framePoints
																							.indexOf(currentKey) - 2))
																	+ " "
																	+ '\u266A');
												lyricsLabelMid
														.setText(songName);
												lyricsLabelNext
														.setText('\u266A'
																+ " "
																+ lyricDict
																		.get(currentKey)
																+ " "
																+ '\u266A');
											}
											break For_Loop;
										}
									}
								} else {
									if (!songName
											.equals('\u266A'
													+ " "
													+ lyricDict.get(framePoints.get(framePoints
															.size() - 1)) + " "
													+ '\u266A')
											|| currentKey >= framePoints
													.get(framePoints.size() - 1)) {
										currentKey = 0;
										songName = '\u266A'
												+ " "
												+ lyricDict
														.get(framePoints
																.get(framePoints
																		.size() - 1))
												+ " " + '\u266A';
										String pre = '\u266A'
												+ " "
												+ lyricDict
														.get(framePoints
																.get(framePoints
																		.size() - 2))
												+ " " + '\u266A';
										lyricsLabelPre.setText(pre);
										lyricsLabelMid.setText(songName);
										lyricsLabelNext.setText(" ");
									}
								}
								Thread.sleep(13);
							}
						}
						Thread.sleep(2);
						// without any interruption
						long min = (long) (currentTimeInMicros / 1e6 / 60);
						long s = (long) (currentTimeInMicros / 1e6) % 60;
						if (s < 10) {
							if (min < 10) {
								if (!LastTimeFrame.equals("0" + min + ":0" + s)) {
									LastTimeFrame = "0" + min + ":0" + s;
									timeLabel.setText(alignment + " "
											+ '\u264f' + " " + LastTimeFrame
											+ " " + '\u264f' + " ");
								}
							} else {
								if (!LastTimeFrame.equals(min + ":0" + s)) {
									LastTimeFrame = min + ":0" + s;
									timeLabel.setText(alignment + " "
											+ '\u264f' + " " + LastTimeFrame
											+ " " + '\u264f' + " ");
								}
							}
						} else {
							if (min < 10) {
								if (!LastTimeFrame.equals("0" + min + ":" + s)) {
									LastTimeFrame = "0" + min + ":" + s;
									timeLabel.setText(alignment + " "
											+ '\u264f' + " " + LastTimeFrame
											+ " " + '\u264f' + " ");
								}
							} else {
								if (!LastTimeFrame.equals(min + ":" + s)) {
									LastTimeFrame = min + ":" + s;
									timeLabel.setText(alignment + " "
											+ '\u264f' + " " + LastTimeFrame
											+ " " + '\u264f' + " ");
								}
							}
						}
					}
					Thread.sleep(5);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
			while (getState() == JFrame.ICONIFIED) {
				try {
					Thread.sleep(50);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
			if (isPlaying) {
				isPlaying = !isPlaying;
			}
			if (!isPlaying) {
				playButton.setIcon(startIcon);
			}
			timeLabel.setText(alignment + timeLabelInit);
		}
	}
}
