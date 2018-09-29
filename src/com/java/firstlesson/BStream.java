package com.java.firstlesson;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

public class BStream {
	private FileInputStream iStream;
	InputStream bufferedStream;

	public BStream() throws IOException {
		iStream = new FileInputStream("D:" + File.separator + "FLAC Library"
				+ File.separator + "Bazzi - Mine" + File.separator
				+ "Bazzi - Mine.flac");
		bufferedStream = new BufferedInputStream(iStream);
		System.out.println(File.separator);
	}

	public static void main(String[] args) {
		try {
			BStream bStream = new BStream();
			System.out.println(bStream.bufferedStream.read());
			System.out.println(bStream.bufferedStream.read());
			System.out.println(bStream.bufferedStream.read());
			System.out.println(bStream.bufferedStream.read());
			bStream.iStream.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
