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
import java.io.BufferedInputStream;
import java.io.EOFException;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
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

/**
 * Frankly speaking, it is actually sort of pain-staking to implement such a
 * program for FLAC audio playing. Divide & conquer, make it as general as you
 * can. Any more lessons from this? Pains and sweets.
 * 
 * @author JoySanctuary
 *
 */
public class FLACAudioHandler {
	private static final Logger logger = Logger
			.getLogger("com.java.audioplayer");
	private static final int valueOfCharf = 0x66;
	private static final int valueOfCharL = 0x4C;
	private static final long streamMarker = 0x664C6143L;
	private static final int bitsofOneByte = 8;
	private static boolean isReady = false;
	private static Stream input;
	private static FLACAudioHandler handler = null;
	private static SourceDataLine line = null;
	private static double request = -0.0001;
	private static GUIGenerator generator = null;
	private static Object lock = new Object();
	private static boolean wayPlaying = false;
	private static boolean waySearching = false;
	private int fixedBlockSize;
	private int sampleRateInHz;
	private int numOfChannels;
	private int bitsPerSample;
	private long totalSamplesInStream;
	private double audioLength;
	private int constantBlockSize;
	private long metadataEndPos;
	public static final String lrcExt = ".lrc";
	private static byte[] sampleInBytes = null;
	private static long clipStartTime;
	private static long currentTimeInMicros;
	private static JSlider slider = new JSlider(SwingConstants.HORIZONTAL, 0,
			10000, 0);
	private static ArrayList<Integer> rawHeader;
	private static ArrayList<Path> songsHavePlayed;
	private static Path songToPlay;
	private static String songName;
	private static Path lastPlayed;
	private static boolean isPlaying;
	private static boolean askForAChange;
	private static boolean filesNotBeenInitialized;
	private static final String timeLabelInit = " " + '\u264f' + " " + "00:00"
			+ " " + '\u264f' + " ";
	private static final int[][] FIXED_PREDICTION_COEFFICIENTS = { {}, { 1 },
			{ 2, -1 }, { 3, -3, 1 }, { 4, -6, 4, -1 }, };
	private static final String[] quotes = {
			"The greater the man, the more restrained his anger.",
			"If you do not learn to think when you are young, you may never learn.",
			"A day is a minature of eternity.",
			"What we think, we become. All that we are arises with our thoughts. With our thoughts, we make the world.",
			"Whoever is careless with the truth in small matters cannot be trusted with important matters.",
			"Insanity is doing the same thing over and over again and expecting different results.",
			"The important thing is not to stop questioning. Curiosity has it own reason for existing.",
			"Few are those who see with their own eyes and feel with their own hearts.",
			"Anger dwells only in the bosom of fools.",
			"Reality is merely an illusion, albeit a very persistent one.",
			"Life is like riding a bicyle, to keep balance you must keep moving.",
			"The intuitive mind is a sacred gift and the rational mind is a faithful servant.",
			"A man should look for what is, and not for what he thinks should be.",
			"I don't believe in the God of theology who rewards good and punishes evil.",
			"Be thankful for what you have, you'll end up having more.",
			"Those who believe they can do something and those who believe they can't are both right.",
			"All our dreams can come true, if we have the courage to pursue them.",
			"Miracles are the natural way of the Universe, our only job is to move our doubting minds aside and let the miracles flow.",
			"The more you praise and celebrate your life, the more there is in life to celebrate.",
			"We have to let go all blaming, all attacking, all jdging, to free our inner selves to attract what we say we want.",
			"My greatest service to others is living happily, and raidating happiness and joy to those around me.",
			"For those who do not live in scarcity, life is unlimited.",
			"What I know for sure is that what you gives comes back to you.",
			"You creates your own reality.",
			"A coincidence is a small miracle in which God chooses to remain anonymous.",
			"Learn to be still. And take your attention away from what you don't want, and all the emotional charge around it, and place your attention on what you wish to experience.",
			"Life is a mirror, and will reflect to the thinker what he thinks into it.",
			"It's the Consciousness with which we participate in activities that makes the difference.",
			"Well-being is making its way to you at all times. If you will relax and find a way to allow it, it will be your experience.",
			"Know that you can have whatever you want, when you ask from a compassionate heart.",
			"It isn't what you have, or who you are, or where you are, or what you are doing that makes you happy or unhappy. It is what you think about.",
			"You become what you think about.",
			"Our consciousness of wealth begins with th realization that we have already arrived!",
			"Celebrate what you want to see more of.",
			"Ask and it will be given to you; seek and you will find; knock and the door will be opened for you.",
			"As you discover your daily good, and believe in it, and think about it, expect it to continue.",
			"When the solution is simple, God is answering.",
			"Understand that the right to choose your own path is a sacred privilege. Use it. Dwell in possibility.",
			"God is all there is - God includes everything, all possibility and all action, for Spirit is the invisible essence and substance of all form.",
			"We each have a sixth sense that is attuned to the oneness dimension in life, providing a means to guide our lives in accord with our ideas.",
			"The most astonishing thing about miracles is that they happen.",
			"The thing always happens that you really believe in; and the belief in a thing makes it happen.",
			"You are to have implicit confidence in your own ability, knowing that it is the nature of thought to externalize your health and affairs, knowing that you are the thinker.",
			"Whatever the mind can conceive and believe, it can acheive.",
			"It is only with the heart that one can see rightly.",
			"You are what you think you are.",
			"The greatest force in the human body is the natural drive of the body to heal itself - but that force is not independent of the belief system. Everything begins with belief. What we believe is the most powerful option of all.",
			"Education is not the filling of a pail, but the lighting of a fire.",
			"Your worst enemy cannot harm you as much as your own unguarded thoughts.",
			"We are shaped by our thoughts, we become what we think. When the mind is pure, joy follows like a shadow that never leaves.",
			"The eyes are not responsible when the mind does the seeing.",
			"There comes a time when the mind takes a higher plane of knowledge but can never prove how it got there.",
			"I lay the battle within my mind, and grant myself peace.",
			"May your mind whirl joyful cartwheels of creativity.",
			"Instead of dwelling on negative thoughts, cause your mind to dwell on peace and joy.",
			"The highest possible stage in moral culture is when we recognize that we ought to control our thoughts.",
			"Nothing is at last sacred, but the integrity of your own mind.",
			"Mind exists as a Principle in the Universe, just as electricity exists as a principle.",
			"Knowledge is of the mind, wisdom is of the soul.",
			"It is a man's own mind, not his enemy or foe, that lures him to evil ways.",
			"The universal Mind contains all knowledge. It is the potential ultimate of all things. To it, all things are possible.",
			"No problem can be solved from the same level of consciousness that created it.",
			"Iron rusts with disuse; water losses its purity with stagnation... Even so does the vigor of the mind.",
			"The first principle is that you must not fool yourself - and you are the easist person to fool.",
			"The most dangerous phrase in the language is, \"We've always done it in this way.\"",
			"I believe that every human mind feels pleasure in doing good to another.",
			"Dare to be naive.",
			"You must not under any pretense allow your mind to dwell on any thought that is not positive, constructive, optimistic, kind.",
			"Happiness depends more on the inward disposition of mind, than on outward circumstances.",
			"Never mind what others do, do better than yourself, beat your own record from day, and you are a success.",
			"Nothing in the world is more dangerous than sincere ignorance and consenticious stupidity.",
			"You can conquer almost any fear if you will only make up your mind to do so. For remember, fear doesn't exist anywhere except in the mind.",
			"The heart is the chief feature of a functioning mind.",
			"You will never do anything in this world without courage. It is the greatest quality of the mind next to honor.",
			"The human mind is our fundamental resource.",
			"The mind has exactly the same power as the hands: not merely to grasp the world, but to change it.",
			"How different our lives are when we really know what is deeply important to us, and keeping that picture in mind, we manage ourselves each day to be and to do what really matters most.",
			"Your thoughts create your reality, your mind is more powerful than you know.",
			"A mind that is stretched by a new experience and never go back to its old dimensions.",
			"One does not become enlightened by imagining figures of light, but by making the darkness conscious.",
			"Be the change you want to see in the world.",
			"When one door closes, another opens. But often we look so long, so regretfully, upon the closed door that we fail to see the one that is opened for us.",
			"You change your life by changing your heart.",
			"The world is perfect. It's a mess. It has always been a mess. We are not going to change it. Our job is to straighten our own lives.",
			"Change what you see by changing how you see.",
			"Open your arms to change, but don't let go of your values.",
			"Progress is impossible without change, and those who cannot change their minds cannot change anything.",
			"If you don't like something, change it. If you can't change it, change your attitude.",
			"Change your thoughts, change your life.",
			"Never believe that a few caring people can't change the world. For, indeed, that's all who ever have.",
			"Just stop it, seriously. Stop doing it long enough to get a glimpse of what the change would actually look like.",
			"We must be the change we wish to see.",
			"The inherent nature of life is constant change. To fear change is to fear life itself.",
			"Change is the essesntial process of all existence.",
			"When you are reluctant to change, think of the beauty of autumn.",
			"The important thing is: to be able, at any moment, to sacrifice what we are for what we could become.",
			"The dream you are living is your creation. It is your perception of reality that you can change at any time.",
			"I believe that everything happens for a reason. People change so that you can learn to let go, things go wrong so thant you can appreciate when they're right, you believe lies so you eventually trust no one but yourself, and sometimes good things fall apart so better things can fall together.",
			"The greatest discovery is that a human being can alter his life by altering his attitude.",
			"To improve is to change, to be perfect is to change often.",
			"If you take the responsibility for your life, you can start changing it.",
			"We cause ourselves untold misery whenever we believe others to be imperfect and try to change them.",
			"Nothing edures but change.",
			"What old people say you cannot do, you try and find that you can. Old deeds for old people, and new deeds for new.",
			"Our grandchildren's grandchildren will shake their heads in shame at some of the beliefs that we hold most dear today - the question is, which ones?",
			"If we don't change, we don't grow. If we don't grow, we aren't really living.",
			"Change always comes bearing gifts.",
			"Forgiveness does not change the past, but it does enlarge the future.",
			"Face the facts of being what you are, for that is what changes what you are.",
			"All change is not growth, as all movement is not forward.",
			"He who rejects change is the architect of decay. The only human institution which rejects progress is the cemetery.",
			"Changes bring opportunity.",
			"The world hates change, yet it is the only thing that has brought progress.",
			"Only the wisest and stupidest of men never change.",
			"Never doubt that a small group of thoughtful, committed people can change the world. Indeed, it is the only thing that ever has.",
			"There is no death, only a change of worlds.",
			"Others do not have to change for us to experience peace of mind.",
			"When we blindly adopt a religion, a political system, a literary dogma, we become automatons. We cease to grow.",
			"We live in a moment of history that is so speeded up that we being to see the present only when it is already disappearing.",
			"Change is such hard work.",
			"You can't move so fast that you try to change the mores faster than people can accept it. That doesn't mean you don't do anything, but it means that you do the things that need to be done according to priority.",
			"I was taught that the way of progress is neither swift nor easy." };
	private static final String flacExt = ".flac";
	/* CRC-8, poly = x^8 + x^2 + x^1 + x^0, init = 0 */

