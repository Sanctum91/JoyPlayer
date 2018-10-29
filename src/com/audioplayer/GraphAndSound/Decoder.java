package com.audioplayer.GraphAndSound;

import java.io.BufferedInputStream;
import java.util.logging.Logger;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.nio.file.Path;
import java.util.logging.Level;

import javax.sound.sampled.LineUnavailableException;

import com.mp3.decoder.JavaLayerException;

public abstract class Decoder {

	public static RandomAccessFile raf = null;

	public static InputStream byteBuffer = null;

	protected static Decoder decoder = null;

	protected double audioLength;

	protected static int sampleRateInHz;

	protected static int numOfChannels;

	protected static int bitsPerSample;

	protected static final int bitsofOneByte = 8;

	protected static boolean isReady = false;

	static {

	}

	public abstract double getAudioLength() throws IOException;

	protected Decoder(Path path) throws IOException, InterruptedException {
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

	protected static Decoder getInstance() {
		return decoder;
	}

	protected static void close() throws IOException {
		if (decoder != null) {
			decoder = null;
		}
		if (raf != null) {
			raf.close();
			raf = null;
		}
		if (byteBuffer != null) {
			byteBuffer.close();
			byteBuffer = null;
		}
	}

	/**
	 * To check if initialization has completed.
	 * 
	 * @return
	 * @throws InterruptedException
	 */
	protected static boolean isReady() throws InterruptedException {
		Logger.getGlobal().log(Level.INFO, "Check if decoder is ready");
		int count = 0;
		while (!isReady && count < 1500) {
			Thread.sleep(1);
			count++;
		}
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

	protected abstract int getSampleRateInHz();

	protected abstract int getBitsPerSample();

	protected abstract int getNumOfChannels();

	protected abstract long getTotalSamplesInStream();

	protected abstract boolean sampleProcessor(double request)
			throws IOException, BadFileFormatException,
			LineUnavailableException, InterruptedException, JavaLayerException;

	protected abstract double getCurrentPosition() throws IOException;

}
