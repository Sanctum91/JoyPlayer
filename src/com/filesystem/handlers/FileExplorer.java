/*
 *    Copyright [2018] [Justin Nelson]

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
import java.util.Iterator;
import java.util.LinkedList;
import java.util.NavigableSet;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.JFileChooser;
import javax.swing.JLabel;

import com.audioplayer.GraphAndSound.FormatExtension;
import com.audioplayer.GraphAndSound.GUISynthesiszer;

public final class FileExplorer {
	private static final FileExplorer EXPLORER = new FileExplorer();
	private static float pennySec;
	private static float currentKey;
	private static String preText;
	private Path[] songList;
	private LinkedList<Path> FLACsongList;
	private LinkedList<Path> mp3songList;
	private LinkedList<Path> folderList;
	private LinkedList<Path> lyricsList;
	private Path specifiedPath;
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
			Logger.getGlobal().log(Level.FINE,
					"main class name: " + mainClassName);
			Logger.getGlobal().log(Level.FINE, fileSourceName);
			while ((p = br.readLine()) != null) {
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
		EXPLORER.songList = null;
		EXPLORER.FLACsongList = new LinkedList<>();
		EXPLORER.mp3songList = new LinkedList<>();
		EXPLORER.folderList = new LinkedList<>();
		EXPLORER.lyricsList = new LinkedList<>();
		specifiedPath = null;
		if (!alreadyStartOneApp) {
			synchronized (this) {
				fileChooser = new JFileChooser();
				fileChooser.setDialogTitle("Please choose a FLAC directory");
				this.notify();
				fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
			}
			int response = fileChooser.showOpenDialog(null);
			if (response == JFileChooser.APPROVE_OPTION) {
				// Initialize file paths and directory paths based on current
				// specified path.
				specifiedPath = Paths.get(fileChooser.getSelectedFile()
						.getAbsolutePath());
				FileAndDirectory.recursiveDirectoryHandler(specifiedPath,
						EXPLORER.FLACsongList, EXPLORER.folderList,
						FormatExtension.FLAC.getExtension());
				FileAndDirectory.recursiveDirectoryHandler(specifiedPath,
						EXPLORER.mp3songList, EXPLORER.folderList,
						FormatExtension.MP3.getExtension());
				FileAndDirectory.recursiveDirectoryHandler(specifiedPath,
						EXPLORER.lyricsList, EXPLORER.folderList,
						FormatExtension.LRC.getExtension());
				EXPLORER.FLACsongList.addAll(EXPLORER.mp3songList);
				EXPLORER.songList = new Path[EXPLORER.FLACsongList.size()];
				EXPLORER.FLACsongList.toArray(EXPLORER.songList);
				Arrays.sort(songList);
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
	public static TreeMap<Float, String> lrcParser(Path lycFilePath) {
		if (EXPLORER.specifiedPath == null) {
			return null;
		}
		Font monospace = new Font("Monospace", Font.TRUETYPE_FONT, 22);
		JLabel label = new JLabel();
		FontMetrics fontMetrics = label.getFontMetrics(monospace);
		TreeMap<Float, String> lyrics = new TreeMap<Float, String>();
		try {
			Logger.getGlobal().log(Level.INFO, "Parsing lyric contents.");
			BufferedReader reader = new BufferedReader(new InputStreamReader(
					Files.newInputStream(lycFilePath), "UTF-8"));
			String content = "";
			while ((content = reader.readLine()) != null) {
				if (content.length() < 11 || content.charAt(6) != '.') {
					continue;
				}
				if (content.charAt(1) < '0' || content.charAt(1) > '9') {
					continue;
				}
				if (content.charAt(9) != ']') {
					content = content.substring(0, 9) + content.substring(10);
				}
				if (content.substring(10).equals("//")) {
					break;
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
				// store temporary partial result during replacing XML entities.
				StringBuilder temp = new StringBuilder();
				String store = lyricString;
				// A nested loop to replace HTML entities of each line of lyrics
				// with corresponding normal character representation.
				Outter_Loop: while (store.contains("&apos;")
						|| store.contains("&quot;") || store.contains("&amp;")) {
					for (int index = 0; index < store.length(); index++) {
						if (store.charAt(index) == '&') {
							// break when reaching the end of the String
							if (index + 4 > store.length() - 1) {
								break Outter_Loop;
							}
							// has encountered "'"
							if (store.charAt(index + 1) == 'a'
									&& store.charAt(index + 4) == 's') {
								// appear at the very head of the String
								if (index == 0) {
									temp.append("'");
									store = store.substring(6);
								} else if (index + 5 < store.length() - 1) {
									temp.append(store.substring(0, index));
									temp.append("'");
									store = store.substring(index + 6);
								} else {
									temp.append(store.substring(0,
											store.length() - 6));
									temp.append("'");
									break Outter_Loop;
								}
								if (!store.contains("&apos;")
										&& !store.contains("&quot;")
										&& !store.contains("&amp;")) {
									temp.append(store);
									break Outter_Loop;
								}
								continue Outter_Loop;
							} else if (store.charAt(index + 1) == 'q'
									&& store.charAt(index + 4) == 't') {
								if (index == 0) {
									temp.append("\"");
									store = store.substring(6);
								} else if (index + 5 < store.length() - 1) {
									temp.append(store.substring(0, index));
									temp.append("\"");
									store = store.substring(index + 6);
								} else {
									temp.append(store.substring(0,
											store.length() - 6));
									temp.append("\"");
									break Outter_Loop;
								}
								if (!store.contains("&apos;")
										&& !store.contains("&quot;")
										&& !store.contains("&amp;")) {
									temp.append(store);
									break Outter_Loop;
								}
								continue Outter_Loop;
							} else if (store.charAt(index + 1) == 'a'
									&& store.charAt(index + 3) == 'p') {
								if (index == 0) {
									temp.append("&");
									store = store.substring(6);
								} else if (index + 4 < store.length() - 1) {
									temp.append(store.substring(0, index));
									temp.append("&");
									store = store.substring(index + 5);
								} else {
									temp.append(store.substring(0,
											store.length() - 5));
									temp.append("&");
									break Outter_Loop;
								}
								if (!store.contains("&apos;")
										&& !store.contains("&quot;")
										&& !store.contains("&amp;")) {
									temp.append(store);
									break Outter_Loop;
								}
								continue Outter_Loop;
							}
						}
					}
				}
				if (!temp.toString().equals("")) {
					lyricString = temp.toString();
				}
				lyrics.put(pennySec, lyricString);
			}
			reader.close();
			// Resort time frame in ascending order.
			NavigableSet<Float> frames = lyrics.navigableKeySet();
			Iterator<Float> iterator = frames.iterator();
			Float f = -3.0f;
			Float ne = f + 0.01f;
			int counter = 0;
			while (iterator.hasNext()) {
				counter++;
				if (f < 0.0f) {
					f = iterator.next();
				}
				// handling display issues.
				ne = iterator.next();
				if (fontMetrics.stringWidth(lyrics.get(f)) > 475) {
					Float differ = ne - f;
					Float fnext = differ * 0.5f + f;
					String str = lyrics.get(f);
					int idx = (int) ((str.length() - 1) * 0.5);
					while (str.charAt(idx) != ' ' && idx < str.length() - 5) {
						idx++;
					}
					lyrics.put(f, str.substring(0, idx));
					if (fontMetrics.stringWidth(str.substring(0, idx)) > 470) {
						int nextHalf = idx + (str.length() - 1 - idx) / 2;
						Loop: while (nextHalf != str.length() - 1) {
							if (str.charAt(nextHalf) != ' ') {
								nextHalf++;
							} else {
								break Loop;
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
					iterator = frames.iterator();
					for (int i = 0; i < counter; i++) {
						iterator.next();
					}
				}
				f = ne;
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		Logger.getGlobal().log(Level.INFO, "Lyrics parsing has finished.");
		return lyrics;
	}

	/**
	 * Based on given parameters to read the lyrics map, then synchronize the
	 * lyrics.
	 * 
	 * @param lyricDict
	 * @param frameKeys
	 * @param currentTimeInMicros
	 * @param songName
	 * @param idx
	 */
	public static void lyricsReader(TreeMap<Float, String> lyricDict,
			Float[] frames, long currentTimeInMicros, String songName, int idx) {
		try {
			if (frames.length != 0) {
				pennySec = (float) (currentTimeInMicros * 0.001 * 0.001);
				if (pennySec < frames[frames.length - 1].floatValue()
						&& pennySec > frames[0].floatValue()) {
					For_Loop: for (; idx < frames.length; idx++) {
						if (pennySec < frames[idx].floatValue()) {
							if (!songName
									.equals(lyricDict.get(frames[idx - 1]))
									|| currentKey != frames[idx]) {
								currentKey = frames[idx];
								songName = lyricDict.get(frames[idx - 1]);
								if (idx < 2) {
									GUISynthesiszer.setPrelableText(" ");
								} else
									GUISynthesiszer.setPrelableText('\u266A'
											+ " "
											+ lyricDict.get(frames[idx - 2])
											+ " " + '\u266A');
								GUISynthesiszer.setMidlableText('\u266A' + " "
										+ songName + " " + '\u266A');
								GUISynthesiszer.setNextlableText('\u266A' + " "
										+ lyricDict.get(currentKey) + " "
										+ '\u266A');
								Thread.sleep(300);
							}
							Thread.sleep(30);
							break For_Loop;
						}
					}
				} else if (pennySec < frames[0].floatValue()) {
					idx = 0;
					if (!songName.equals(" ")) {
						songName = " ";
						GUISynthesiszer.setPrelableText(songName);
						GUISynthesiszer.setMidlableText(songName);
						GUISynthesiszer.setNextlableText('\u266A' + " "
								+ lyricDict.get(frames[0]) + " " + '\u266A');
						Thread.sleep(50);
					}
				} else if (pennySec == frames[0].floatValue()) {
					currentKey = frames[0];
					if (!songName.equals('\u266A' + " "
							+ lyricDict.get(frames[0]) + " " + '\u266A')) {
						songName = '\u266A' + " " + lyricDict.get(frames[0])
								+ " " + '\u266A';
						GUISynthesiszer.setPrelableText(" ");
						GUISynthesiszer.setMidlableText(songName);
						GUISynthesiszer.setNextlableText('\u266A' + " "
								+ lyricDict.get(frames[0]) + " " + '\u266A');
						Thread.sleep(50);
					}
				} else {
					if (!songName.equals('\u266A' + " "
							+ lyricDict.get(frames[frames.length - 1]) + " "
							+ '\u266A')
							|| currentKey >= frames[frames.length - 1]) {
						currentKey = 0;
						songName = '\u266A' + " "
								+ lyricDict.get(frames[frames.length - 1])
								+ " " + '\u266A';
						preText = '\u266A' + " "
								+ lyricDict.get(frames[frames.length - 2])
								+ " " + '\u266A';
						GUISynthesiszer.setPrelableText(preText);
						GUISynthesiszer.setMidlableText(songName);
						GUISynthesiszer.setNextlableText(" ");
						Thread.sleep(50);
					}
				}
			}
			Thread.sleep(5);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Search the files based on the given file extension from the local file
	 * directory and refresh the file paths in the ArrayList of Class Path.
	 * 
	 * @param fileExtension
	 */
	public static void refreshDirectory(FormatExtension fileExtension) {
		if (EXPLORER.specifiedPath == null) {
			return;
		}
		switch (fileExtension) {
		case FLAC:
			if (!EXPLORER.FLACsongList.isEmpty())
				EXPLORER.FLACsongList.clear();
			if (!EXPLORER.folderList.isEmpty())
				EXPLORER.folderList.clear();
			FileAndDirectory.recursiveDirectoryHandler(EXPLORER.specifiedPath,
					EXPLORER.FLACsongList, EXPLORER.folderList,
					fileExtension.getExtension());
			EXPLORER.FLACsongList.addAll(EXPLORER.mp3songList);
			EXPLORER.songList = new Path[EXPLORER.FLACsongList.size()];
			EXPLORER.FLACsongList.toArray(EXPLORER.songList);
			Arrays.sort(EXPLORER.songList);
			break;
		case MP3:
			if (!EXPLORER.mp3songList.isEmpty())
				EXPLORER.mp3songList.clear();
			if (!EXPLORER.folderList.isEmpty())
				EXPLORER.folderList.clear();
			FileAndDirectory.recursiveDirectoryHandler(EXPLORER.specifiedPath,
					EXPLORER.mp3songList, EXPLORER.folderList,
					fileExtension.getExtension());
			EXPLORER.FLACsongList.addAll(EXPLORER.mp3songList);
			EXPLORER.songList = new Path[EXPLORER.FLACsongList.size()];
			EXPLORER.FLACsongList.toArray(EXPLORER.songList);
			Arrays.sort(EXPLORER.songList);
			break;
		case LRC:
			if (!EXPLORER.lyricsList.isEmpty())
				EXPLORER.lyricsList.clear();
			if (!EXPLORER.folderList.isEmpty())
				EXPLORER.folderList.clear();
			FileAndDirectory.recursiveDirectoryHandler(EXPLORER.specifiedPath,
					EXPLORER.lyricsList, EXPLORER.folderList,
					FormatExtension.LRC.getExtension());
			break;
		default:
			break;
		}
	}

	public static void refreshDirectory() {
		refreshDirectory(FormatExtension.MP3);
		refreshDirectory(FormatExtension.FLAC);
		refreshDirectory(FormatExtension.LRC);
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
			refreshDirectory(FormatExtension.LRC);
		}
		String str = null;
		if (songPath.toString().endsWith(FormatExtension.FLAC.getExtension())) {
			str = songPath.toString().substring(0,
					songPath.toString().length() - 5)
					+ FormatExtension.LRC.getExtension();
		} else if (songPath.toString().endsWith(
				FormatExtension.MP3.getExtension())) {
			str = songPath.toString().substring(0,
					songPath.toString().length() - 4)
					+ FormatExtension.LRC.getExtension();
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
		if (name.endsWith(FormatExtension.FLAC.getExtension())) {
			return name.substring(0, name.length() - 5);
		} else if (name.endsWith(FormatExtension.MP3.getExtension())) {
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
		for (Path path : EXPLORER.songList) {
			if (path.toString().toUpperCase().contains(response.toUpperCase())) {
				return path;
			}
		}
		return null;
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
		for (Path path : EXPLORER.songList) {
			if (!songsHavePicked.contains(path)) {
				return path;
			}
		}
		return null;
	}
}
