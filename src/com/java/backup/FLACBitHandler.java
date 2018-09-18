package com.java.backup;

import java.io.BufferedInputStream;
import java.io.EOFException;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.util.Arrays;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;

public class FLACBitHandler {
	private Stream input;
	private static FLACBitHandler handler;
	private static SourceDataLine line;
	private int fixedBlockSize;
	private int sampleRateInHz;
	private int numOfChannels;
	private int bitsPerSample;
	private long totalSamplesInStream;
	private int fileLength;
	private double audioLength;
	private int constantBlockSize;
	private static final int[][] FIXED_PREDICTION_COEFFICIENTS = { {}, { 1 },
			{ 2, -1 }, { 3, -3, 1 }, { 4, -6, 4, -1 }, };

	public double getAudioLength() {
		return audioLength;
	}

	public long getFileLength() {
		return fileLength;
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

	public long getTotalSamplesInStream() {
		return totalSamplesInStream;
	}

	public static void closeFile() throws IOException {
		if (handler != null) {
			handler.close();
			handler = null;
		}
		if (line != null) {
			line.drain();
			line.close();
			line = null;
		}
	}

	public FLACBitHandler(String fileNameFullPath)
			throws InvalidInputException, IOException, BadFileFormatException {
		input = new Stream(new File(fileNameFullPath));
		/**
		 * <32 bits> "fLaC", the FLAC stream marker in ASCII, meaning byte 0 of
		 * the stream is 0x66, followed by 0x4C 0x61 0x43
		 */
		// boolean isMagicString = true;
		// while (isMagicString) {
		// isMagicString = input.readBit(8) != 0x66;
		// }
		if (input.readBit(32) != 0x664C6143)
			throw new BadFileFormatException("Invalid stream marker!");
		// if (input.readBit(32) != 0x664C6143)
		// throw new BadFileFormatException("Invalid stream marker!");
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
				int minBlockSize = 0;
				int maxBlockSize = 0;
				minBlockSize = input.readBit(16);
				maxBlockSize = input.readBit(16);
				if (minBlockSize == maxBlockSize)
					constantBlockSize = maxBlockSize;
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
		audioLength = (double) totalSamplesInStream / sampleRateInHz;
		fileLength = (int) input.getLength();
		String audioInfoString = "";
		audioInfoString += "Audio length: " + (int) getAudioLength() / 60
				+ " min " + (int) getAudioLength() % 60 + " s";
		audioInfoString += ", Bit rate: "
				+ (int) ((getFileLength() - input.bytePosition) * 8
						/ getAudioLength() / 1000) + " kbps";
		audioInfoString += "\nNow playing audio file ...";
		System.out.println(audioInfoString);
	}

	public void close() throws IOException {
		input.close();
	}

	/**
	 * an audio player with which to parse FLAC format file.
	 * 
	 * @param fileNameFullPath
	 *            absolute file path of FLAC file to play.
	 * @throws BadFileFormatException
	 * @throws InvalidInputException
	 * @throws IOException
	 * @throws LineUnavailableException
	 * @throws InterruptedException
	 * @throws Exception
	 */
	public static void FLACPlayer(String fileNameFullPath)
			throws BadFileFormatException, InvalidInputException, IOException,
			LineUnavailableException, InterruptedException {
		handler = new FLACBitHandler(fileNameFullPath);
		long[][] samples = null;
		AudioFormat format = new AudioFormat(handler.getSampleRateInHz(),
				handler.getBitsPerSample(), handler.getNumOfChannels(), true,
				false);
		line = (SourceDataLine) AudioSystem.getLine(new DataLine.Info(
				SourceDataLine.class, format));
		line.open(format);
		line.start();
		long length = System.currentTimeMillis()
				+ (long) handler.getAudioLength() * 1000;
		Thread.sleep(1200);
		while (System.currentTimeMillis() < length) {
			Object[] temp = handler.readNextBlock();
			if (temp != null)
				samples = (long[][]) temp[0];
			if (samples == null)
				return;
			// convert samples to channel-interleaved bytes in little-endian
			int bytesPerSample = handler.getBitsPerSample() / 8;
			byte[] sampleInBytes = new byte[samples.length * samples[0].length
					* bytesPerSample];
			for (int i = 0, k = 0; i < samples[0].length; i++) {
				for (int ch = 0; ch < samples.length; ch++) {
					long val = samples[ch][i];
					for (int j = 0; j < bytesPerSample; j++, k++)
						sampleInBytes[k] = (byte) (val >>> (j << 3));
				}
			}
			line.write(sampleInBytes, 0, sampleInBytes.length);
		}
		closeFile();
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
	 * @throws InvalidInputException
	 * @throws Exception
	 */
	public Object[] readNextBlock() throws BadFileFormatException,
			InvalidInputException, IOException {
		/* Sync code */
		int byteVal = input.readByte();
		if (byteVal == -1)
			return null;
		int sync = byteVal << 6 | input.readBit(6);
		if (sync != 0x3FFE)
			return null;
		if (input.readBit(1) != 0)
			throw new BadFileFormatException("Reserved bit");
		int blockStrategy = input.readBit(1);
		int blockSizeCode = input.readBit(4);
		int sampleRate = input.readBit(4);
		int channleAssign = input.readBit(4);
		/* Sample size in bits */
		switch (input.readBit(3)) {
		case 1:
			if (bitsPerSample != 8)
				throw new BadFileFormatException("Sample depth mismatch");
			break;
		case 2:
			if (bitsPerSample != 12)
				throw new BadFileFormatException("Sample depth mismatch");
			break;
		case 4:
			if (bitsPerSample != 16)
				throw new BadFileFormatException("Sample depth mismatch");
			break;
		case 5:
			if (bitsPerSample != 20)
				throw new BadFileFormatException("Sample depth mismatch");
			break;
		case 6:
			if (bitsPerSample != 24)
				throw new BadFileFormatException("Sample depth mismatch");
			break;
		default:
			throw new BadFileFormatException("Sample depth mismatch");
		}
		/* Reserved bit */
		if (input.readBit(1) != 0)
			throw new BadFileFormatException("reserved bit value is not right.");

		byteVal = input.readBit(8);
		long rawPosition;
		if (byteVal < 0x80)
			rawPosition = byteVal;
		else {
			int rawPosNumBytes = Integer.numberOfLeadingZeros(~(byteVal << 24)) - 1;
			rawPosition = byteVal & (0x3F >>> rawPosNumBytes);
			for (int i = 0; i < rawPosNumBytes; i++)
				rawPosition = (rawPosition << 6) | (input.readBit(8) & 0x3F);
		}

		/* Block size in inter-channel samples */
		int blockSize;
		if (blockSizeCode == 1)
			blockSize = 192;
		else if (blockSizeCode >= 2 && blockSizeCode <= 5)
			blockSize = 576 << (blockSizeCode - 2);
		else if (blockSizeCode == 6)
			blockSize = input.readBit(8) + 1;
		else if (blockSizeCode == 7) {
			blockSize = input.readBit(16) + 1;
		} else if (blockSizeCode >= 8 && blockSizeCode <= 15)
			blockSize = 256 << (blockSizeCode - 8);
		else
			throw new BadFileFormatException("Reserved bit.");
		/* Sample rate */
		if (sampleRate == 12)
			input.readBit(8);
		else if (sampleRate == 13 || sampleRate == 14)
			input.readBit(16);
		/* read CRC-8 */
		input.readBit(8);
		long[][] samples = decodeSubframes(blockSize, bitsPerSample,
				channleAssign);
		/* Align to byte */
		input.alignToByte();
		/* read Frame footer */
		input.readBit(16);
		/* calculate the sample number from the frame number if needed */
		// if (blockStrategy == 0)
		// sampleStartingPosition = rawPosition * constantBlockSize;
		// else
		// sampleStartingPosition = rawPosition;
		return new Object[] { samples,
				rawPosition * (blockStrategy == 0 ? constantBlockSize : 1) };
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
	 * @throws InvalidInputException
	 * @throws Exception
	 */
	private long[][] decodeSubframes(int blockSize, int sampleDepth,
			int chanAsgn) throws BadFileFormatException, InvalidInputException,
			IOException {
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
			throw new BadFileFormatException("Reserved channel assignment");
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
	 * @throws InvalidInputException
	 * @throws Exception
	 */
	private void decodeSubframe(int sampleDepth, long[] result)
			throws BadFileFormatException, InvalidInputException, IOException {
		if (input.readBit(1) != 0)
			throw new BadFileFormatException("Invalid padding bit");
		int type = input.readBit(6);
		int shift = input.readBit(1);
		if (shift == 1) {
			while (input.readBit(1) == 0)
				shift++;
		}
		sampleDepth -= shift;
		if (type == 0) // Constant coding
			Arrays.fill(result, 0, result.length,
					input.readSignedBit(sampleDepth));
		else if (type == 1) // Verbatim coding
			for (int i = 0; i < result.length; i++)
				result[i] = input.readSignedBit(sampleDepth);
		else if (8 <= type && type <= 12 || 32 <= type && type <= 63) {
			int predOrder;
			int[] lpcCoefs;
			int lpcShift;
			if (type <= 12) { // Fixed prediction
				// predOrder = type - 8;
				predOrder = type & 7;
				for (int i = 0; i < predOrder; i++)
					result[i] = input.readSignedBit(sampleDepth);
				lpcCoefs = FIXED_PREDICTION_COEFFICIENTS[predOrder];
				lpcShift = 0;
			} else { // Linear predictive coding
				// predOrder = type - 31;
				predOrder = (type & 0x1F) + 1;
				for (int i = 0; i < predOrder; i++)
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
			throw new BadFileFormatException("Reserved subframe type");
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
	 * @throws InvalidInputException
	 * @throws Exception
	 */
	private void decodeRiceResiduals(int warmup, long[] result)
			throws BadFileFormatException, InvalidInputException, IOException {
		int method = input.readBit(2);
		if (method >= 2)
			throw new BadFileFormatException("Reserved residual coding method");
		int paramBits = method == 0 ? 4 : 5;
		int escapeParam = method == 0 ? 0xF : 0x1F;
		int partitionOrder = input.readBit(4);
		int numPartitions = 1 << partitionOrder;
		if (result.length % numPartitions != 0)
			throw new BadFileFormatException(
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

	public static final class Stream {
		private RandomAccessFile raf;
		private long bytePosition;
		private InputStream byteBuffer;
		private long bitBuffer;
		private int bitBufferLen;

		public Stream(File file) throws IOException {
			raf = new RandomAccessFile(file, "r");
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
	}

	public class BadFileFormatException extends Exception {

		public BadFileFormatException(String message) {
			super(message);
		}

		/**
		 * default serial ID
		 */
		private static final long serialVersionUID = 1L;
	}

	public class InvalidInputException extends Exception {

		public InvalidInputException(String message) {
			super(message);
		}

		/**
		 * default serial ID
		 */
		private static final long serialVersionUID = 1L;
	}

}
