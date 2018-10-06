package com.filesystem.handlers;

import java.awt.Font;
import java.awt.FontMetrics;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.JFileChooser;
import javax.swing.JLabel;

import com.audioplayer.UIlayout.GUISynthesiszer;

public final class FileExplorer {
	private static final FileExplorer EXPLORER = new FileExplorer();
	private static final Object lock = new Object();
	private static final String flacExt = ".flac";
	private static final String mp3Ext = ".mp3";
	private static final String lrcExt = ".lrc";
	private ArrayList<Path> songList;
	private ArrayList<Path> FLACsongList;
	private ArrayList<Path> mp3songList;
	private ArrayList<Path> folderList;
	private ArrayList<Path> lyricsList;
	private Path specifiedPath;
	private Path songToPlay;
	private int songIdx = 0;
	private JFileChooser fileChooser;
	private boolean alreadyStartOneApp;

	static {
		EXPLORER.initialize();
	}

	private FileExplorer() {
	}

	// Make constructor private
	private void initialize() {
		// Make sure that there is no more than one single Java process that do
		// "plays the FLAC audio file".
		EXPLORER.alreadyStartOneApp = false;
		Process process = null;
		EXPLORER.songToPlay = null;
		try {
			process = Runtime.getRuntime().exec("jps");
			BufferedReader br = new BufferedReader(new InputStreamReader(
					process.getInputStream()));
			String p = null;
			int count = 0;
			String mainClassName = GUISynthesiszer.class.getSimpleName();
			String fileSource = GUISynthesiszer.class.getResource(
					mainClassName + ".class").toString();
			if (fileSource.startsWith("jar:file:/")) {
				fileSource = fileSource.substring(10);
				fileSource = fileSource.substring(0,
						fileSource.lastIndexOf("!"));
			} else if (fileSource.startsWith("file:/")) {
				fileSource = fileSource.substring(6);
			}
			Path sourceFile = Paths.get(fileSource);
			String fileSourceName = sourceFile.toAbsolutePath().toString();
			fileSourceName = fileSourceName.substring(fileSourceName
					.lastIndexOf(File.separator) + 1);
			System.out.println(mainClassName);
			System.out.println(fileSourceName);
			while ((p = br.readLine()) != null) {
				System.out.println(p);
				if (p.endsWith(" " + mainClassName)
						|| p.endsWith(" " + fileSourceName)) {
					count++;
				}
			}
			if (count > 1) {
				EXPLORER.alreadyStartOneApp = !EXPLORER.alreadyStartOneApp;
			}
			br.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		EXPLORER.songList = new ArrayList<>();
		EXPLORER.FLACsongList = new ArrayList<>();
		EXPLORER.mp3songList = new ArrayList<>();
		EXPLORER.folderList = new ArrayList<>();
		EXPLORER.lyricsList = new ArrayList<>();
		specifiedPath = null;
		if (!alreadyStartOneApp) {
			fileChooser = new JFileChooser();
			fileChooser.setDialogTitle("Please choose a FLAC directory");
			synchronized (lock) {
				lock.notify();
				fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
			}
			int response = fileChooser.showOpenDialog(null);
			if (response == JFileChooser.APPROVE_OPTION) {
				// Initialize file paths and directory paths based on current
				// specified path.
				specifiedPath = Paths.get(fileChooser.getSelectedFile()
						.getAbsolutePath());
				FileHandler.recursiveDirectoryHandler(specifiedPath,
						EXPLORER.FLACsongList, EXPLORER.folderList, flacExt);
				FileHandler.recursiveDirectoryHandler(specifiedPath,
						EXPLORER.mp3songList, EXPLORER.folderList, mp3Ext);
				FileHandler.recursiveDirectoryHandler(specifiedPath,
						EXPLORER.lyricsList, EXPLORER.folderList, lrcExt);
				for (Path path : EXPLORER.FLACsongList) {
					songList.add(path);
				}
				for (Path path : EXPLORER.mp3songList) {
					songList.add(path);
				}
			}
		}
		// parsing files and directories under specified path
		// refreshDirectory(lrcExt);
	}

	// LRC file parser
	/**
	 * Parse the LRC file from the given file path, return the time frames and
	 * corresponding lyrics in the form of an instance of HashMap<Float,
	 * String>.
	 * 
	 * @param lycFilePath
	 * @return a resorted instance of HashMap<Float, String> representing the
	 *         time frames and the corresponding lyrics in time sequenced order.
	 */
	public static HashMap<Float, String> lrcParser(Path lycFilePath) {
		if (EXPLORER.specifiedPath == null) {
			return null;
		}
		Font monospace = new Font("Monospace", Font.TRUETYPE_FONT, 22);
		JLabel label = new JLabel();
		FontMetrics fontMetrics = label.getFontMetrics(monospace);
		HashMap<Float, String> lyrics = new HashMap<Float, String>();
		try {
			Logger.getGlobal().log(Level.INFO, "Parsing lyric contents.");
			BufferedReader reader = new BufferedReader(new InputStreamReader(
					Files.newInputStream(lycFilePath), "UTF-8"));
			String content = "";
			while ((content = reader.readLine()) != null) {
				if (content.contains("//")) {
					break;
				}
				if (content.length() < 11 || content.charAt(6) != '.'
						|| content.contains("Öø×÷È¨")) {
					continue;
				}
				if (content.charAt(1) < '0' || content.charAt(1) > '9') {
					continue;
				}
				if (content.charAt(9) >= '0' && content.charAt(9) <= '9') {
					content = content.substring(0, 9) + content.substring(10);
				}
				String minString = content.substring(
						(content.indexOf('[') + 1), content.indexOf(':'));
				String secondString = content.substring(
						(content.indexOf(':') + 1), content.indexOf('.'));
				String pennyString = content.substring(
						(content.indexOf('.') + 1), content.indexOf(']'));
				if (minString.charAt(0) == '0') {
					minString = minString.substring(1);
				}
				int min = Integer.valueOf(minString);
				int s = Integer.valueOf(secondString);
				int penny = Integer.valueOf(pennyString);
				float pennySec = (float) (min * 60 + s + penny * 0.01f);
				String lyricString = content
						.substring(content.indexOf(']') + 1);
				if (lyricString.contains(" ")) {
					int len = lyricString.length();
					while (len > 0) {
						if (lyricString.charAt(len - 1) != ' ') {
							break;
						}
						len--;
					}
					if (len == 0) {
						lyricString = "*music*";
					}
				}
				while (lyricString.contains("&apos;")) {
					if (lyricString.charAt(lyricString.indexOf('&') + 5) == ';') {
						if (lyricString.charAt(lyricString.indexOf('&') + 2) == 'p') {
							if (lyricString.indexOf(';') == lyricString
									.length() - 1) {
								lyricString = lyricString.substring(0,
										lyricString.indexOf('&')) + "'";
							} else
								lyricString = lyricString.substring(0,
										lyricString.indexOf('&'))
										+ "'"
										+ lyricString.substring(lyricString
												.indexOf(';') + 1);
						}
					}
				}
				while (lyricString.contains("&quot;")) {
					if (lyricString.charAt(lyricString.indexOf('&') + 5) == ';') {
						if (lyricString.charAt(lyricString.indexOf('&') + 2) == 'u') {
							if (lyricString.indexOf(';') == lyricString
									.length() - 1) {
								lyricString = lyricString.substring(0,
										lyricString.indexOf('&')) + "\"";
							} else
								lyricString = lyricString.substring(0,
										lyricString.indexOf('&'))
										+ "\""
										+ lyricString.substring(lyricString
												.indexOf(';') + 1);
						}
					}
				}
				lyrics.put(pennySec, lyricString);
			}
			reader.close();
			// Resort time frame in ascending order.
			Float[] keysArray = new Float[lyrics.keySet().size()];
			lyrics.keySet().toArray(keysArray);
			Arrays.sort(keysArray);
			ArrayList<Float> fKeys = new ArrayList<Float>();
			ArrayList<String> strValues = new ArrayList<String>();
			for (Float f : keysArray) {
				fKeys.add(f);
				strValues.add(lyrics.get(f));
			}
			fKeys.trimToSize();
			for (Float f : fKeys) {
				if (f != fKeys.get(fKeys.size() - 1)) {
					// handling display issues.
					if (fontMetrics
							.stringWidth(strValues.get(fKeys.indexOf(f))) > 475) {
						Float differ = fKeys.get(fKeys.indexOf(f) + 1) - f;
						Float fnext = differ * 0.5f + f;
						String str = strValues.get(fKeys.indexOf(f));
						int idx = (int) ((str.length() - 1) * 0.5);
						while (idx > 0) {
							if (str.charAt(idx) != ' ') {
								idx--;
							} else {
								break;
							}
						}
						lyrics.put(f, str.substring(0, idx));
						if (fontMetrics.stringWidth(str.substring(0, idx)) > 470) {
							int nextHalf = idx + (str.length() - 1 - idx) / 2;
							while (nextHalf != str.length() - 1) {
								if (str.charAt(nextHalf) != ' ') {
									nextHalf++;
								} else {
									break;
								}
							}
							lyrics.put(fnext, str.substring(idx + 1, nextHalf));
							if (nextHalf != str.length() - 1) {
								fnext = differ * 0.7f + f;
								lyrics.put(fnext, str.substring(nextHalf + 1));
							}
						} else {
							lyrics.put(fnext, str.substring(idx + 1));
						}
					}
				}
			}
		} catch (NumberFormatException | IOException e) {
			e.printStackTrace();
		}
		Logger.getGlobal().log(Level.INFO, "Lyrics parsing has finished.");
		return lyrics;
	}

	/**
	 * Search the files based on the given file extension from the local file
	 * directory and refresh the file paths in the ArrayList of Class Path.
	 * 
	 * @param fileExtension
	 */
	public static void refreshDirectory(String fileExtension) {
		if (EXPLORER.specifiedPath == null) {
			return;
		}
		switch (fileExtension) {
		case flacExt:
			for (Path path : EXPLORER.FLACsongList) {
				if (EXPLORER.songList.contains(path)) {
					EXPLORER.songList.remove(path);
				}
			}
			if (!EXPLORER.FLACsongList.isEmpty())
				EXPLORER.FLACsongList.clear();
			if (!EXPLORER.folderList.isEmpty())
				EXPLORER.folderList.clear();
			FileHandler.recursiveDirectoryHandler(EXPLORER.specifiedPath,
					EXPLORER.FLACsongList, EXPLORER.folderList, fileExtension);
			for (Path path : EXPLORER.FLACsongList) {
				if (!EXPLORER.songList.contains(path)) {
					EXPLORER.songList.add(path);
				}
			}
			break;
		case mp3Ext:
			for (Path path : EXPLORER.mp3songList) {
				if (EXPLORER.songList.contains(path)) {
					EXPLORER.songList.remove(path);
				}
			}
			if (!EXPLORER.mp3songList.isEmpty())
				EXPLORER.mp3songList.clear();
			if (!EXPLORER.folderList.isEmpty())
				EXPLORER.folderList.clear();
			FileHandler.recursiveDirectoryHandler(EXPLORER.specifiedPath,
					EXPLORER.mp3songList, EXPLORER.folderList, fileExtension);
			for (Path path : EXPLORER.mp3songList) {
				if (!EXPLORER.songList.contains(path)) {
					EXPLORER.songList.add(path);
				}
			}
			break;
		case lrcExt:
			if (!EXPLORER.lyricsList.isEmpty())
				EXPLORER.lyricsList.clear();
			if (!EXPLORER.folderList.isEmpty())
				EXPLORER.folderList.clear();
			FileHandler.recursiveDirectoryHandler(EXPLORER.specifiedPath,
					EXPLORER.lyricsList, EXPLORER.folderList, lrcExt);
			break;
		default:
			break;
		}
	}

	/**
	 * Get the matching LRC file path to the given song path if the LRC file
	 * exists.
	 * 
	 * @param songPath
	 * @return the matching LRC file path if it exists, otherwise null.
	 */
	public static Path getLRCFilePath(Path songPath) {
		if (EXPLORER.specifiedPath == null) {
			return null;
		}
		if (EXPLORER.lyricsList.isEmpty()) {
			refreshDirectory(lrcExt);
		}
		String str = null;
		if (songPath.toString().endsWith(flacExt)) {
			str = songPath.toString().substring(0,
					songPath.toString().length() - 5)
					+ lrcExt;
		} else if (songPath.toString().endsWith(mp3Ext)) {
			str = songPath.toString().substring(0,
					songPath.toString().length() - 4)
					+ lrcExt;
		}
		Path path = Paths.get(str);
		if (Files.exists(path)) {
			return path;
		}
		return null;
	}

	/**
	 * Convert given path to the song name.
	 * 
	 * @param songPath
	 * @return the String representation of the song name.
	 */
	public static String getSongName(Path songPath) {
		String name = songPath.getFileName().toString();
		if (name.endsWith(flacExt)) {
			return name.substring(0, name.length() - 5);
		} else if (name.endsWith(mp3Ext)) {
			return name.substring(0, name.length() - 4);
		}
		return "Audio Player";
	}

	/**
	 * Search the possibly matching song based on given String.
	 * 
	 * @param response
	 * @return any one of possible songs that contains the given String.
	 */
	public static Path fileSearcher(String response) {
		if (EXPLORER.specifiedPath == null) {
			return null;
		}
		Path res = null;
		for (Iterator<Path> iterator = EXPLORER.songList.iterator(); iterator
				.hasNext();) {
			Path song = iterator.next();
			if (song.getFileName().toString().toLowerCase()
					.contains(response.toLowerCase())) {
				res = song;
				return res;
			}
		}
		return res;
	}

	/**
	 * Randomly pick a song which is not listed on the given ArrayList of Class
	 * Path.
	 * 
	 * @param songsHavePicked
	 * @return the picked song path.
	 */
	public static Path songMenu(ArrayList<Path> songsHavePicked) {
		if (EXPLORER.specifiedPath == null) {
			return null;
		}
		if (EXPLORER.songIdx < EXPLORER.songList.size()) {
			do {
				EXPLORER.songToPlay = EXPLORER.songList.get((int) (Math
						.random() * EXPLORER.songList.size()));
			} while (songsHavePicked.contains(EXPLORER.songToPlay));
			if (!songsHavePicked.contains(EXPLORER.songToPlay))
				EXPLORER.songIdx++;
			return EXPLORER.songToPlay;
		}
		return null;
	}
}
