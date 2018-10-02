package com.java.audioplayer;

import java.awt.Font;
import java.awt.FontMetrics;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;

import javax.swing.JFileChooser;
import javax.swing.JLabel;

public class FLACFileExplorer {
	private static Path specifiedPath;
	private static ArrayList<Path> songList;
	private static ArrayList<Path> folderList;
	private static ArrayList<Path> lyricsList;
	private static Path songToPlay;
	private static final String flacExt = ".flac";
	private static final String lrcExt = ".lrc";
	private static JFileChooser fileChooser;
	private static Object lock;
	private static int songIdx = 0;

	static {
		songList = new ArrayList<>();
		folderList = new ArrayList<>();
		lyricsList = new ArrayList<>();
		lock = new Object();
		songIdx = 0;
		specifiedPath = null;
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
			FileHandler.recursiveDirectoryHandler(specifiedPath, songList,
					folderList, flacExt);
		}
		// parsing files and directories under specified path
		// refreshDirectory(lrcExt);
	}

	// LRC file parser
	/**
	 * According to given time frame, output corresponding lyrics.
	 * 
	 * @param lycFile
	 * @param timeFrame
	 * @return
	 */
	public static HashMap<Float, String> lrcParser(Path lycFilePath) {
		if (specifiedPath == null) {
			return null;
		}
		Font monospace = new Font("Monospace", Font.TRUETYPE_FONT, 22);
		JLabel label = new JLabel();
		FontMetrics fontMetrics = label.getFontMetrics(monospace);
		HashMap<Float, String> lyrics = new HashMap<Float, String>();
		try {
			// Perhaps Windows could be a better place to try.
			BufferedReader reader = new BufferedReader(new InputStreamReader(
					new FileInputStream(lycFilePath.toFile()), "UTF-8"));
			String content = "";
			while ((content = reader.readLine()) != null) {
				if (content.contains("//")) {
					break;
				}
				if (content.length() < 11 || content.charAt(6) != '.'
						|| content.contains("����Ȩ")) {
					continue;
				}
				if (content.charAt(1) != '0' && content.charAt(1) != '1'
						&& content.charAt(1) != '2' && content.charAt(1) != '3'
						&& content.charAt(1) != '4' && content.charAt(1) != '5') {
					continue;
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
		return lyrics;
	}

	// ShuffleMusic implementation
	public static Path shuffle(ArrayList<Path> songArray) {
		if (specifiedPath == null) {
			return null;
		}
		Path songPath = null;
		if (songArray.isEmpty()) {
			return null;
		}
		songPath = songArray.get((int) (Math.random() * songArray.size()));
		return songPath.toAbsolutePath();
	}

	public static void refreshDirectory(String fileExtension) {
		if (specifiedPath == null) {
			return;
		}
		switch (fileExtension) {
		case flacExt:
			if (!songList.isEmpty())
				songList.clear();
			if (!folderList.isEmpty())
				folderList.clear();
			FileHandler.recursiveDirectoryHandler(specifiedPath, songList,
					folderList, fileExtension);
			break;
		case lrcExt:
			if (!lyricsList.isEmpty())
				lyricsList.clear();
			if (!folderList.isEmpty())
				folderList.clear();
			FileHandler.recursiveDirectoryHandler(specifiedPath, lyricsList,
					folderList, lrcExt);
			break;
		default:
			break;
		}
	}

	public static String getLRCFilePath(String songPath) {
		if (specifiedPath == null) {
			return null;
		}
		if (lyricsList.isEmpty()) {
			refreshDirectory(lrcExt);
		}
		String str = songPath.substring(0, songPath.length() - 5) + lrcExt;
		for (Path f : lyricsList) {
			if (f.toString().equals(str)) {
				return str;
			}
		}
		return null;
	}

	public static String getSongName(String songPath) {
		String songName = "";
		String copyOfPath = songPath;
		int i = songPath.length() - 1;
		boolean slashNotFound = true;
		while (slashNotFound) {
			if (songPath.charAt(i) == File.separator.toCharArray()[0]) {
				songName = copyOfPath.substring(i + 1);
				break;
			}
			i--;
		}
		return songName.substring(0, songName.length() - 5);
	}

	public static String fileExplorer(String response) {
		if (specifiedPath == null) {
			return null;
		}
		String res = null;
		for (Iterator<Path> iterator = songList.iterator(); iterator.hasNext();) {
			Path song = iterator.next();
			if (song.toString().contains(response.toLowerCase())) {
				res = song.toString();
				break;
			}
		}
		return res;
	}

	/* Be ware! Curly braces could be a potential rich source of bugs. */
	public static Path songMenu(ArrayList<Path> songsHavePicked) {
		if (specifiedPath == null) {
			return null;
		}
		if (songIdx < songList.size()) {
			do {
				songToPlay = shuffle(songList);
			} while (songsHavePicked.contains(songToPlay));
			if (!songsHavePicked.contains(songToPlay))
				songIdx++;
			return songToPlay;
		} else {
			return null;
		}
	}
}
