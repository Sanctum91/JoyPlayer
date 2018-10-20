/*
 *    Copyright [2018] [Justin Lee]

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
 */
package com.audioplayer.UIlayout;

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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
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

import com.filesystem.handlers.FileExplorer;
import com.flac.decoder.FLACCodec;
import com.flac.decoder.FLACCodec.BadFileFormatException;
import com.mp3.codec.decoder.BitstreamException;
import com.mp3.codec.decoder.JavaLayerException;
import com.mp3.codec.player.Accessor;

public final class GUISynthesiszer extends JFrame implements MouseListener,
		KeyListener {
	/**
	 * Create ImageIcon fields for UI layout.
	 */
	private ImageIcon startIcon;
	private ImageIcon pauseIcon;
	private ImageIcon preIcon;
	private ImageIcon nextIcon;
	private ImageIcon stopIcon;
	private ImageIcon pandaImage;

	/**
	 * Create JButton fields for UI layout.
	 */
	private JButton preButton;
	private JButton playButton;
	private JButton nextButton;
	private JButton stopButton;
	private JButton pandaButton;

	/**
	 * Create JLabel fields for UI layout.
	 */
	private JLabel timeLabel;
	private JLabel lyricsLabelPre;
	private JLabel lyricsLabelMid;
	private JLabel lyricsLabelNext;

	/**
	 * Create Box field to lay out components in a vertical manner.
	 */
	private Box verticalBox;

	/**
	 * Create Container field for overall layout.
	 */
	private Container contentPane;

	/**
	 * Create Boolean fields for signaling or changing current player state.
	 */
	private boolean playNextSong;
	private boolean playPreviousSong;
	private boolean stopPlaying;
	private boolean playOrPause;

	/**
	 * Create final Strings for representing different requests.
	 */
	private final static String next = "playNextSong";
	private final static String previous = "playPreviousSong";
	private final static String stop = "stopPlaying";
	private final static String playOrNot = "playOrPause";

	/**
	 * Create JSlider field and BasicSliderUI field for JSlider UI decoration.
	 */
	private static JSlider slider = new JSlider(SwingConstants.HORIZONTAL, 0,
			10000, 0);
	private BasicSliderUI sliderUi;

	/**
	 * Create Font relevant fields for font style and pixels.
	 */
	private FontMetrics fontMetrics;
	private Font monospace;

	/**
	 * create String field for layout alignment.
	 */
	private String alignment;

	/**
	 * Create Boolean fields for player state indication and control.
	 */
	private boolean stillHandlingLyrics = true;
	private static boolean isPlaying = false;
	private boolean askForAChange = false;
	private boolean filesNotBeenInitialized = true;
	private static boolean wayPlaying = false;
	private boolean fileIsNotReady = false;

	/**
	 * Create double field for signaling a request ranging from 0 to 1.
	 */
	private static double request = -0.01;

	/**
	 * Create constant String fields for file extensions.
	 */
	private final static String lrcExt = ".lrc";
	private final static String flacExt = ".flac";
	private final static String mp3Ext = ".mp3";

	/**
	 * Create SourceDataLine field which decoded audio data would be written to.
	 */
	private static SourceDataLine line = null;

	/**
	 * Create Object field for thread synchronization.
	 */
	private static final Object lock = new Object();

	/**
	 * Create long field for indicating current time on slider and time label.
	 */
	private long clipStartTime;
	private long currentTimeInMicros;

	/**
	 * Create String field to represent current song name.
	 */
	private String songName = null;

	/**
	 * Create Path field to store current song path and song path of the most
	 * recently played one.
	 */
	private Path songToPlay = null;
	private Path lastPlayed = null;

	/**
	 * Create constant String array to store some quotes which would be randomly
	 * selected to show on the lyric label when the lyric is unavailable.
	 */
	private static final String[] quotes = {
			"When the solution is simple, God is answering.",
			"The greater the man, the more restrained his anger.",
			"If you do not learn to think when you are young, you may never learn.",
			"You can't move so fast that you try to change the mores faster than people can accept it. That doesn't mean you don't do anything, but it means that you do the things that need to be done according to priority.",
			"I was taught that the way of progress is neither swift nor easy." };

	// serial number
	private static final long serialVersionUID = 3313777002045451998L;

	/**
	 * Create byte array to store decode audio data.
	 */
	private byte[] sampleInBytes = null;

	/**
	 * Create ArrayList of Class Path to store song paths that have been played
	 * at least once.
	 */
	private ArrayList<Path> songsHavePlayed;

	/**
	 * Create GUISynthesiszer field.
	 */
	private static GUISynthesiszer synthesiszer = null;

	/**
	 * Create constant String to represent the initial content of time label.
	 */
	private static final String timeLabelInit = " " + '\u264f' + " " + "00:00"
			+ " " + '\u264f' + " ";

	/**
	 * Create Logger field for logging.
	 */
	private static final Logger logger = Logger
			.getLogger("com.audioplayer.UIlayout");

	/**
	 * Create MP3Codec field for decoding and playing mp3 encoded audio.
	 */
	private Accessor mp3Invoker = null;

	/**
	 * Create float field for store total duration of current mp3 audio.
	 */
	private float mp3Duration = 0.0f;

	private GUISynthesiszer() {
		super();
		songsHavePlayed = new ArrayList<Path>();
		songToPlay = FileExplorer.songMenu(songsHavePlayed);
		try {
			if (songToPlay != null) {
				songName = FileExplorer.getSongName(songToPlay);
				if (songToPlay.toString().endsWith(flacExt)) {
					FLACCodec.createInstance(songToPlay);
				}
				if (!askForAChange) {
					askForAChange = !askForAChange;
				}
			} else {
				if (!fileIsNotReady) {
					fileIsNotReady = !fileIsNotReady;
				}
				logger.log(Level.WARNING, "Application is not ready!");
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
			FileExplorer.refreshDirectory(mp3Ext);
			FileExplorer.refreshDirectory(lrcExt);
			if (songsHavePlayed.isEmpty()) {
				songToPlay = FileExplorer.songMenu(songsHavePlayed);
			} else {
				songToPlay = songsHavePlayed.get(songsHavePlayed
						.indexOf(lastPlayed));
			}
		}
		if (fileIsNotReady) {
			return;
		}
		// Disable resizing.
		this.setResizable(false);
		playNextSong = false;
		playPreviousSong = false;
		stopPlaying = false;
		// this.setPreferredSize(new Dimension(width, height));

		// Initialize layout components.
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

		// set preferred dimensions for four buttons.
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

		// Construct UI layout
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

		// Add mouse listener and keyboard listener
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

		// SetsetFocusTraversalKeysEnabled for hot key settings.
		pandaButton.setFocusTraversalKeysEnabled(false);
		preButton.setFocusTraversalKeysEnabled(false);
		stopButton.setFocusTraversalKeysEnabled(false);
		nextButton.setFocusTraversalKeysEnabled(false);

		this.pack();

		// Disable input methods
		contentPane.enableInputMethods(false);
		pandaButton.enableInputMethods(false);
		playButton.enableInputMethods(false);
		stopButton.enableInputMethods(false);
		preButton.enableInputMethods(false);
		nextButton.enableInputMethods(false);
		slider.enableInputMethods(false);
		timeLabel.enableInputMethods(false);

		// Set layout dimension and location on the screen
		GraphicsDevice gd = GraphicsEnvironment.getLocalGraphicsEnvironment()
				.getDefaultScreenDevice();
		int screenWidth = gd.getDisplayMode().getWidth();
		int screenHeight = gd.getDisplayMode().getHeight();
		this.setLocation(screenWidth / 2 - 346, screenHeight / 2 - 256);
		contentPane.requestFocusInWindow();
		this.setVisible(true);
	}

	/**
	 * Set the corresponding slider position based on the given double value.
	 * 
	 * @param t
	 */
	private static void setSliderPosition(final double t) {
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				if (isPlaying && !slider.getValueIsAdjusting())
					slider.setValue((int) Math.round(t * slider.getMaximum()));
			}
		});
	}

	/**
	 * Make GUISynthesiszer a singleton class.
	 * 
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
		// logger.setLevel(Level.OFF);
		GUIsynthesiszerInitializer();
	}

	public static Boolean wayPlaying() {
		return wayPlaying;
	}

	// Mouse click event handler.
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
					new requestHandler(next);
				}
			} else if (e.getSource().equals(this.preButton)) {
				if (Math.pow(e.getX() - preButton.getHeight() / 2, 2)
						+ Math.pow(e.getY() - preButton.getHeight() / 2, 2) > Math
							.pow(preButton.getHeight() / 2, 2)) {
					return;
				} else {
					Thread.sleep(50);
					playPreviousSong = true;
					new requestHandler(previous);
				}
			} else if (e.getSource().equals(this.stopButton)) {
				if (Math.pow(e.getX() - stopButton.getHeight() / 2, 2)
						+ Math.pow(e.getY() - stopButton.getHeight() / 2, 2) > Math
							.pow(stopButton.getHeight() / 2, 2)) {
					return;
				} else {
					Thread.sleep(50);
					stopPlaying = true;
					new requestHandler(stop);
				}
			} else if (e.getSource().equals(this.playButton)) {
				if (Math.pow(e.getX() - playButton.getHeight() / 2, 2)
						+ Math.pow(e.getY() - playButton.getHeight() / 2, 2) > Math
							.pow(playButton.getHeight() / 2, 2)) {
					return;
				} else {
					Thread.sleep(50);
					if (songToPlay.toString().endsWith(flacExt)) {
						if (FLACCodec.getInstance() == null) {
							askForAChange = true;
							if (isPlaying) {
								isPlaying = !isPlaying;
							}
							playOrPause = true;
							synchronized (lock) {
								new requestHandler(playOrNot);
								try {
									Thread.sleep(300);
								} catch (InterruptedException e1) {
									e1.printStackTrace();
								}
								lock.notify();
							}
						} else {
							if (askForAChange == true)
								playOrPause = true;
							else {
								playOrPause = !isPlaying;
							}
							synchronized (lock) {
								new requestHandler(playOrNot);
								try {
									Thread.sleep(300);
								} catch (InterruptedException e1) {
									e1.printStackTrace();
								}
								lock.notify();
							}
						}
					} else if (songToPlay.toString().endsWith(mp3Ext)) {
						if (mp3Invoker == null) {
							askForAChange = true;
							if (isPlaying) {
								isPlaying = !isPlaying;
							}
							playOrPause = true;
							synchronized (lock) {
								new requestHandler(playOrNot);
								try {
									Thread.sleep(300);
								} catch (InterruptedException e1) {
									e1.printStackTrace();
								}
								lock.notify();
							}
						} else {
							if (askForAChange == true)
								playOrPause = true;
							else {
								playOrPause = !isPlaying;
							}
							synchronized (lock) {
								new requestHandler(playOrNot);
								try {
									Thread.sleep(300);
								} catch (InterruptedException e1) {
									e1.printStackTrace();
								}
								lock.notify();
							}
						}
					}
				}
			} else if (e.getSource().equals(this.pandaButton)) {
				while (true) {
					String rawInput = JOptionPane.showInputDialog(pandaButton,
							"Type in what you want to hear...",
							"Song Explorer", JOptionPane.INFORMATION_MESSAGE);
					if (rawInput == null) {
						logger.log(Level.WARNING,
								"The input in not valid, please check again.");
						return;
					} else {
						Path searchRes = FileExplorer.fileSearcher(rawInput);
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
								FileExplorer.refreshDirectory(mp3Ext);
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
													+ FileExplorer
															.getSongName(searchRes)
													+ "\n \"YES\" to start play, \"NO\" to start next search, \"CANCEL\" to quit searching.",
											"Search Results",
											JOptionPane.YES_NO_CANCEL_OPTION,
											JOptionPane.QUESTION_MESSAGE);
							if (reply == JOptionPane.YES_OPTION) {
								if (isPlaying) {
									isPlaying = !isPlaying;
									stopPlaying = true;
									new requestHandler(stop);
								}
								songToPlay = searchRes.toAbsolutePath();
								lastPlayed = songToPlay;
								while (wayPlaying || FLACCodec.waySearching()) {
								}
								askForAChange = true;
								Thread.sleep(20);
								if (!songsHavePlayed.contains(songToPlay))
									songsHavePlayed.add(songToPlay);
								if (isPlaying) {
									isPlaying = !isPlaying;
								}
								playOrPause = true;
								synchronized (lock) {
									new requestHandler(playOrNot);
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

	/**
	 * Create an inner class to package and handle different requests for code
	 * simplicity and reuse. There are four different requests, they are as
	 * follows: 1. play next song; 2. play previous song; 3. play or pause; 4.
	 * stop playing;
	 * 
	 * @author JoySanctuary
	 *
	 */
	public class requestHandler {
		// The result of the constructor would depend on the given String
		// parameter.
		public requestHandler(String option) {
			try {
				switch (option) {
				case next:
					logger.log(Level.INFO, "Switching to next song.");
					preConditionHandler();
					if (playNextSong) {
						playNextSong = !playNextSong;
					}
					if (songsHavePlayed.indexOf(lastPlayed) == (songsHavePlayed
							.size() - 1)) {
						songToPlay = FileExplorer.songMenu(songsHavePlayed);
						if (songToPlay == null) {
							songToPlay = songsHavePlayed.get(0);
						}
					} else
						songToPlay = songsHavePlayed.get(songsHavePlayed
								.indexOf(lastPlayed) + 1);
					postConditionHandler();
					break;
				// handling play previous song
				case previous:
					logger.log(Level.INFO, "Start playing previous audio.");
					preConditionHandler();
					if (playPreviousSong) {
						playPreviousSong = !playPreviousSong;
					}
					if (songsHavePlayed.indexOf(lastPlayed) > 0) {
						songToPlay = songsHavePlayed.get(songsHavePlayed
								.indexOf(lastPlayed) - 1);
						lastPlayed = songToPlay;
					} else {
						songToPlay = lastPlayed;
						if (!stopPlaying) {
							stopPlaying = !stopPlaying;
						}
						playButton.setIcon(startIcon);
						new requestHandler(stop);
						return;
					}
					postConditionHandler();
					break;
				case playOrNot:
					if (stopPlaying) {
						stopPlaying = !stopPlaying;
					}
					while (wayPlaying || FLACCodec.waySearching()) {
						Thread.sleep(1);
					}
					if (songToPlay.toString().endsWith(flacExt)) {
						howToPlay(flacExt);
					} else if (songToPlay.getFileName().toString()
							.endsWith(mp3Ext)) {
						howToPlay(mp3Ext);
					}
					break;
				case stop:
					logger.log(Level.INFO, "Stop current audio.");
					while (wayPlaying || FLACCodec.waySearching()) {
						Thread.sleep(1);
					}
					askForAChange = true;
					lyricsLabelPre.setText(quotes[0]);
					lyricsLabelMid.setText(quotes[0]);
					lyricsLabelNext.setText(quotes[0]);
					synthesiszer.setTitle(quotes[0]);
					Thread.sleep(10);
					// reset GUI outlook.
					if (songToPlay.toString().endsWith(flacExt)) {
						FLACCodec.closeFile();
					} else if (songToPlay.toString().endsWith(mp3Ext)) {
						// mp3Invoker.closeStreaming();
						if (isPlaying) {
							playButton.setIcon(startIcon);
							isPlaying = !isPlaying;
						}
						if (mp3Invoker != null) {
							mp3Invoker.closeStreaming();
						}
						mp3Invoker = null;
					}
					Thread.sleep(100);
					slider.setValue(slider.getMinimum());
					slider.setEnabled(false);
					if (stopPlaying) {
						stopPlaying = !stopPlaying;
					}
					break;
				default:
					break;
				}
			} catch (IOException | InterruptedException | BitstreamException e) {
				e.printStackTrace();
			}
		}

		/**
		 * Pre-handler for requests.
		 */
		private void preConditionHandler() {
			try {
				logger.log(Level.INFO, "Preconditioning for switching songs.");
				while (wayPlaying || FLACCodec.waySearching()) {
					Thread.sleep(1);
				}
				logger.log(Level.INFO, "Signal current running thread to quit.");
				askForAChange = true;
				if (stopPlaying) {
					stopPlaying = !stopPlaying;
				}
				Thread.sleep(200);
				timeLabel.setText(alignment + timeLabelInit);
				if (songToPlay.toString().endsWith(flacExt)) {
					logger.log(Level.INFO, "Closing current flac streaming.");
					FLACCodec.closeFile();
				} else if (songToPlay.toString().endsWith(mp3Ext)) {
					logger.log(Level.INFO, "Blocking current mp3 streaming");
					if (mp3Invoker != null) {
						mp3Invoker.closeStreaming();
					}
				}
			} catch (InterruptedException | IOException | BitstreamException e) {
				e.printStackTrace();
			}
		}

		/**
		 * Post-condition handler for requests.
		 */
		private void postConditionHandler() {
			try {
				if (songToPlay != null) {
					if (songToPlay.toString().endsWith(flacExt)) {
						logger.log(Level.INFO,
								"Creating new streaming for flac file.");
						FLACCodec.createInstance(songToPlay);
					} else if (songToPlay.toString().endsWith(mp3Ext)) {
						logger.log(Level.INFO, "Set mp3 invoker to null.");
						mp3Invoker = null;
					}
					lastPlayed = songToPlay;
				} else {
					logger.log(Level.INFO,
							"New song path is invalid, use most recently played one instead.");
					songToPlay = lastPlayed;
					if (!stopPlaying) {
						stopPlaying = !stopPlaying;
					}
					playButton.setIcon(startIcon);
					logger.log(Level.INFO,
							"Stop playing when current song is the most recently played one by default.");
					new requestHandler(stop);
					return;
				}

				if (!songsHavePlayed.contains(songToPlay)) {
					logger.log(Level.INFO,
							"Add new song path to songsHavePlayed.");
					songsHavePlayed.add(songToPlay);
				}
				Thread.sleep(50);
				logger.log(Level.INFO, "Reset the boolean state of isPlaying.");
				if (isPlaying) {
					isPlaying = !isPlaying;
				}
				if (!isPlaying) {
					isPlaying = !isPlaying;
					playButton.setIcon(pauseIcon);
					stillHandlingLyrics = true;
					logger.log(Level.INFO,
							"Executing two threads, one for audio, the other lyrics.");
					ExecutorService executor = Executors.newFixedThreadPool(2);
					executor.execute(new AudioParser());
					executor.execute(new LyricsParser());
					executor.shutdown();
				}
			} catch (InterruptedException | IOException
					| BadFileFormatException e) {
				e.printStackTrace();
			}
		}

		/**
		 * Decide how to play the audio based on given file extension.
		 * 
		 * @param fileExtension
		 */
		private void howToPlay(String fileExtension) {
			try {
				switch (fileExtension) {
				case flacExt:
					if (FLACCodec.getInstance() == null) {
						askForAChange = true;
						timeLabel.setText(alignment + timeLabelInit);
						logger.log(Level.INFO,
								"Creating an flac streaming for the null instance of FLAC decoder.");
						FLACCodec.createInstance(songToPlay);
						logger.log(Level.INFO,
								"Set current song to play as the most recently played one.");
						if (lastPlayed != songToPlay) {
							lastPlayed = songToPlay;
						}
						Thread.sleep(200);
						if (playOrPause) {
							logger.log(Level.INFO, "Start playing audio.");
							if (askForAChange) {
								stillHandlingLyrics = true;
								Thread audioThread = new Thread(
										new AudioParser());
								audioThread.setDaemon(true);
								audioThread.start();
								isPlaying = true;
							}
							isPlaying = playOrPause;
							Thread timeThread = new Thread(new LyricsParser());
							timeThread.setDaemon(true);
							timeThread.start();
							playButton.setIcon(pauseIcon);
						} else {
							logger.log(Level.INFO, "Pause current audio.");
							isPlaying = playOrPause;
							playButton.setIcon(startIcon);
						}
					} else {
						if (playOrPause) {
							if (askForAChange) {
								logger.log(Level.INFO, "Start playing audio.");
								timeLabel.setText(alignment + timeLabelInit);
								Thread.sleep(200);
								stillHandlingLyrics = true;
								logger.log(Level.INFO,
										"Executing two threads, one for audio, the other lyrics.");
								ExecutorService executor = Executors
										.newFixedThreadPool(2);
								executor.execute(new AudioParser());
								executor.execute(new LyricsParser());
								executor.shutdown();
							}
							isPlaying = playOrPause;
							playButton.setIcon(pauseIcon);
						} else {
							logger.log(Level.INFO, "Pause current audio.");
							isPlaying = playOrPause;
							playButton.setIcon(startIcon);
						}
					}
					break;
				case mp3Ext:
					if (mp3Invoker == null) {
						askForAChange = true;
						timeLabel.setText(alignment + timeLabelInit);

						if ((mp3Invoker = Accessor.createInstance(songToPlay)) != null)
							logger.log(Level.INFO,
									"Successfully created an instance for current null instance of mp3 invoker.");
						else
							logger.log(Level.WARNING,
									"Failed to create an instance for current null instance of mp3 invoker. ");
						logger.log(Level.INFO,
								"Set current song to play as the most recently played one.");
						if (lastPlayed != songToPlay) {
							lastPlayed = songToPlay;
						}
						Thread.sleep(200);
						if (playOrPause) {
							logger.log(Level.INFO, "Start playing audio.");
							if (askForAChange) {
								stillHandlingLyrics = true;
								Thread audioThread = new Thread(
										new AudioParser());
								audioThread.setDaemon(true);
								audioThread.start();
								isPlaying = true;
							}
							isPlaying = playOrPause;
							Thread timeThread = new Thread(new LyricsParser());
							timeThread.setDaemon(true);
							timeThread.start();
							playButton.setIcon(pauseIcon);
						} else {
							logger.log(Level.INFO, "Pause current audio.");
							isPlaying = playOrPause;
							playButton.setIcon(startIcon);
						}
					} else {
						if (playOrPause) {
							if (askForAChange) {
								logger.log(Level.INFO, "Start playing audio.");
								timeLabel.setText(alignment + timeLabelInit);
								Thread.sleep(200);
								stillHandlingLyrics = true;
								logger.log(Level.INFO,
										"Executing two threads, one for audio, the other lyrics.");
								ExecutorService executor = Executors
										.newFixedThreadPool(2);
								executor.execute(new AudioParser());
								executor.execute(new LyricsParser());
								executor.shutdown();
							}
							isPlaying = playOrPause;
							playButton.setIcon(pauseIcon);
						} else {
							logger.log(Level.INFO, "Pause current audio.");
							isPlaying = playOrPause;
							playButton.setIcon(startIcon);
						}
					}
					break;
				default:
					break;
				}
			} catch (InterruptedException | BadFileFormatException
					| IOException | JavaLayerException e) {
				e.printStackTrace();
			}
		}
	}

	/**
	 * Initialize current SourceDataLine field with given parameters.
	 * 
	 * @param frequency
	 * @param sampleDepth
	 * @param channels
	 * @param signed
	 * @param bigEndian
	 * @throws LineUnavailableException
	 */
	protected static void sourceDataLineInitializer(int frequency,
			int sampleDepth, int channels, Boolean signed, Boolean bigEndian)
			throws LineUnavailableException {
		logger.log(Level.INFO, "Initializing flac SourceDataLine.");
		AudioFormat format = new AudioFormat(frequency, sampleDepth, channels,
				signed, bigEndian);
		line = (SourceDataLine) AudioSystem.getLine(new DataLine.Info(
				SourceDataLine.class, format));
		line.open(format);
		line.start();
	}

	/**
	 * An audio handler for decoding and playing audio data.
	 * 
	 * @author JoySanctuary
	 *
	 */
	private class AudioParser implements Runnable {

		public AudioParser() {
			logger.log(Level.INFO,
					"Starting thread for decoding flac codec and playing.");
			if (songToPlay.toString().endsWith(flacExt)) {
				logger.log(Level.INFO, "Current song to play is " + songToPlay);
				if (!FLACCodec.isReady()) {
					logger.log(Level.INFO,
							"FLAC decoder is not ready, thread is quitting.");
					return;
				}
				try {
					clipStartTime = 0;
					sourceDataLineInitializer(FLACCodec.getInstance()
							.getSampleRateInHz(), FLACCodec.getInstance()
							.getBitsPerSample(), FLACCodec.getInstance()
							.getNumOfChannels(), true, false);
					if (filesNotBeenInitialized) {
						logger.log(Level.INFO,
								"Cancel wrong state since the stream has been initialized properly.");
						filesNotBeenInitialized = !filesNotBeenInitialized;
					}
				} catch (IllegalArgumentException ex) {
					JOptionPane.showMessageDialog(null, "Cannot play \""
							+ songToPlay.getFileName()
							+ "\" on your current OS.\nSample rate: "
							+ FLACCodec.getInstance().getSampleRateInHz()
							+ ", sample depth: "
							+ FLACCodec.getInstance().getBitsPerSample()
							+ ", channels: "
							+ FLACCodec.getInstance().getNumOfChannels(),
							"Initialization Failed", JOptionPane.ERROR_MESSAGE);
					filesNotBeenInitialized = true;
				} catch (LineUnavailableException e) {
					e.printStackTrace();
				}
			} else if (songToPlay.toString().endsWith(mp3Ext)) {
				logger.log(Level.INFO, "Current song to play is " + songToPlay);
				try {
					if (mp3Invoker == null) {
						filesNotBeenInitialized = true;
						logger.log(Level.INFO,
								"Creating mp3 invoker for decoding and playing current mp3 song to play.");
						if ((mp3Invoker = Accessor.createInstance(songToPlay)) != null)
							logger.log(Level.INFO,
									"Successfully created an instance for current null instance of mp3 invoker.");
						else
							logger.log(Level.WARNING,
									"Failed to create an instance for current null instance of mp3 invoker. ");
						Thread.sleep(10);
						if (mp3Invoker.decodeFrameHeader()) {
							mp3Duration = mp3Invoker.getTotoalDuration();
							clipStartTime = mp3Invoker.getPosition();
							logger.log(Level.INFO,
									"mp3 SourceDataLine initialized successfully.");
							if (filesNotBeenInitialized) {
								filesNotBeenInitialized = !filesNotBeenInitialized;
							}
						} else {
							logger.log(Level.WARNING,
									"Initialize mp3 SourceDataLine has failed.");
						}
					} else {
						if (filesNotBeenInitialized) {
							logger.log(Level.INFO,
									"Cancel wrong state since the stream has been initialized properly.");
							filesNotBeenInitialized = !filesNotBeenInitialized;
						}
						logger.log(Level.INFO, "Initialize mp3 SourceDataLine.");
						if (mp3Invoker.decodeFrameHeader()) {
							mp3Duration = mp3Invoker.getTotoalDuration();
							clipStartTime = mp3Invoker.getPosition();
							logger.log(Level.INFO,
									"mp3 SourceDataLine initialized successfully.");
							if (filesNotBeenInitialized) {
								filesNotBeenInitialized = !filesNotBeenInitialized;
							}
						} else {
							logger.log(Level.WARNING,
									"Initialize mp3 SourceDataLine has failed.");
						}
					}
				} catch (LineUnavailableException | IOException
						| JavaLayerException | InterruptedException e) {
					e.printStackTrace();
				}
			}
		}

		@Override
		public void run() {
			if (filesNotBeenInitialized) {
				logger.log(Level.WARNING,
						"Stream has failed to initialize, thread is quitting.");
				stopPlaying = true;
				new requestHandler(stop);
				try {
					Thread.sleep(10);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				playButton.setIcon(startIcon);
				logger.log(Level.INFO,
						"Reset state of filesNotBeenInitialized for further use.");
				filesNotBeenInitialized = !filesNotBeenInitialized;
				return;
			}
			if (askForAChange) {
				logger.log(Level.INFO, "Cancel the signaling state.");
				askForAChange = !askForAChange;
			}
			try {
				double oneInMillion = 1e0 / 1e6;
				if (songToPlay.toString().endsWith(flacExt)) {
					logger.log(Level.INFO,
							"Prepare some parameters for decoding and playing flac streaming.");
					double sampleRateReciprocal = 1e0;
					double totalSampleReciprocal = 1e0 / FLACCodec
							.getInstance().getTotalSamplesInStream();
					if (FLACCodec.getInstance().getSampleRateInHz() == 0) {
						logger.log(Level.INFO,
								"Sampling rate is invalid for flac streaming, thread is quitting.");
						return;
					} else
						sampleRateReciprocal = 1e0 / FLACCodec.getInstance()
								.getSampleRateInHz();
					if (FLACCodec.getInstance().getSampleRateInHz() > 44100) {
						Thread.sleep(500);
					}
					int bytesPerSample = FLACCodec.getInstance()
							.getBitsPerSample() / 8;

					while (!askForAChange) {
						while (stillHandlingLyrics) {
							Thread.sleep(1);
						}
						if (FLACCodec.getInstance() == null) {
							logger.log(Level.INFO,
									"flac streaming instance is null ,thread is quitting.");
							break;
						}
						if (playNextSong) {
							break;
						} else if (playPreviousSong) {
							break;
						} else if (stopPlaying) {
							new requestHandler(stop);
							break;
						} else if (!isPlaying) {
							Thread.sleep(1);
							continue;
						}
						long[][] samples = null;
						if (request >= 0) {
							long samplePos = Math.round(FLACCodec.getInstance()
									.getTotalSamplesInStream() * request);
							while (FLACCodec.waySearching()) {
								wayPlaying = true;
							}
							samples = FLACCodec.getInstance()
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
							if (FLACCodec.getInstance().getCurrentStream() == null) {
								logger.log(Level.INFO,
										"flac input stream is null, thread is quitting.");
								return;
							}
							while (FLACCodec.waySearching()) {
								wayPlaying = true;
							}
							Object[] temp = FLACCodec.readNextBlock(false);
							if (temp != null) {
								samples = (long[][]) temp[0];
							}
						}
						if (samples == null)
							return;
						// convert samples to channel-interleaved bytes in
						// little-endian
						sampleInBytes = new byte[samples.length
								* samples[0].length * bytesPerSample];
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
						if (FLACCodec.getInstance() != null) {
							double t = currentTimeInMicros
									* oneInMillion
									* FLACCodec.getInstance()
											.getSampleRateInHz()
									* totalSampleReciprocal;
							setSliderPosition(t);
							if (currentTimeInMicros * oneInMillion >= FLACCodec
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
									lock.notify();
									playNextSong = true;
									Thread.sleep(1000);
									new requestHandler(next);
									break;
								}
							}
						}
					}
				} else if (songToPlay.toString().endsWith(mp3Ext)) {
					if (mp3Duration <= 0) {
						logger.log(Level.WARNING,
								"Invalid mp3 audio duration, threading is quitting.");
						return;
					}
					double reciprocalOfDuration = 1 / (mp3Duration * 1e3);
					while (!askForAChange) {
						while (stillHandlingLyrics) {
							Thread.sleep(1);
						}
						while (!isPlaying) {
							Thread.sleep(1);
							continue;
						}
						if (mp3Invoker != null) {
							if (request >= 0) {
								clipStartTime = mp3Invoker.getPosition()
										- Math.round(mp3Invoker
												.readNextDecodebaleFrame(request) * 1e3);
								request = -1.0;
								if (!playButton.getIcon().equals(pauseIcon)) {
									Thread.sleep(250);
									playButton.setIcon(pauseIcon);
								}
							}
							currentTimeInMicros = mp3Invoker.getPosition()
									- clipStartTime;
							if (!mp3Invoker.play()) {
								// stopPlaying = true;
								// new requestHandler(stop);
								playNextSong = true;
								new requestHandler(next);
								break;
							}
						} else {
							stopPlaying = true;
							new requestHandler(stop);
							break;
						}
						setSliderPosition((double) (currentTimeInMicros * reciprocalOfDuration));
					}
				}
			} catch (IOException ex) {
				JOptionPane.showMessageDialog(synthesiszer,
						"The I/O device is not ready!",
						"I/O Error(s) occurred!", JOptionPane.ERROR_MESSAGE);
				ex.printStackTrace();
				new requestHandler(stop);
			} catch (InterruptedException | BadFileFormatException
					| JavaLayerException e) {
				e.printStackTrace();
			} catch (Exception e) {
				e.printStackTrace();
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
			new requestHandler(next);
		} else if (e.getKeyCode() == 37 && e.getModifiersEx() == 128
				|| e.getKeyCode() == 37 && e.getModifiersEx() == 512
				|| e.getKeyCode() == 37 && e.getModifiersEx() == 64) {
			preButton.requestFocusInWindow();
			playPreviousSong = true;
			new requestHandler(previous);
		} else if (e.getKeyCode() == 32) {
			playButton.requestFocusInWindow();
			if (songToPlay.toString().endsWith(flacExt)) {
				if (FLACCodec.getInstance() == null) {
					askForAChange = true;
					if (isPlaying) {
						isPlaying = !isPlaying;
					}
					playOrPause = true;
					synchronized (lock) {
						new requestHandler(playOrNot);
						try {
							Thread.sleep(300);
						} catch (InterruptedException e1) {
							e1.printStackTrace();
						}
						lock.notify();
					}
				} else {
					if (askForAChange == true)
						playOrPause = true;
					else {
						playOrPause = !isPlaying;
					}
					synchronized (lock) {
						new requestHandler(playOrNot);
						try {
							Thread.sleep(300);
						} catch (InterruptedException e1) {
							e1.printStackTrace();
						}
						lock.notify();
					}
				}
			} else if (songToPlay.toString().endsWith(mp3Ext)) {
				if (mp3Invoker == null) {
					askForAChange = true;
					if (isPlaying) {
						isPlaying = !isPlaying;
					}
					playOrPause = true;
					synchronized (lock) {
						new requestHandler(playOrNot);
						try {
							Thread.sleep(300);
						} catch (InterruptedException e1) {
							e1.printStackTrace();
						}
						lock.notify();
					}
				} else {
					if (askForAChange == true)
						playOrPause = true;
					else {
						playOrPause = !isPlaying;
					}
					synchronized (lock) {
						new requestHandler(playOrNot);
						try {
							Thread.sleep(300);
						} catch (InterruptedException e1) {
							e1.printStackTrace();
						}
						lock.notify();
					}
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
	 * The LyricsParser mains has two functionalities: 1. to parse the lyrics if
	 * the given LRC file path exists; 2: Match what lyrics to show on the
	 * labels based on the current position in audio data of the SourceDataLine
	 * instance.
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
			logger.log(Level.INFO,
					"Constructing new thread for parsing lyrics.");
			Path lrcPath = FileExplorer.getLRCFilePath(songToPlay);
			songName = FileExplorer.getSongName(songToPlay);
			LastTimeFrame = timeLabelInit;
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
			if (lrcPath != null) {
				lyricsLabelPre.setForeground(new Color(0x003366));
				lyricsLabelMid.setForeground(new Color(0x003366));
				lyricsLabelNext.setForeground(new Color(0x003366));
				lyricDict = FileExplorer.lrcParser(lrcPath);
				// Resort time frames in ascending order.
				Float[] frameKeys = new Float[lyricDict.keySet().size()];
				lyricDict.keySet().toArray(frameKeys);
				Arrays.sort(frameKeys);
				framePoints = new ArrayList<Float>();
				for (Float f : frameKeys)
					framePoints.add(f);
				framePoints.trimToSize();
			} else {
				logger.log(Level.INFO,
						"Cannot find the lyrics for current song.");
				stillHandlingLyrics = false;
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
			slider.setEnabled(true);
			int unshownLenMid = 0;
			String midText = null;
			int lenInPixelsMid = -1;
			int midIdx = 0;
			if (framePoints == null) {
				midText = lyricsLabelMid.getText();
				lenInPixelsMid = fontMetrics.stringWidth(midText);
				if (lenInPixelsMid > 475) {
					unshownLenMid = 1;
					midText += " ";
					midText += midText;
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
					if (songToPlay.toString().endsWith(flacExt)) {
						if (FLACCodec.getInstance() == null) {
							break;
						}
					} else if (songToPlay.toString().endsWith(mp3Ext)) {
						if (mp3Invoker == null) {
							break;
						}
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
								}
								if (midIdx > (midText.length() - 1) / 2) {
									midText = midText.substring((midText
											.length() - 1) / 2 + 1);
									midText += midText;
									midIdx = 0;
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
			// while (getState() == JFrame.ICONIFIED) {
			// try {
			// Thread.sleep(50);
			// } catch (InterruptedException e) {
			// e.printStackTrace();
			// }
			// }
			lyricsLabelPre.setText(quotes[0]);
			lyricsLabelMid.setText(quotes[0]);
			lyricsLabelNext.setText(quotes[0]);
			synthesiszer.setTitle(quotes[0]);
			playButton.setIcon(startIcon);
			slider.setValue(slider.getMinimum());
			timeLabel.setText(alignment + timeLabelInit);
		}
	}
}
