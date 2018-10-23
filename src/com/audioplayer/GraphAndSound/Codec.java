package com.audioplayer.GraphAndSound;

import java.io.BufferedInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.nio.file.Path;

public abstract class Codec {

	public static RandomAccessFile raf = null;

	public static InputStream byteBuffer = null;

	protected static Codec codec = null;

	protected double audioLength;

	protected static int sampleRateInHz;

	protected static int numOfChannels;

	protected static int bitsPerSample;

	protected static final int bitsofOneByte = 8;

	protected static boolean isReady = false;

	static {

	}

	public abstract double getAudioLength() throws IOException;

	protected Codec(Path path) throws IOException, InterruptedException {
		try {
			raf = new RandomAccessFile(path.toFile(), "r");
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		byteBuffer = new BufferedInputStream(new InputStream() {

			@Override
			public int read() throws IOException {
				return raf.read();
			}

			@Override
			public int read(byte[] b, int off, int len) throws IOException {
				return raf.read(b, off, len);
			}
		});
	}

	protected InputStream getCurrentStream() {
		return byteBuffer;
	}

	protected static Codec getInstance() {
		return codec;
	}

	protected static void close() throws IOException {
		// TODO Auto-generated method stub
	}

	/**
	 * To check if initialization has completed.
	 * 
	 * @return
	 */
	protected static boolean isReady() {
		return isReady;
	}

	/**
	 * User-defined exception for indicating unexpected file data/format has
	 * occurred.
	 * 
	 * @author Justin Nelson
	 *
	 */
	protected final class BadFileFormatException extends Exception {

		private static final long serialVersionUID = -6038660784354160702L;

		public BadFileFormatException(String message) {
			super(message);
		}
	}

}
