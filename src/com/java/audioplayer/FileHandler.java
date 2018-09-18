package com.java.audioplayer;

import java.io.File;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;

public class FileHandler {

	/**
	 * Given a path name, parse files and directories in current path name, and
	 * store files into fileList and directories folderList.
	 * 
	 * @param pathName
	 * @param fileList
	 * @param folderList
	 * @return
	 * @throws IOException
	 */
	public static boolean recursiveDirectoryHandler(Path pathName,
			ArrayList<File> fileList, ArrayList<Path> folderList,
			String fileExtension) {
		boolean stillHasFolder = false;
		DirectoryStream<Path> stream;
		try {
			if (!pathName.isAbsolute()) {
				stream = Files.newDirectoryStream(pathName.toAbsolutePath());
			} else {
				stream = Files.newDirectoryStream(pathName);
			}
			for (Path entry : stream) {
				if (fileHander(entry, fileList, fileExtension)) {
					;
				} else if (entry.toAbsolutePath().toFile().isDirectory()
						&& !folderList.contains(entry.toAbsolutePath())) {
					folderList.add(entry.toAbsolutePath());
					stillHasFolder = true;
				}
			}
			stream.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return stillHasFolder;
	}

	// This method is used for processing pure files only, not for
	// directory.
	public static boolean fileHander(Path entry, ArrayList<File> files,
			String fileExtension) {
		// String processedFullName = delimiterCanceller(file);
		boolean canHandleWithFileHandler = false;
		if (entry.toAbsolutePath().toFile().isFile()
				&& entry.toAbsolutePath().toFile().getAbsolutePath()
						.endsWith(fileExtension)
				&& !files.contains(entry.toAbsolutePath().toFile())) {
			canHandleWithFileHandler = true;
			files.add(entry.toAbsolutePath().toFile());
		}
		return canHandleWithFileHandler;
	}
}