	private static short[] FLAC__crc8_table = { 0x00, 0x07, 0x0E, 0x09, 0x1C,
			0x1B, 0x12, 0x15, 0x38, 0x3F, 0x36, 0x31, 0x24, 0x23, 0x2A, 0x2D,
			0x70, 0x77, 0x7E, 0x79, 0x6C, 0x6B, 0x62, 0x65, 0x48, 0x4F, 0x46,
			0x41, 0x54, 0x53, 0x5A, 0x5D, 0xE0, 0xE7, 0xEE, 0xE9, 0xFC, 0xFB,
			0xF2, 0xF5, 0xD8, 0xDF, 0xD6, 0xD1, 0xC4, 0xC3, 0xCA, 0xCD, 0x90,
			0x97, 0x9E, 0x99, 0x8C, 0x8B, 0x82, 0x85, 0xA8, 0xAF, 0xA6, 0xA1,
			0xB4, 0xB3, 0xBA, 0xBD, 0xC7, 0xC0, 0xC9, 0xCE, 0xDB, 0xDC, 0xD5,
			0xD2, 0xFF, 0xF8, 0xF1, 0xF6, 0xE3, 0xE4, 0xED, 0xEA, 0xB7, 0xB0,
			0xB9, 0xBE, 0xAB, 0xAC, 0xA5, 0xA2, 0x8F, 0x88, 0x81, 0x86, 0x93,
			0x94, 0x9D, 0x9A, 0x27, 0x20, 0x29, 0x2E, 0x3B, 0x3C, 0x35, 0x32,
			0x1F, 0x18, 0x11, 0x16, 0x03, 0x04, 0x0D, 0x0A, 0x57, 0x50, 0x59,
			0x5E, 0x4B, 0x4C, 0x45, 0x42, 0x6F, 0x68, 0x61, 0x66, 0x73, 0x74,
			0x7D, 0x7A, 0x89, 0x8E, 0x87, 0x80, 0x95, 0x92, 0x9B, 0x9C, 0xB1,
			0xB6, 0xBF, 0xB8, 0xAD, 0xAA, 0xA3, 0xA4, 0xF9, 0xFE, 0xF7, 0xF0,
			0xE5, 0xE2, 0xEB, 0xEC, 0xC1, 0xC6, 0xCF, 0xC8, 0xDD, 0xDA, 0xD3,
			0xD4, 0x69, 0x6E, 0x67, 0x60, 0x75, 0x72, 0x7B, 0x7C, 0x51, 0x56,
			0x5F, 0x58, 0x4D, 0x4A, 0x43, 0x44, 0x19, 0x1E, 0x17, 0x10, 0x05,
			0x02, 0x0B, 0x0C, 0x21, 0x26, 0x2F, 0x28, 0x3D, 0x3A, 0x33, 0x34,
			0x4E, 0x49, 0x40, 0x47, 0x52, 0x55, 0x5C, 0x5B, 0x76, 0x71, 0x78,
			0x7F, 0x6A, 0x6D, 0x64, 0x63, 0x3E, 0x39, 0x30, 0x37, 0x22, 0x25,
			0x2C, 0x2B, 0x06, 0x01, 0x08, 0x0F, 0x1A, 0x1D, 0x14, 0x13, 0xAE,
			0xA9, 0xA0, 0xA7, 0xB2, 0xB5, 0xBC, 0xBB, 0x96, 0x91, 0x98, 0x9F,
			0x8A, 0x8D, 0x84, 0x83, 0xDE, 0xD9, 0xD0, 0xD7, 0xC2, 0xC5, 0xCC,
			0xCB, 0xE6, 0xE1, 0xE8, 0xEF, 0xFA, 0xFD, 0xF4, 0xF3 };

