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

import javax.sound.sampled.LineUnavailableException;

import com.audioplayer.GraphAndSound.Decoder.BadFileFormatException;
import com.codec.player.MP3Decoder;
import com.flac.decoder.FLACDecoder;
import com.mp3.decoder.JavaLayerException;

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
	private static Decoder decoding = Decoder.getInstance();
	private static boolean isFLACEncoded;
	private static boolean isMP3Encoded;

	/**
	 * Terminate decoding process.
	 * 
	 * @param songToPlay
	 */
	public static void closeFile() {
		if (isFLACEncoded) {
			isFLACEncoded = !isFLACEncoded;
		}
		if (isMP3Encoded) {
			isMP3Encoded = !isMP3Encoded;
		}
		try {
			Decoder.close();
		} catch (IOException e) {
			e.printStackTrace();
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
	public static Decoder createInstance(Path songToPlay) throws IOException,
			BadFileFormatException, JavaLayerException {
		try {
			Thread.sleep(100);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		getLinkedDecoder(songToPlay);
		decoding = Decoder.getInstance();
		return decoding;
	}

	/**
	 * Acquire an instance of the decoder based on current song to play.
	 * 
	 * @param songToPlay
	 * @return
	 */
	public static Decoder getInstance() {
		decoding = Decoder.getInstance();
		return decoding;
	}

	/**
	 * To see if initialization has completed.
	 * 
	 * @param songToPlay
	 * @return
	 */
	public static boolean isReady() {
		try {
			return Decoder.isReady();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		return false;
	}

	/**
	 * Get sample rate in Hz.
	 * 
	 * @param songToPlay
	 * @return
	 */
	public static int getSampleRateInHz() {
		return decoding.getSampleRateInHz();
	}

	/**
	 * Get bits per sample.
	 * 
	 * @param songToPlay
	 * @return
	 */
	public static int getBitsPerSample() {
		return decoding.getBitsPerSample();
	}

	/**
	 * Get number of channels.
	 * 
	 * @param songToPlay
	 * @return
	 */
	public static int getNumOfChannels() {
		return decoding.getNumOfChannels();
	}

	/**
	 * Get the audio length of current audio stream.
	 * 
	 * @param songToPlay
	 * @return
	 */
	public static double getAudioLength() {
		try {
			return decoding.getAudioLength();
		} catch (IOException e) {
			e.printStackTrace();
			return 0;
		}
	}

	/**
	 * Get the linked decoder based on the file extension of given file path.
	 * 
	 * @param songToPlay
	 * @return file extension in type String
	 */
	public static void getLinkedDecoder(Path filePath) {
		int idx = filePath.toString().lastIndexOf('.');
		final String extension = filePath.toString().substring(idx);
		if (extension.equals(FormatExtension.FLAC.getExtension())) {
			FLACDecoder.createInstance(filePath);
		} else if (extension.equals(FormatExtension.MP3.getExtension())) {
			MP3Decoder.createInstance(filePath);
		}
	}

	/**
	 * A method which is specifically used for dealing with decoding different
	 * formats and write the decoded sample data into the audio device, i.e.,
	 * the SourceDataLine.
	 * 
	 * @param request
	 * @return
	 */
	public static boolean sampleProcessor(double request) {
		try {
			return decoding.sampleProcessor(request);
		} catch (IOException | BadFileFormatException
				| LineUnavailableException | InterruptedException
				| JavaLayerException e) {
			e.printStackTrace();
			return false;
		}
	}

	/**
	 * Calculate current slider position in respect to maximum audio length.
	 * 
	 * @return
	 */
	public static double getCurrentPosition() {
		try {
			return decoding.getCurrentPosition();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return 0;
	}

}
