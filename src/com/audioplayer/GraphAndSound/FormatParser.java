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
package com.audioplayer.GraphAndSound;

import java.io.IOException;
import java.nio.file.Path;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.sound.sampled.LineUnavailableException;

import com.audioplayer.GraphAndSound.Codec.BadFileFormatException;
import com.codec.player.MP3Codec;
import com.codec.player.Player;
import com.flac.decoder.FLACCodec;
import com.mp3.codec.decoder.JavaLayerException;
import com.mp3.codec.decoder.SampleBuffer;

/**
 * Create an audio parser which is capable of automatically recognize audio
 * streams of different encoding formats, then using appropriate codec to decode
 * stream, and write decoded samples into audio device. That is to say, this is
 * a class with which to unify streams of different encoding formats.
 * 
 * @author Justin Nelson
 *
 */
public final class FormatParser {

	/**
	 * Create constant String fields for file extensions.
	 */
	private static Logger logger = Logger.getLogger(GUISynthesiszer.class
			.getName());
	private static Codec codec = null;
	private static boolean isFLACEncoded;
	private static boolean isMP3Encoded;
	private final static String flacExt = ".flac";
	private final static String mp3Ext = ".mp3";
	/**
	 * Create byte array to store decode audio data.
	 */
	private static byte[] sampleInBytes = null;

	/**
	 * Terminate decoding process.
	 * 
	 * @param songToPlay
	 */
	public static void closeFile(Path songToPlay) {
		isFLACEncoded = false;
		isMP3Encoded = false;
		if (songToPlay.toString().endsWith(flacExt)) {
			logger.log(Level.INFO, "Closing FLAC decoder...");
			FLACCodec.closeFile();
		} else if (songToPlay.toString().endsWith(mp3Ext)) {
			logger.log(Level.INFO, "Closing MP3 decoder...");
			MP3Codec.closeFile();
		}
	}

	/**
	 * Create an instance of the appropriate decoder based on file formats.
	 * 
	 * @param songToPlay
	 * @return the appropriate decoder
	 * @throws IOException
	 * @throws BadFileFormatException
	 * @throws JavaLayerException
	 */
	public static Codec createInstance(Path songToPlay) throws IOException,
			BadFileFormatException, JavaLayerException {
		if (songToPlay.toString().endsWith(flacExt)) {
			logger.log(Level.INFO, "Initializing FLAC codec...");
			return FLACCodec.createInstance(songToPlay);
		} else if (songToPlay.toString().endsWith(mp3Ext)) {
			logger.log(Level.INFO, "Initializing MP3 codec...");
			return MP3Codec.createInstance(songToPlay);
		}
		logger.log(Level.WARNING, "Failed to initialize.");
		return codec;
	}

	/**
	 * Acquire an instance of the decoder based on current song to play.
	 * 
	 * @param songToPlay
	 * @return
	 */
	public static Codec getInstance(Path songToPlay) {
		if (songToPlay.toString().endsWith(flacExt)) {
			return FLACCodec.getInstance();
		} else if (songToPlay.toString().endsWith(mp3Ext)) {
			return MP3Codec.getInstance();
		}
		return codec;
	}

	/**
	 * To see if initialization has completed.
	 * 
	 * @param songToPlay
	 * @return
	 */
	public static boolean isReady(Path songToPlay) {
		if (songToPlay.toString().endsWith(flacExt)) {
			return FLACCodec.isReady();
		} else if (songToPlay.toString().endsWith(mp3Ext)) {
			return MP3Codec.isReady();
		}
		return false;
	}

	/**
	 * Get sample rate in Hz.
	 * 
	 * @param songToPlay
	 * @return
	 */
	public static int getSampleRateInHz(Path songToPlay) {
		if (songToPlay.toString().endsWith(flacExt)) {
			return FLACCodec.getSampleRateInHz();
		} else if (songToPlay.toString().endsWith(mp3Ext)) {
			return MP3Codec.getSampleRateInHz();
		}
		return -1;
	}

	/**
	 * Get bits per sample.
	 * 
	 * @param songToPlay
	 * @return
	 */
	public static int getBitsPerSample(Path songToPlay) {
		if (songToPlay.toString().endsWith(flacExt)) {
			return FLACCodec.getBitsPerSample();
		} else if (songToPlay.toString().endsWith(mp3Ext)) {
			return MP3Codec.getBitsPerSample();
		}
		return 0;
	}

