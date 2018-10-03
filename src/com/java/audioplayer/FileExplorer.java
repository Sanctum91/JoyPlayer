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

public final class FileExplorer {
	private static Path specifiedPath;
	private static ArrayList<Path> FLACsongList;
	private static ArrayList<Path> mp3songList;
	private static ArrayList<Path> folderList;
	private static ArrayList<Path> lyricsList;
	private static Path songToPlay;
	private static final String flacExt = ".flac";
	private static final String mp3Ext = ".mp3";
	private static final String lrcExt = ".lrc";
	private static JFileChooser fileChooser;
	private static Object lock;
	private static int FLACsongIdx = 0;
	private static int mp3songIdx = 0;
	private static boolean alreadyStartOneApp;

	static {
		// Make sure that there is no more than one single Java process that do
		// "plays the FLAC audio file".
		alreadyStartOneApp = false;
		Process process = null;
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
				alreadyStartOneApp = !alreadyStartOneApp;
			}
			br.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		FLACsongList = new ArrayList<>();
		mp3songList = new ArrayList<>();
		folderList = new ArrayList<>();
		lyricsList = new ArrayList<>();
		lock = new Object();
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
						FLACsongList, folderList, flacExt);
				FileHandler.recursiveDirectoryHandler(specifiedPath,
						mp3songList, folderList, mp3Ext);
			}
		}
		// parsing files and directories under specified path
		// refreshDirectory(lrcExt);
	}

	private FileExplorer() {
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
						|| content.contains("Öø×÷È¨")) {
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

	public static void refreshDirectory(String fileExtension) {
		if (specifiedPath == null) {
			return;
		}
		switch (fileExtension) {
		case flacExt:
			if (!FLACsongList.isEmpty())
				FLACsongList.clear();
			if (!folderList.isEmpty())
				folderList.clear();
			FileHandler.recursiveDirectoryHandler(specifiedPath, FLACsongList,
					folderList, fileExtension);
			break;
		case mp3Ext:
			if (!mp3songList.isEmpty())
				mp3songList.clear();
			if (!folderList.isEmpty())
				folderList.clear();
			FileHandler.recursiveDirectoryHandler(specifiedPath, mp3songList,
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

	public static String getSongName(Path songPath) {
		String name = songPath.getFileName().toString();
		if (name.endsWith(flacExt)) {
			return name.substring(0, name.length() - 5);
		} else if (name.endsWith(mp3Ext)) {
			return name.substring(0, name.length() - 4);
		}
		return "Audio Player";
	}

	public static String fileSearcher(String response) {
		if (specifiedPath == null) {
			return null;
		}
		String res = null;
		if (response.endsWith(flacExt)) {
			for (Iterator<Path> iterator = FLACsongList.iterator(); iterator
					.hasNext();) {
				Path song = iterator.next();
				if (song.getFileName().toString().toLowerCase()
						.contains(response.toLowerCase())) {
					res = song.toString();
					break;
				}
			}
		} else if (response.endsWith(mp3Ext)) {
			for (Iterator<Path> iterator = mp3songList.iterator(); iterator
					.hasNext();) {
				Path song = iterator.next();
				if (song.getFileName().toString().toLowerCase()
						.contains(response.toLowerCase())) {
					res = song.toString();
					break;
				}
			}
		}
		return res;
	}

	/* Be ware! Curly braces could be a potential rich source of bugs. */
	public static Path songMenu(ArrayList<Path> songsHavePicked) {
		if (specifiedPath == null) {
			return null;
		}
		if (Math.random() > 0.3) {
			if (FLACsongIdx < FLACsongList.size()) {
				do {
					songToPlay = FLACsongList
							.get((int) (Math.random() * FLACsongList.size()));
				} while (songsHavePicked.contains(songToPlay));
				if (!songsHavePicked.contains(songToPlay))
					FLACsongIdx++;
				return songToPlay;
			}
			return null;
		} else {
			if (mp3songIdx < mp3songList.size()) {
				do {
					songToPlay = mp3songList
							.get((int) (Math.random() * mp3songList.size()));
				} while (songsHavePicked.contains(songToPlay));
				if (!songsHavePicked.contains(songToPlay))
					mp3songIdx++;
				return songToPlay;
			}
			return null;
		}
	}
}
