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
package com.flac.decoder;

import java.io.BufferedInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;

import javax.sound.sampled.LineUnavailableException;
import javax.swing.JOptionPane;

import com.audioplayer.GraphAndSound.Codec;
import com.audioplayer.GraphAndSound.GUISynthesiszer;

public final class FLACCodec extends Codec {

	/**
	 * Constants for verifying stream marker.
	 */
	private static final int valueOfCharf = 0x66;

	private static final int valueOfCharL = 0x4C;

	private static final long streamMarker = 0x664C6143L;

	// Fixed prediction coefficients.
	private static final int[][] FIXED_PREDICTION_COEFFICIENTS = { {}, { 1 },
			{ 2, -1 }, { 3, -3, 1 }, { 4, -6, 4, -1 }, };

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

	// Store header bytes for CRC verification
	private static ArrayList<Integer> rawHeader = new ArrayList<Integer>();

	private static Stream input = null;

	private static double request = -0.0001;

	private static boolean waySearching = false;

	private static byte[] sampleInBytes = null;

	private int fixedBlockSize;

	private long totalSamplesInStream;

	private static int constantBlockSize;

	private long metadataEndPos;

	@Override
	public double getAudioLength() {
		return audioLength;
	}

	public static int getConstantBlockSize() {
		return constantBlockSize;
	}

	public int getFixedBlockSize() {
		return fixedBlockSize;
	}

	public static int getSampleRateInHz() {
		return sampleRateInHz;
	}

	public static int getNumOfChannels() {
		return numOfChannels;
	}

	public static int getBitsPerSample() {
		return bitsPerSample;
	}

	public long getTotalSamplesInStream() {
		return totalSamplesInStream;
	}

	// Close stream
	public static void closeFile() {
		if (codec != null) {
			try {
				close();
			} catch (IOException e) {
				e.printStackTrace();
			}
			codec = null;
		}
	}

	public static FLACCodec getInstance() {
		return (FLACCodec) codec;
	}

	// Test if codec is handling the search request.
	public static Boolean waySearching() {
		return waySearching;
	}

	// Create a new instance in singleton mode.
	public static FLACCodec createInstance(Path songPath) {
		codec = null;
		try {
			codec = new FLACCodec(songPath);
		} catch (IOException | BadFileFormatException | InterruptedException e) {
			e.printStackTrace();
		}
		return (FLACCodec) codec;
	}