	/**
	 * Get number of channels.
	 * 
	 * @param songToPlay
	 * @return
	 */
	public static int getNumOfChannels(Path songToPlay) {
		if (songToPlay.toString().endsWith(flacExt)) {
			return FLACCodec.getNumOfChannels();
		} else if (songToPlay.toString().endsWith(mp3Ext)) {
			return MP3Codec.getNumOfChannels();
		}
		return -1;
	}

	/**
	 * Get the audio length of current audio stream.
	 * 
	 * @param songToPlay
	 * @return
	 */
	public static double getAudioLength(Path songToPlay) {
		if (songToPlay.toString().endsWith(flacExt)) {
			return FLACCodec.getInstance().getAudioLength();
		} else if (songToPlay.toString().endsWith(mp3Ext)) {
			try {
				return MP3Codec.getInstance().getAudioLength();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return -1;
	}

	/**
	 * A method which is specifically used for dealing with decoding different
	 * formats and write the decoded sample data into the audio device, i.e.,
	 * the SourceDataLine.
	 * 
	 * @param request
	 * @param songToPlay
	 * @return An indicator showing false if the stream has reached the end,
	 *         true otherwise.
	 * @throws IOException
	 * @throws BadFileFormatException
	 * @throws InterruptedException
	 * @throws JavaLayerException
	 * @throws LineUnavailableException
	 */
	public static boolean sampleProcessor(double request, Path songToPlay)
			throws IOException, BadFileFormatException, InterruptedException,
			JavaLayerException, LineUnavailableException {
		if (isFLACEncoded) {
			long[][] samples = null;
			if (request >= 0) {
				long samplePos = Math.round(FLACCodec.getInstance()
						.getTotalSamplesInStream() * request);
				while (FLACCodec.waySearching()) {
					GUISynthesiszer.enablePlaying();
				}
				samples = FLACCodec.getInstance().findNextDecodeableBlock(
						samplePos);
				GUISynthesiszer.getSourceDataLine().flush();
				long clipStartTime = GUISynthesiszer.getSourceDataLine()
						.getMicrosecondPosition()
						- Math.round(1e6 * samplePos
								* GUISynthesiszer.getSampleRateReciprocal());
				GUISynthesiszer.setClipStartTime(clipStartTime);
				GUISynthesiszer.getSourceDataLine().flush();
				GUISynthesiszer.consumeRequest();
				GUISynthesiszer.changeIcon(true);
			} else {
				if (FLACCodec.getInstance().getCurrentStream() == null) {
					logger.log(Level.INFO,
							"flac input stream is null, thread is quitting.");
					return false;
				}
				while (FLACCodec.waySearching()) {
					GUISynthesiszer.enablePlaying();
				}
				Object[] temp = FLACCodec.readNextBlock(false);
				if (temp != null) {
					samples = (long[][]) temp[0];
				}
			}
			if (samples == null)
				return false;
			// convert samples to channel-interleaved bytes in
			// little-endian
			sampleInBytes = new byte[samples.length * samples[0].length
					* FLACCodec.getBitsPerSample() / 8];
			for (int i = 0, k = 0; i < samples[0].length; i++) {
				for (int ch = 0; ch < samples.length; ch++) {
					long cache = samples[ch][i];
					for (int j = 0; j < FLACCodec.getBitsPerSample() / 8; j++, k++)
						sampleInBytes[k] = (byte) (cache >>> (j << 3));
				}
			}
			GUISynthesiszer.getSourceDataLine().write(sampleInBytes, 0,
					sampleInBytes.length);
			return true;
		} else if (isMP3Encoded) {
			if (request >= 0) {
				long clipStartTime = GUISynthesiszer.getSourceDataLine()
						.getMicrosecondPosition()
						- Math.round(MP3Codec.getInstance()
								.readNextDecodebaleFrame(request) * 1e3);
				GUISynthesiszer.setClipStartTime(clipStartTime);
				GUISynthesiszer.getSourceDataLine().flush();
				GUISynthesiszer.consumeRequest();
				GUISynthesiszer.changeIcon(true);
			}
			SampleBuffer output = MP3Codec.getPlayerInstance()
					.getDecodedSamples();
			if (output != null) {
				Player.writeAudioSamples(output,
						GUISynthesiszer.getSourceDataLine());
				return true;
			} else {
				return false;
			}
		} else {
			if (songToPlay.toString().endsWith(flacExt)) {
				if (!isFLACEncoded) {
					isFLACEncoded = !isFLACEncoded;
				}
				if (isMP3Encoded) {
					isMP3Encoded = !isMP3Encoded;
				}
				long[][] samples = null;
				if (request >= 0) {
					long samplePos = Math.round(FLACCodec.getInstance()
							.getTotalSamplesInStream() * request);
					while (FLACCodec.waySearching()) {
						GUISynthesiszer.enablePlaying();
					}
					samples = FLACCodec.getInstance().findNextDecodeableBlock(
							samplePos);
					if (FLACCodec.waySearching()) {
						FLACCodec.disableSearching();
					}
					GUISynthesiszer.getSourceDataLine().flush();
					long clipStartTime = GUISynthesiszer.getSourceDataLine()
							.getMicrosecondPosition()
							- Math.round(1e6 * samplePos
									* GUISynthesiszer.getSampleRateReciprocal());
					GUISynthesiszer.setClipStartTime(clipStartTime);
					GUISynthesiszer.getSourceDataLine().flush();
					GUISynthesiszer.consumeRequest();
					GUISynthesiszer.changeIcon(true);
				} else {
					if (FLACCodec.getInstance().getCurrentStream() == null) {
						logger.log(Level.INFO,
								"flac input stream is null, thread is quitting.");
						return false;
					}
					while (FLACCodec.waySearching()) {
						GUISynthesiszer.enablePlaying();
					}
					Object[] temp = FLACCodec.readNextBlock(false);
					if (temp != null) {
						samples = (long[][]) temp[0];
					}
				}
				if (samples == null)
					return false;
				// convert samples to channel-interleaved bytes in
				// little-endian
				byte[] sampleInBytes = new byte[samples.length
						* samples[0].length * FLACCodec.getBitsPerSample() / 8];
				for (int i = 0, k = 0; i < samples[0].length; i++) {
					for (int ch = 0; ch < samples.length; ch++) {
						long cache = samples[ch][i];
						for (int j = 0; j < FLACCodec.getBitsPerSample() / 8; j++, k++)
							sampleInBytes[k] = (byte) (cache >>> (j << 3));
					}
				}
				GUISynthesiszer.getSourceDataLine().write(sampleInBytes, 0,
						sampleInBytes.length);
				return true;
			} else if (isMP3Encoded || songToPlay.toString().endsWith(mp3Ext)) {
				if (isFLACEncoded) {
					isFLACEncoded = !isFLACEncoded;
				}
				if (!isMP3Encoded) {
					isMP3Encoded = !isMP3Encoded;
				}
				if (request >= 0) {
					long clipStartTime = GUISynthesiszer.getSourceDataLine()
							.getMicrosecondPosition()
							- Math.round(MP3Codec.getInstance()
									.readNextDecodebaleFrame(request) * 1e3);
					GUISynthesiszer.setClipStartTime(clipStartTime);
					GUISynthesiszer.getSourceDataLine().flush();
					GUISynthesiszer.consumeRequest();
					GUISynthesiszer.changeIcon(true);
				}
				Player.writeAudioSamples(MP3Codec.getPlayerInstance()
						.getDecodedSamples(), GUISynthesiszer
						.getSourceDataLine());
			}
		}
		return true;
	}

	/**
	 * Calculate current slider position in respect to maximum audio length.
	 * 
	 * @param songToPlay
	 * @return
	 * @throws IOException
	 */
	public static double getCurrentPosition(Path songToPlay) throws IOException {
		if (isFLACEncoded) {
			return GUISynthesiszer.getCurrentTimeInMicros()
					* GUISynthesiszer.getOneInMillion()
					* FormatParser.getSampleRateInHz(songToPlay)
					* GUISynthesiszer.getTotalSampleReciprocal();
		} else if (isMP3Encoded) {
			return (double) (GUISynthesiszer.getCurrentTimeInMicros() * 1e0 / (MP3Codec
					.getInstance().getAudioLength() * 1e3));
		} else {
			if (songToPlay.toString().endsWith(flacExt)) {
				return GUISynthesiszer.getCurrentTimeInMicros()
						* GUISynthesiszer.getOneInMillion()
						* FormatParser.getSampleRateInHz(songToPlay)
						* GUISynthesiszer.getTotalSampleReciprocal();
			} else if (songToPlay.toString().endsWith(mp3Ext)) {
				return (double) (GUISynthesiszer.getCurrentTimeInMicros() * 1e0 / (MP3Codec
						.getInstance().getAudioLength() * 1e3));
			}
		}
		return -1;
	}
}