	static {
		askForAChange = false;
		filesNotBeenInitialized = true;
		rawHeader = new ArrayList<Integer>();
		isPlaying = false;
		songsHavePlayed = new ArrayList<Path>();
		lastPlayed = null;
		songToPlay = FLACFileExplorer.songMenu(songsHavePlayed);
		if (songToPlay != null) {
			filesNotBeenInitialized = false;
			try {
				handler = new FLACAudioHandler(songToPlay);
				askForAChange = true;
			} catch (IOException | BadFileFormatException e) {
				e.printStackTrace();
			}
		} else {
			System.out.println("Cannot find a playable audio file!");
		}

	}

	private static void setSliderPosition(final double t) {
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				if (isPlaying && !slider.getValueIsAdjusting())
					slider.setValue((int) Math.round(t * slider.getMaximum()));
			}
		});
	}

	public double getAudioLength() {
		return audioLength;
	}

	public int getFixedBlockSize() {
		return fixedBlockSize;
	}

	public int getSampleRateInHz() {
		return sampleRateInHz;
	}

	public int getNumOfChannels() {
		return numOfChannels;
	}

	public int getBitsPerSample() {
		return bitsPerSample;
	}

	private long getTotalSamplesInStream() {
		return totalSamplesInStream;
	}

	private static void closeFile() throws IOException, InterruptedException {
		if (handler != null) {
			handler.close();
			handler = null;
		}
	}

	private FLACAudioHandler(Path songPath) throws IOException,
			BadFileFormatException {
		try {
			songName = FLACFileExplorer.getSongName(songPath.toString());
			input = new Stream(songPath);
			if (!songsHavePlayed.contains(songPath))
				songsHavePlayed.add(songPath);
			lastPlayed = songPath;
		} catch (FileNotFoundException e) {
			JOptionPane.showMessageDialog(null,
					"No matched file has been found!", "File Not Found",
					JOptionPane.ERROR_MESSAGE);
			if (songsHavePlayed.contains(songPath)) {
				songsHavePlayed.remove(songPath);
			}
			FLACFileExplorer.refreshDirectory(flacExt);
			FLACFileExplorer.refreshDirectory(lrcExt);
			if (songsHavePlayed.isEmpty()) {
				songToPlay = FLACFileExplorer.songMenu(songsHavePlayed);
			} else {
				songToPlay = songsHavePlayed.get(songsHavePlayed
						.indexOf(lastPlayed));
			}
			filesNotBeenInitialized = true;
			return;
		}
		/**
		 * <32 bits> "fLaC", the FLAC stream marker in ASCII, meaning byte 0 of
		 * the stream is 0x66, followed by 0x4C 0x61 0x43
		 */
		try {
			long marker = (input.readByte() << bitsofOneByte)
					| input.readByte();
			if (marker != ((valueOfCharf << bitsofOneByte) | valueOfCharL)) {
				while (marker != valueOfCharf) {
					marker = input.readByte();
				}
				marker = (marker << bitsofOneByte) | input.readByte();
			}
			for (int i = 0; i < 2; i++) {
				marker = (marker << bitsofOneByte) | input.readByte();
			}
			while (marker != streamMarker) {
				marker = ((marker & 0xFFFFFF) << bitsofOneByte)
						| input.readByte();
			}
		} catch (EOFException e) {
			filesNotBeenInitialized = true;
			JOptionPane.showMessageDialog(generator,
					"Cannot find valid stream marker!", "Error Occurred",
					JOptionPane.ERROR_MESSAGE);
			throw new BadFileFormatException("Invalid stream marker!");
		}
		/**
		 * METADATA_BLOCK_HEADER handler (There could be more than one metadata
		 * blocks, and all the block headers shall the same size)
		 */
		boolean isLast = false;
		while (!isLast) {
			// process each handler
			isLast = input.readBit(1) == 1;
			int type = input.readBit(7);
			int bytesOfMetadataToFollow = input.readBit(24);
			// parse stream info
			if (type == 0) {
				constantBlockSize = input.readBit(16);
				if (constantBlockSize != input.readBit(16))
					constantBlockSize = -1;
				input.readBit(24);
				input.readBit(24);
				sampleRateInHz = input.readBit(20);
				numOfChannels = input.readBit(3) + 1;
				bitsPerSample = input.readBit(5) + 1;
				totalSamplesInStream = input.readBit(4) << 32;
				totalSamplesInStream += input.readBit(32);
				if (totalSamplesInStream == 0)
					throw new BadFileFormatException("Invalid file format!");
				for (int i = 0; i < 4; i++)
					input.readBit(32);
			} else
				for (int i = 0; i < bytesOfMetadataToFollow; i++)
					input.readBit(8);
		}
		if (sampleRateInHz == 0) {
			throw new BadFileFormatException("Invalid sample rate!");
		}
		metadataEndPos = input.bytePosition;
		audioLength = (double) totalSamplesInStream / sampleRateInHz;
		// use bit rate instead of sample rate when dealing with encoded
		// frames.
		if (!isReady) {
			isReady = !isReady;
		}
	}

	/**
	 * make parent class a singleton class.
	 * 
	 * @return
	 */
	public static void audioPlayerInitializer() {
		if (filesNotBeenInitialized) {
			return;
		}
		while (!isReady) {
			;
		}
		generator = GUIGenerator.GUIGeneratorInitializer();
		handler = null;
	}

	/**
	 * verify CRC-8 code of the given integer array list
	 * 
	 * @param header
	 * @return
	 */
	private static int crc8_Generator(ArrayList<Integer> header) {
		int len = header.size();
		int crc = 0;
		int i = 0;
		while (len > 0) {
			len--;
			crc = FLAC__crc8_table[crc ^ header.get(i)];
			i++;
		}

		return crc;
	}

	private void close() throws IOException {
		input.close();
	}

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
	private static class GUIGenerator extends JFrame implements MouseListener,
			KeyListener {
		/**
		 * 
		 */
		private static final long serialVersionUID = 3313777002045451998L;
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

		private GUIGenerator() {
			super();
			stillHandlingLyrics = true;
			this.setResizable(false);
			askForAChange = false;
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
			preIcon = new ImageIcon(getClass().getResource(
					"previous_128Pixels.png"));
			startIcon = new ImageIcon(getClass().getResource(
					"play_128Pixels.png"));
			nextIcon = new ImageIcon(getClass().getResource(
					"next_128Pixels.png"));
			pauseIcon = new ImageIcon(getClass().getResource(
					"pause_128Pixels.png"));
			stopIcon = new ImageIcon(getClass().getResource(
					"stop_128Pixels.png"));
			pandaImage = new ImageIcon(getClass().getResource(
					"panda_128Pixels.png"));
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
			timeLabel
					.setFont(new Font(Font.SANS_SERIF, Font.TRUETYPE_FONT, 22));
			lyricsLabelPre.setFont(monospace);
			lyricsLabelPre.setAlignmentX(Component.LEFT_ALIGNMENT);
			lyricsLabelPre.setForeground(new Color(0xFFFFFF));
			lyricsLabelMid.setFont(monospace);
			lyricsLabelMid.setAlignmentX(Component.LEFT_ALIGNMENT);
			lyricsLabelMid.setForeground(new Color(0xFFFFFF));
			lyricsLabelNext.setFont(monospace);
			lyricsLabelNext.setAlignmentX(Component.LEFT_ALIGNMENT);
			lyricsLabelNext.setForeground(new Color(0xFFFFFF));
			lyricsLabelPre
					.setText(quotes[(int) (quotes.length * Math.random())]);
			lyricsLabelMid
					.setText(quotes[(int) (quotes.length * Math.random())]);
			lyricsLabelNext
					.setText(quotes[(int) (quotes.length * Math.random())]);
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
			GraphicsDevice gd = GraphicsEnvironment
					.getLocalGraphicsEnvironment().getDefaultScreenDevice();
			int screenWidth = gd.getDisplayMode().getWidth();
			int screenHeight = gd.getDisplayMode().getHeight();
			this.setLocation(screenWidth / 2 - 346, screenHeight / 2 - 256);
			contentPane.requestFocusInWindow();
			this.setVisible(true);
		}

		/**
		 * make GUIGenerate a singleton class.
		 * 
		 * @param frameTitle
		 * @return
		 */
		public static GUIGenerator GUIGeneratorInitializer() {
			if (generator == null) {
				generator = new GUIGenerator();
				generator
						.setTitle(quotes[(int) (quotes.length * Math.random())]);
				return generator;
			} else {
				return generator;
			}
		}

		@Override
		public void mouseClicked(MouseEvent e) {
			if (e.getButton() != MouseEvent.BUTTON1) {
				return;
			}
			if (e.getSource().equals(pandaButton)) {
				while (true) {
					try {
						String rawInput = JOptionPane.showInputDialog(
								pandaButton, "type what you want to hear...",
								"Song Explorer", JOptionPane.QUESTION_MESSAGE);
						if (rawInput == null || rawInput.equals(""))
							return;
						else {
							String searchRes = FLACFileExplorer
									.fileExplorer(rawInput);
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
									Thread.sleep(100);
									FLACFileExplorer.refreshDirectory(flacExt);
									FLACFileExplorer.refreshDirectory(lrcExt);
									Thread.sleep(10);
									continue;
								} else
									return;
							} else {
								int reply = JOptionPane
										.showConfirmDialog(
												pandaButton,
												"Matched song has been found: "
														+ FLACFileExplorer
																.getSongName(searchRes)
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
									while (wayPlaying || waySearching) {
									}
									handler = null;
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
										try {
											Thread.sleep(300);
										} catch (InterruptedException e1) {
											e1.printStackTrace();
										}
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

					} catch (InterruptedException e1) {
						e1.printStackTrace();
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
			} else if (e.getSource().equals(this.nextButton)) {
				if (Math.pow(e.getX() - nextButton.getHeight() / 2, 2)
						+ Math.pow(e.getY() - nextButton.getHeight() / 2, 2) > Math
							.pow(nextButton.getHeight() / 2, 2)) {
					return;
				} else {
					try {
						Thread.sleep(50);
					} catch (InterruptedException e1) {
						e1.printStackTrace();
					}
					playNextSong = true;
					new requestHandler("playNextSong");
				}
			} else if (e.getSource().equals(this.preButton)) {
				if (Math.pow(e.getX() - preButton.getHeight() / 2, 2)
						+ Math.pow(e.getY() - preButton.getHeight() / 2, 2) > Math
							.pow(preButton.getHeight() / 2, 2)) {
					return;
				} else {
					try {
						Thread.sleep(50);
					} catch (InterruptedException e1) {
						e1.printStackTrace();
					}
					playPreviousSong = true;
					new requestHandler("playPreviousSong");
				}
			} else if (e.getSource().equals(this.stopButton)) {
				if (Math.pow(e.getX() - stopButton.getHeight() / 2, 2)
						+ Math.pow(e.getY() - stopButton.getHeight() / 2, 2) > Math
							.pow(stopButton.getHeight() / 2, 2)) {
					return;
				} else {
					try {
						Thread.sleep(50);
					} catch (InterruptedException e1) {
						e1.printStackTrace();
					}
					stopPlaying = true;
					new requestHandler("stopPlaying");
				}
			} else if (e.getSource().equals(playButton)) {
				if (Math.pow(e.getX() - playButton.getHeight() / 2, 2)
						+ Math.pow(e.getY() - playButton.getHeight() / 2, 2) > Math
							.pow(playButton.getHeight() / 2, 2)) {
					return;
				} else {
					try {
						Thread.sleep(50);
					} catch (InterruptedException e1) {
						e1.printStackTrace();
					}
					if (handler == null) {
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
				}
			}
		}

		@Override
		public void mousePressed(MouseEvent e) {
			return;
			// TODO Auto-generated method stub
		}

		@Override
		public void mouseReleased(MouseEvent e) {
			if (e.getSource().equals(slider)) {
				if (slider.isEnabled()) {
					synchronized (lock) {
						if (isPlaying) {
							isPlaying = !isPlaying;
						}
						double temp = (double) sliderUi.valueForXPosition(e
								.getX()) / slider.getMaximum();
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
			// TODO Auto-generated method stub
		}

		public class requestHandler {
			public requestHandler(String option) {
				try {
					switch (option) {
					// handling play next song
					case "playNextSong":
						logger.log(Level.INFO, "Start playing next audio.");
						while (wayPlaying || waySearching) {
							Thread.sleep(1);
						}
						askForAChange = true;
						if (stopPlaying) {
							stopPlaying = !stopPlaying;
						}
						closeFile();
						Thread.sleep(200);
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
						if (playNextSong) {
							playNextSong = !playNextSong;
						}
						if (songsHavePlayed.indexOf(lastPlayed) == (songsHavePlayed
								.size() - 1))
							songToPlay = FLACFileExplorer
									.songMenu(songsHavePlayed);
						else
							songToPlay = songsHavePlayed.get(songsHavePlayed
									.indexOf(lastPlayed) + 1);
						if (songToPlay != null) {
							handler = new FLACAudioHandler(songToPlay);
							lastPlayed = songToPlay;
						} else {
							songToPlay = lastPlayed;
							handler = new FLACAudioHandler(songToPlay);
							if (!stopPlaying) {
								stopPlaying = !stopPlaying;
							}
							playButton.setIcon(startIcon);
							generator
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
						while (wayPlaying || waySearching) {
							if (playPreviousSong) {
								playPreviousSong = !playPreviousSong;
							}
						}
						askForAChange = true;
						closeFile();
						Thread.sleep(200);
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
						if (playPreviousSong) {
							playPreviousSong = !playPreviousSong;
						}
						// When the previous song is not the last played
						// song.
						if (songsHavePlayed.indexOf(lastPlayed) > 0) {
							songToPlay = songsHavePlayed.get(songsHavePlayed
									.indexOf(lastPlayed) - 1);
							lastPlayed = songToPlay;
							handler = new FLACAudioHandler(songToPlay);
						} else {
							songToPlay = lastPlayed;
							handler = new FLACAudioHandler(songToPlay);
							if (!stopPlaying) {
								stopPlaying = !stopPlaying;
							}
							playButton.setIcon(startIcon);
							generator
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
						while (wayPlaying || waySearching) {
							Thread.sleep(1);
						}
						if (handler == null) {
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
							handler = new FLACAudioHandler(songToPlay);
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
								Thread timeThread = new Thread(
										new LyricsParser());
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
									logger.log(Level.INFO,
											"Start playing audio.");
									lyricsLabelPre
											.setText(quotes[(int) (quotes.length * Math
													.random())]);
									lyricsLabelMid
											.setText(quotes[(int) (quotes.length * Math
													.random())]);
									lyricsLabelNext
											.setText(quotes[(int) (quotes.length * Math
													.random())]);
									timeLabel
											.setText(alignment + timeLabelInit);
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
						while (wayPlaying || waySearching) {
							Thread.sleep(1);
						}
						askForAChange = true;
						Thread.sleep(10);
						synchronized (lock) {
							// reset GUI outlook.
							closeFile();
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
						generator.setTitle(quotes[(int) (quotes.length * Math
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
				logger.log(Level.INFO, "Start decoding codec.");
				if (filesNotBeenInitialized) {
					return;
				}
				clipStartTime = 0;
				AudioFormat format = new AudioFormat(
						handler.getSampleRateInHz(),
						handler.getBitsPerSample(), handler.getNumOfChannels(),
						true, false);
				try {
					line = (SourceDataLine) AudioSystem
							.getLine(new DataLine.Info(SourceDataLine.class,
									format));
					line.open(format);
					line.start();
				} catch (IllegalArgumentException ex) {
					JOptionPane.showMessageDialog(
							null,
							"Cannot play \"" + songName
									+ "\" on your current OS.\nSample rate: "
									+ handler.getSampleRateInHz()
									+ ", sample depth: "
									+ handler.getBitsPerSample(),
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
				double totalSampleReciprocal = 1e0 / handler
						.getTotalSamplesInStream();
				if (handler.getSampleRateInHz() == 0) {
					return;
				} else
					sampleRateReciprocal = 1e0 / handler.getSampleRateInHz();
				if (handler.getSampleRateInHz() > 44100) {
					try {
						Thread.sleep(500);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
				int bytesPerSample = handler.getBitsPerSample() / 8;
				double oneInMillion = 1e0 / 1e6;
				while (!askForAChange) {
					try {
						while (stillHandlingLyrics) {
							Thread.sleep(1);
						}
						if (handler == null) {
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
							long samplePos = Math.round(handler
									.getTotalSamplesInStream() * request);
							while (waySearching) {
								wayPlaying = true;
							}
							samples = handler
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
							if (input == null)
								return;
							while (waySearching) {
								wayPlaying = true;
							}
							Object[] temp = readNextBlock(false);
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
						if (handler != null) {
							double t = currentTimeInMicros * oneInMillion
									* handler.getSampleRateInHz()
									* totalSampleReciprocal;
							setSliderPosition(t);
							if (currentTimeInMicros * oneInMillion >= handler
									.getAudioLength() - 1.1) {
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
						JOptionPane.showMessageDialog(generator,
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
				if (handler == null) {
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
		 * 1. To show instantaneous time frame in seconds. 2. To show
		 * instantaneous lyric corresponding to current time frame.
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
					generator.setTitle('\u264f' + " "
							+ songName.substring(songName.indexOf('-') + 1)
							+ " " + '\u264f');
				} else
					generator.setTitle('\u264f' + " " + songName + " "
							+ '\u264f');
				songName += "   ";
				slider.setEnabled(true);
				String lrcPathStr = FLACFileExplorer.getLRCFilePath(songToPlay
						.toString());
				if (lrcPathStr != null) {
					lyricsLabelPre.setForeground(new Color(0x003366));
					lyricsLabelMid.setForeground(new Color(0x003366));
					lyricsLabelNext.setForeground(new Color(0x003366));
					lyricDict = FLACFileExplorer.lrcParser(Paths
							.get(lrcPathStr));
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
						if (handler == null) {
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
													+ nextText.substring(
															nextIdx,
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
									if (pennySec < framePoints.get(0)
											.floatValue()) {
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
										if (!songName.equals('\u266A'
												+ " "
												+ lyricDict.get(framePoints
														.get(0)) + " "
												+ '\u266A')) {
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
																+ " "
																+ '\u266A')
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
														lyricsLabelPre
																.setText(" ");
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
														+ lyricDict
																.get(framePoints
																		.get(framePoints
																				.size() - 1))
														+ " " + '\u266A')
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
									if (!LastTimeFrame.equals("0" + min + ":0"
											+ s)) {
										LastTimeFrame = "0" + min + ":0" + s;
										timeLabel.setText(alignment + " "
												+ '\u264f' + " "
												+ LastTimeFrame + " "
												+ '\u264f' + " ");
									}
								} else {
									if (!LastTimeFrame.equals(min + ":0" + s)) {
										LastTimeFrame = min + ":0" + s;
										timeLabel.setText(alignment + " "
												+ '\u264f' + " "
												+ LastTimeFrame + " "
												+ '\u264f' + " ");
									}
								}
							} else {
								if (min < 10) {
									if (!LastTimeFrame.equals("0" + min + ":"
											+ s)) {
										LastTimeFrame = "0" + min + ":" + s;
										timeLabel.setText(alignment + " "
												+ '\u264f' + " "
												+ LastTimeFrame + " "
												+ '\u264f' + " ");
									}
								} else {
									if (!LastTimeFrame.equals(min + ":" + s)) {
										LastTimeFrame = min + ":" + s;
										timeLabel.setText(alignment + " "
												+ '\u264f' + " "
												+ LastTimeFrame + " "
												+ '\u264f' + " ");
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

	public static void main(String[] args) {
		FLACAudioHandler.audioPlayerInitializer();
	}

	/**
	 * search for the closet frame in respect to given value, the value should
	 * range from 0 and 1.
	 * 
	 * @param samplePos
	 * @throws IOException
	 * @throws BadFileFormatException
	 */
	public long[][] findNextDecodeableBlock(long samplePos) throws IOException,
			BadFileFormatException {
		while (wayPlaying) {
			waySearching = true;
		}
		long startFilePos = metadataEndPos;
		// Is that the true difference? Like serialization or something?
		long endFilePos = input.getLength();
		long curSamplePos = 0;
		while (endFilePos - startFilePos > 100000) {
			long middle = (startFilePos + endFilePos) / 2;
			long[] offsets = findNextDecodableFrame(middle);
			if (offsets == null || offsets[1] > samplePos)
				endFilePos = middle;
			else {
				startFilePos = offsets[0];
				curSamplePos = offsets[1];
			}
		}
		input.seekTo(startFilePos);
		while (true) {
			Object[] temp = readNextBlock(false);
			if (temp == null)
				return null;
			long[][] samples = (long[][]) temp[0];
			int blockSize = samples[0].length;
			long nextSamplePos = curSamplePos + blockSize;
			if (nextSamplePos > samplePos) {
				long[][] result = new long[samples.length][];
				for (int ch = 0; ch < getNumOfChannels(); ch++)
					result[ch] = Arrays.copyOfRange(samples[ch],
							(int) (samplePos - curSamplePos), blockSize);
				return result;
			}
			curSamplePos = nextSamplePos;
		}
	}

	public long[] findNextDecodableFrame(long bytePos) throws IOException {
		int val = 0;
		while (true) {
			try {
				input.seekTo(bytePos);
				boolean foundHalf = false;
				inner_loop: while (true) {
					val = input.readByte();
					if (val == -1)
						return null;
					if (!foundHalf && val == 0xFF)
						foundHalf = true;
					else if (foundHalf && (val >> 1) == 0x7C)
						break inner_loop;
					else
						foundHalf = false;
				}
				bytePos = input.getPosition() - 2;
				input.seekTo(bytePos);
				Object[] temp = readNextBlock(false);
				if (temp == null) {
					return null;
				} else {
					long curSamplePos = (long) temp[1];
					return new long[] { bytePos, curSamplePos };
				}
			} catch (BadFileFormatException e) {
				bytePos += 2;
			}
		}
	}

	public void decodeAndPlay() throws BadFileFormatException, IOException,
			LineUnavailableException, InterruptedException {
		long[][] samples = null;
		if (request >= 0) {
			long samplePos = Math.round(handler.getTotalSamplesInStream()
					* request);
			samples = handler.findNextDecodeableBlock(samplePos);
		} else {
			if (handler == null)
				return;
			Object[] temp = readNextBlock(false);
			if (temp != null) {
				samples = (long[][]) temp[0];
			}
		}
		if (samples == null)
			return;
		// convert samples to channel-interleaved bytes in little-endian
		int bytesPerSample = handler.getBitsPerSample() / 8;
		sampleInBytes = new byte[samples.length * samples[0].length
				* bytesPerSample];
		for (int i = 0, k = 0; i < samples[0].length; i++) {
			for (int ch = 0; ch < samples.length; ch++) {
				long cache = samples[ch][i];
				for (int j = 0; j < bytesPerSample; j++, k++)
					sampleInBytes[k] = (byte) (cache >>> (j << 3));
			}
		}
	}

	/*
	 * DO NOT leave out any necessary procedures! Or you won't get what you
	 * want. ;)
	 */
	/**
	 * Decode a single frame with its subframes.
	 * 
	 * @return an array of type Object with which to return both the raw
	 *         position of starting position of audio data in the frame and
	 *         samples of audio data.
	 * @throws BadFileFormatException
	 * @throws IOException
	 * @throws Exception
	 */
	public static Object[] readNextBlock(boolean verifyCRC8)
			throws BadFileFormatException, IOException {
		/* Sync code */
		if (verifyCRC8) {
			rawHeader.clear();
			rawHeader.add(input.readByte());
			if (rawHeader.get(0) == -1)
				return null;
			rawHeader.add(input.readByte());
			int sync = rawHeader.get(0) << 6 | (rawHeader.get(1) >> 2);
			if (sync != 0x3FFE)
				return null;
			if ((rawHeader.get(1) & 2) != 0)
				throw handler.new BadFileFormatException("Reserved bit");
			int blockStrategy = rawHeader.get(1) & 1;
			rawHeader.add(input.readByte());
			int blockSizeCode = rawHeader.get(2) >> 4;
			int sampleRate = rawHeader.get(2) & 0xF;
			rawHeader.add(input.readByte());
			int channleAssign = rawHeader.get(3) >> 4;
			/* Sample size in bits */
			switch ((rawHeader.get(3) >> 1) & 7) {
			case 1:
				if (handler.bitsPerSample != 8)
					throw handler.new BadFileFormatException(
							"Sample depth mismatch");
				break;
			case 2:
				if (handler.bitsPerSample != 12)
					throw handler.new BadFileFormatException(
							"Sample depth mismatch");
				break;
			case 4:
				if (handler.bitsPerSample != 16)
					throw handler.new BadFileFormatException(
							"Sample depth mismatch");
				break;
			case 5:
				if (handler.bitsPerSample != 20)
					throw handler.new BadFileFormatException(
							"Sample depth mismatch");
				break;
			case 6:
				if (handler.bitsPerSample != 24)
					throw handler.new BadFileFormatException(
							"Sample depth mismatch");
				break;
			default:
				throw handler.new BadFileFormatException(
						"Sample depth mismatch");
			}
			/* Reserved bit */
			if ((rawHeader.get(3) & 1) != 0)
				throw handler.new BadFileFormatException(
						"reserved bit value is not right.");

			rawHeader.add(input.readByte());
			long rawPosition;
			if (rawHeader.get(4) < 0x80)
				rawPosition = rawHeader.get(4);
			else {
				int rawPosNumBytes = Integer.numberOfLeadingZeros(~(rawHeader
						.get(4) << 24)) - 1;
				rawPosition = rawHeader.get(4) & (0x3F >>> rawPosNumBytes);
				for (int i = 0; i < rawPosNumBytes; i++) {
					rawHeader.add(input.readByte());
					rawPosition = (rawPosition << 6)
							| (rawHeader.get(4 + i) & 0x3F);
				}
			}

			/* Block size in inter-channel samples */
			int blockSize;
			if (blockSizeCode == 1)
				blockSize = 192;
			else if (blockSizeCode >= 2 && blockSizeCode <= 5)
				blockSize = 576 << (blockSizeCode - 2);
			else if (blockSizeCode == 6) {
				rawHeader.add(input.readByte());
				blockSize = rawHeader.get(rawHeader.size() - 1) + 1;
			} else if (blockSizeCode == 7) {
				rawHeader.add(input.readByte());
				rawHeader.add(input.readByte());
				blockSize = ((rawHeader.get(rawHeader.size() - 2) << 8) & rawHeader
						.get(rawHeader.size() - 1)) + 1;
			} else if (blockSizeCode >= 8 && blockSizeCode <= 15)
				blockSize = 256 << (blockSizeCode - 8);
			else
				throw handler.new BadFileFormatException(
						"Invalid blocksize code!");

			/* Sample rate */
			if (sampleRate == 12)
				rawHeader.add(input.readByte());
			else if (sampleRate == 13 || sampleRate == 14) {
				rawHeader.add(input.readByte());
				rawHeader.add(input.readByte());
			}
			rawHeader.trimToSize();
			/* read CRC-8 */
			int crc8 = input.readByte();
			int crc8_val = crc8_Generator(rawHeader);
			if (crc8 != crc8_val)
				throw handler.new BadFileFormatException(
						"Unexpected CRC-8 value.");
			long[][] samples = decodeSubframes(blockSize,
					handler.bitsPerSample, channleAssign);
			/* Align to byte */
			input.alignToByte();
			/* read Frame footer */
			input.readBit(16);
			return new Object[] { samples,
					rawPosition * (blockStrategy == 0 ? blockSize : 1) };
		} else {
			int byteVal = input.readByte();
			if (byteVal == -1)
				return null;
			int sync = byteVal << 6 | input.readBit(6);
			if (sync != 0x3FFE)
				return null;
			if (input.readBit(1) != 0)
				throw handler.new BadFileFormatException("Reserved bit");
			int blockStrategy = input.readBit(1);

			// Read numerous header fields, and ignore some of them
			int blockSizeCode = input.readBit(4);
			int sampleRateCode = input.readBit(4);
			int chanAsgn = input.readBit(4);
			switch (input.readBit(3)) {
			case 1:
				if (handler.bitsPerSample != 8)
					throw handler.new BadFileFormatException(
							"Sample depth mismatch");
				break;
			case 2:
				if (handler.bitsPerSample != 12)
					throw handler.new BadFileFormatException(
							"Sample depth mismatch");
				break;
			case 4:
				if (handler.bitsPerSample != 16)
					throw handler.new BadFileFormatException(
							"Sample depth mismatch");
				break;
			case 5:
				if (handler.bitsPerSample != 20)
					throw handler.new BadFileFormatException(
							"Sample depth mismatch");
				break;
			case 6:
				if (handler.bitsPerSample != 24)
					throw handler.new BadFileFormatException(
							"Sample depth mismatch");
				break;
			default:
				throw handler.new BadFileFormatException(
						"Reserved/invalid sample depth");
			}
			if (input.readBit(1) != 0)
				throw handler.new BadFileFormatException("Reserved bit");

			byteVal = input.readBit(8);
			long rawPosition;
			if (byteVal < 0x80)
				rawPosition = byteVal;
			else {
				int rawPosNumBytes = Integer
						.numberOfLeadingZeros(~(byteVal << 24)) - 1;
				rawPosition = byteVal & (0x3F >>> rawPosNumBytes);
				for (int i = 0; i < rawPosNumBytes; i++)
					rawPosition = (rawPosition << 6)
							| (input.readBit(8) & 0x3F);
			}

			int blockSize;
			if (blockSizeCode == 1)
				blockSize = 192;
			else if (2 <= blockSizeCode && blockSizeCode <= 5)
				blockSize = 576 << (blockSizeCode - 2);
			else if (blockSizeCode == 6)
				blockSize = input.readBit(8) + 1;
			else if (blockSizeCode == 7)
				blockSize = input.readBit(16) + 1;
			else if (8 <= blockSizeCode && blockSizeCode <= 15)
				blockSize = 256 << (blockSizeCode - 8);
			else
				throw handler.new BadFileFormatException("Reserved block size");

			if (sampleRateCode == 12)
				input.readBit(8);
			else if (sampleRateCode == 13 || sampleRateCode == 14)
				input.readBit(16);

			input.readBit(8);
			// Decode each channel's subframe, then skip footer
			long[][] samples = decodeSubframes(blockSize,
					handler.bitsPerSample, chanAsgn);
			input.alignToByte();
			input.readBit(16);
			if (handler == null) {
				return null;
			}
			return new Object[] {
					samples,
					rawPosition
							* (blockStrategy == 0 ? handler.constantBlockSize
									: 1) };
		}

	}

	/**
	 * Given block size in samples, bits per sample and channel assignment of
	 * current frame, decode each sub-frame of current frame.
	 * 
	 * @param blockSize
	 *            : Block size in inter-channel samples
	 * @param sampleDepth
	 *            : Sample size in bits
	 * @param chanAsgn
	 *            : channel assignment
	 * @return a two-dimension array containing info of both channels and
	 *         samples in each channel.
	 * @throws IOException
	 * @throws Exception
	 */
	private static long[][] decodeSubframes(int blockSize, int sampleDepth,
			int chanAsgn) throws BadFileFormatException, IOException {
		long[][] result;
		if (0 <= chanAsgn && chanAsgn <= 7) {
			result = new long[chanAsgn + 1][blockSize];
			for (int ch = 0; ch < result.length; ch++)
				decodeSubframe(sampleDepth, result[ch]);
		} else if (8 <= chanAsgn && chanAsgn <= 10) {
			result = new long[2][blockSize];
			decodeSubframe(sampleDepth + (chanAsgn == 9 ? 1 : 0), result[0]);
			decodeSubframe(sampleDepth + (chanAsgn == 9 ? 0 : 1), result[1]);
			if (chanAsgn == 8) {
				for (int i = 0; i < blockSize; i++)
					result[1][i] = result[0][i] - result[1][i];
			} else if (chanAsgn == 9) {
				for (int i = 0; i < blockSize; i++)
					result[0][i] += result[1][i];
			} else if (chanAsgn == 10) {
				for (int i = 0; i < blockSize; i++) {
					long side = result[1][i];
					long right = result[0][i] - (side >> 1);
					result[1][i] = right;
					result[0][i] = right + side;
				}
			}
		} else
			throw handler.new BadFileFormatException(
					"Reserved channel assignment");
		return result;
	}

	/**
	 * Decode each sub-frame with respect to channel assignment.
	 * 
	 * @param sampleDepth
	 *            : bits per sample in current frame
	 * @param result
	 *            : samples in current channel.
	 * @throws IOException
	 * @throws Exception
	 */
	private static void decodeSubframe(int sampleDepth, long[] result)
			throws BadFileFormatException, IOException {
		if (input.readBit(1) != 0)
			throw handler.new BadFileFormatException("Invalid padding bit");
		int type = input.readBit(6);
		int shift = input.readBit(1);
		if (shift == 1) {
			while (input.readBit(1) == 0)
				shift++;
		}
		sampleDepth -= shift;
		if (type == 0) // Constant coding
			// for constant coding audio data size is sampleDepth *
			// result.length.
			Arrays.fill(result, 0, result.length,
					input.readSignedBit(sampleDepth));
		else if (type == 1) // Verbatim coding
			// for verbatim coding audio data size is sampleDepth *
			// result.length.
			for (int i = 0; i < result.length; i++)
				result[i] = input.readSignedBit(sampleDepth);
		else if (8 <= type && type <= 12 || 32 <= type && type <= 63) {
			int predOrder;
			int[] lpcCoefs;
			int lpcShift;
			if (type <= 12) { // Fixed predictive coding
				// predOrder = type - 8;
				predOrder = type & 7;
				int length = predOrder <= result.length ? predOrder
						: result.length;
				// for fixed predictive coding audio data size is sampleDepth *
				// result.length.
				for (int i = 0; i < length; i++)
					result[i] = input.readSignedBit(sampleDepth);
				lpcCoefs = FIXED_PREDICTION_COEFFICIENTS[predOrder];
				lpcShift = 0;
			} else { // Linear predictive coding
				// predOrder = type - 31;
				predOrder = (type & 0x1F) + 1;
				int length = predOrder <= result.length ? predOrder
						: result.length;
				// for linear predictive coding audio data size is sampleDepth *
				// result.length, except for residual size.
				for (int i = 0; i < length; i++)
					result[i] = input.readSignedBit(sampleDepth);
				int precision = input.readBit(4) + 1;
				lpcShift = input.readSignedBit(5);
				lpcCoefs = new int[predOrder];
				for (int i = 0; i < predOrder; i++)
					lpcCoefs[i] = input.readSignedBit(precision);
			}
			decodeRiceResiduals(predOrder, result);
			for (int i = predOrder; i < result.length; i++) { // LPC
				// restoration
				long sum = 0;
				for (int j = 0; j < lpcCoefs.length; j++)
					sum += result[i - 1 - j] * lpcCoefs[j];
				result[i] += sum >> lpcShift;
			}
		} else
			throw handler.new BadFileFormatException("Invalid predicton order.");
		for (int i = 0; i < result.length; i++)
			result[i] <<= shift;
	}

	/**
	 * Entropy coding method handler, with which to restore residuals into
	 * samples.
	 * 
	 * @param warmup
	 *            : Unencoded warm-up samples
	 * @param result
	 *            : restored data
	 * @throws IOException
	 * @throws Exception
	 */
	private static void decodeRiceResiduals(int warmup, long[] result)
			throws BadFileFormatException, IOException {
		int method = input.readBit(2);
		if (method >= 2)
			throw handler.new BadFileFormatException(
					"Reserved residual coding method");
		int paramBits = method == 0 ? 4 : 5;
		int escapeParam = method == 0 ? 0xF : 0x1F;
		int partitionOrder = input.readBit(4);
		int numPartitions = 1 << partitionOrder;
		if (result.length % numPartitions != 0)
			throw handler.new BadFileFormatException(
					"Block size not divisible by number of Rice partitions");
		int partitionSize = result.length / numPartitions;

		for (int i = 0; i < numPartitions; i++) {
			int start = i * partitionSize + (i == 0 ? warmup : 0);
			int end = (i + 1) * partitionSize;
			int param = input.readBit(paramBits);
			if (param < escapeParam) {
				for (int j = start; j < end; j++) { // Read Rice signed
					// integers
					long val = 0;
					while (input.readBit(1) == 0)
						val++;
					val = (val << param) | input.readBit(param);
					result[j] = (val >>> 1) ^ -(val & 1);
				}
			} else {
				int numBits = input.readBit(5);
				for (int j = start; j < end; j++)
					result[j] = input.readSignedBit(numBits);
			}
		}
	}

	public static final class Stream implements Runnable {
		private RandomAccessFile raf;
		private long bytePosition;
		private InputStream byteBuffer;
		private long bitBuffer;
		private int bitBufferLen;

		public Stream(Path file) throws IOException {
			raf = new RandomAccessFile(file.toFile(), "r");
			seekTo(0);
		}

		public void close() throws IOException {
			raf.close();
		}

		public long getLength() throws IOException {
			return raf.length();
		}

		public long getPosition() {
			return bytePosition;
		}

		public void seekTo(long pos) throws IOException {
			raf.seek(pos);
			bytePosition = pos;
			byteBuffer = new BufferedInputStream(new InputStream() {
				public int read() throws IOException {
					return raf.read();
				}

				public int read(byte[] b, int off, int len) throws IOException {
					return raf.read(b, off, len);
				}
			});
			bitBufferLen = 0;
		}

		public int readByte() throws IOException {
			if (bitBufferLen > 8) {
				return readBit(8);
			} else {
				int result = byteBuffer.read();
				if (result != -1) {
					bytePosition++;
				}
				return result;
			}
		}

		public int readBit(int n) throws IOException {
			while (n > bitBufferLen) {
				int temp = byteBuffer.read();
				bytePosition++;
				if (temp == -1)
					throw new EOFException();
				bitBuffer = (bitBuffer << 8) | temp;
				bitBufferLen += 8;
			}
			bitBufferLen -= n;
			int result = (int) (bitBuffer >>> bitBufferLen);
			if (n < 32)
				result &= (1 << n) - 1;
			return result;
		}

		public int readSignedBit(int n) throws IOException {
			return (readBit(n) << (32 - n)) >> (32 - n);
		}

		public void alignToByte() {
			bitBufferLen -= bitBufferLen % 8;
		}

		@Override
		public void run() {
			// TODO Auto-generated method stub

		}
	}

	public class BadFileFormatException extends Exception {

		/**
		 * This exception would be wanted to throw when unexpected data/formats
		 * of the file are detected.
		 */
		private static final long serialVersionUID = -6038660784354160702L;

		public BadFileFormatException(String message) {
			super(message);
		}
	}

}