	// Constructor
	private FLACCodec(Path songPath) throws IOException,
			BadFileFormatException, InterruptedException {
		super(songPath);
		input = new Stream(songPath);
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
			if (!isReady) {
				isReady = !isReady;
			}
			JOptionPane.showMessageDialog(null,
					"Cannot find valid stream marker!", "Error Occurred",
					JOptionPane.ERROR_MESSAGE);
			throw new BadFileFormatException("Invalid stream marker!");
		}
		/**
		 * METADATA_BLOCK_HEADER codec (There could be more than one metadata
		 * blocks, and all the block headers shall the same size)
		 */
		boolean isLast = false;
		while (!isLast) {
			// process each codec
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
	 * verify CRC-8 code for the given integer array list
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

	protected static void close() throws IOException {
		input.close();
		input = null;
	}

	/**
	 * Search for the closet frame in respect to given value, the value should
	 * range from 0 and 1, mainly used for seeking random sample position.
	 * 
	 * @param samplePos
	 * @throws IOException
	 * @throws BadFileFormatException
	 */
	public long[][] findNextDecodeableBlock(long samplePos) throws IOException,
			BadFileFormatException {
		while (GUISynthesiszer.wayPlaying()) {
			if (!waySearching) {
				waySearching = !waySearching;
			}
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

	public static void disableSearching() {
		waySearching = false;
	}

	/**
	 * Search stream till next frame header is found.
	 * 
	 * @param bytePos
	 * @return
	 * @throws IOException
	 */
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

	/**
	 * Decode one single frame and play the decoded audio data.
	 * 
	 * @throws BadFileFormatException
	 * @throws IOException
	 * @throws LineUnavailableException
	 * @throws InterruptedException
	 */
	public void decodeAndPlay() throws BadFileFormatException, IOException,
			LineUnavailableException, InterruptedException {
		long[][] samples = null;
		if (request >= 0) {
			long samplePos = Math.round(getTotalSamplesInStream() * request);
			samples = findNextDecodeableBlock(samplePos);
		} else {
			if (codec == null)
				return;
			Object[] temp = readNextBlock(false);
			if (temp != null) {
				samples = (long[][]) temp[0];
			}
		}
		if (samples == null)
			return;
		// convert samples to channel-interleaved bytes in little-endian
		int bytesPerSample = getBitsPerSample() / 8;
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
				throw codec.new BadFileFormatException("Reserved bit");
			int blockStrategy = rawHeader.get(1) & 1;
			rawHeader.add(input.readByte());
			int blockSizeCode = rawHeader.get(2) >> 4;
			int sampleRate = rawHeader.get(2) & 0xF;
			rawHeader.add(input.readByte());
			int channleAssign = rawHeader.get(3) >> 4;
			/* Sample size in bits */
			switch ((rawHeader.get(3) >> 1) & 7) {
			case 1:
				if (bitsPerSample != 8)
					throw codec.new BadFileFormatException(
							"Sample depth mismatch");
				break;
			case 2:
				if (bitsPerSample != 12)
					throw codec.new BadFileFormatException(
							"Sample depth mismatch");
				break;
			case 4:
				if (bitsPerSample != 16)
					throw codec.new BadFileFormatException(
							"Sample depth mismatch");
				break;
			case 5:
				if (bitsPerSample != 20)
					throw codec.new BadFileFormatException(
							"Sample depth mismatch");
				break;
			case 6:
				if (bitsPerSample != 24)
					throw codec.new BadFileFormatException(
							"Sample depth mismatch");
				break;
			default:
				throw codec.new BadFileFormatException("Sample depth mismatch");
			}
			/* Reserved bit */
			if ((rawHeader.get(3) & 1) != 0)
				throw codec.new BadFileFormatException(
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
				throw codec.new BadFileFormatException(
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
				throw codec.new BadFileFormatException(
						"Unexpected CRC-8 value.");
			long[][] samples = decodeSubframes(blockSize, bitsPerSample,
					channleAssign);
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
				throw codec.new BadFileFormatException("Reserved bit");
			int blockStrategy = input.readBit(1);

			// Read numerous header fields, and ignore some of them
			int blockSizeCode = input.readBit(4);
			int sampleRateCode = input.readBit(4);
			int chanAsgn = input.readBit(4);
			switch (input.readBit(3)) {
			case 1:
				if (bitsPerSample != 8)
					throw codec.new BadFileFormatException(
							"Sample depth mismatch");
				break;
			case 2:
				if (bitsPerSample != 12)
					throw codec.new BadFileFormatException(
							"Sample depth mismatch");
				break;
			case 4:
				if (bitsPerSample != 16)
					throw codec.new BadFileFormatException(
							"Sample depth mismatch");
				break;
			case 5:
				if (bitsPerSample != 20)
					throw codec.new BadFileFormatException(
							"Sample depth mismatch");
				break;
			case 6:
				if (bitsPerSample != 24)
					throw codec.new BadFileFormatException(
							"Sample depth mismatch");
				break;
			default:
				throw codec.new BadFileFormatException(
						"Reserved/invalid sample depth");
			}
			if (input.readBit(1) != 0)
				throw codec.new BadFileFormatException("Reserved bit");

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
				throw codec.new BadFileFormatException("Reserved block size");

			if (sampleRateCode == 12)
				input.readBit(8);
			else if (sampleRateCode == 13 || sampleRateCode == 14)
				input.readBit(16);

			input.readBit(8);
			// Decode each channel's subframe, then skip footer
			long[][] samples = decodeSubframes(blockSize, bitsPerSample,
					chanAsgn);
			input.alignToByte();
			input.readBit(16);
			if (codec == null) {
				return null;
			}
			return new Object[] {
					samples,
					rawPosition
							* (blockStrategy == 0 ? getConstantBlockSize() : 1) };
		}

	}

	/**
	 * Given block size, sample depth and channel assignment of current frame,
	 * decode each sub-frame of current frame.
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
			throw codec.new BadFileFormatException(
					"Reserved channel assignment");
		return result;
	}

	/**
	 * Decode each sub-frame in respect to sample depth.
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
			throw codec.new BadFileFormatException("Invalid padding bit");
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
			throw codec.new BadFileFormatException("Invalid predicton order.");
		for (int i = 0; i < result.length; i++)
			result[i] <<= shift;
	}

	/**
	 * Entropy coding method codec, with which to restore residuals into
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
			throw codec.new BadFileFormatException(
					"Reserved residual coding method");
		int paramBits = method == 0 ? 4 : 5;
		int escapeParam = method == 0 ? 0xF : 0x1F;
		int partitionOrder = input.readBit(4);
		int numPartitions = 1 << partitionOrder;
		if (result.length % numPartitions != 0)
			throw codec.new BadFileFormatException(
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

	/**
	 * User-defined inner class intended for processing bits and bytes in files.
	 * 
	 * @author unascribed
	 *
	 */
	private static final class Stream implements Runnable {
		private long bytePosition;
		private long bitBuffer;
		private int bitBufferLen;

		// Stream constructor
		public Stream(Path file) throws IOException {
			bytePosition = 0;
			bitBufferLen = 0;
		}

		public void close() throws IOException {
			raf.close();
		}

		/**
		 * Get the length of current byte stream, measured in bytes.
		 * 
		 * @return
		 * @throws IOException
		 */
		public long getLength() throws IOException {
			return raf.length();
		}

		/**
		 * Get current file pointer position , measured in bytes.
		 * 
		 * @return
		 */
		public long getPosition() {
			return bytePosition;
		}

		/**
		 * Re-implement random access to file position
		 * 
		 * @param pos
		 * @throws IOException
		 */
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

		/**
		 * Read one single byte from stream
		 * 
		 * @return
		 * @throws IOException
		 */
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

		/**
		 * Read one single bit from stream
		 * 
		 * @param n
		 * @return
		 * @throws IOException
		 */
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

		/**
		 * Get signed integer value of the given one.
		 * 
		 * @param n
		 * @return
		 * @throws IOException
		 */
		public int readSignedBit(int n) throws IOException {
			return (readBit(n) << (32 - n)) >> (32 - n);
		}

		/**
		 * Padding bit alignment to ensure fixed sample depth.
		 */
		public void alignToByte() {
			bitBufferLen -= bitBufferLen % 8;
		}

		@Override
		public void run() {
			// TODO Auto-generated method stub

		}
	}

}
