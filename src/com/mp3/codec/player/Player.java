/*
 * 11/19/04		1.0 moved to LGPL.
 * 29/01/00		Initial version. mdm@techie.com
 *-----------------------------------------------------------------------
 *   This program is free software; you can redistribute it and/or modify
 *   it under the terms of the GNU Library General Public License as published
 *   by the Free Software Foundation; either version 2 of the License, or
 *   (at your option) any later version.
 *
 *   This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU Library General Public License for more details.
 *
 *   You should have received a copy of the GNU Library General Public
 *   License along with this program; if not, write to the Free Software
 *   Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 *----------------------------------------------------------------------
 */

package com.mp3.codec.player;

import java.io.InputStream;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;

import com.mp3.codec.decoder.Bitstream;
import com.mp3.codec.decoder.BitstreamException;
import com.mp3.codec.decoder.Decoder;
import com.mp3.codec.decoder.DecoderException;
import com.mp3.codec.decoder.Header;
import com.mp3.codec.decoder.JavaLayerException;
import com.mp3.codec.decoder.SampleBuffer;

/**
 * The <code>Player</code> class implements a simple player for playback of an
 * MPEG audio stream.
 * 
 * @author Mat McGowan
 * @since 0.0.8
 */

// REVIEW: the audio device should not be opened until the
// first MPEG audio frame has been decoded.
public class Player {

	/**
	 * The MPEG audio bitstream.
	 */
	// javac blank final bug.
	/* final */private static Bitstream bitstream = null;

	/**
	 * The MPEG audio decoder.
	 */
	/* final */private static Decoder decoder;

	/**
	 * The AudioDevice the audio samples are written to.
	 */
	private static AudioDevice audio = null;

	/**
	 * Has the player been closed?
	 */
	private boolean closed = false;

	/**
	 * Has the player played back all frames from the stream?
	 */
	private boolean complete = false;

	private static long lastPosition = 0;

	private static Header h = null;

	private static SourceDataLine line = null;

	private static byte[] samples = null;

	private static JavaSoundAudioDevice sound = null;

	private static SampleBuffer output = null;

	/**
	 * Creates a new <code>Player</code> instance.
	 */
	public Player(InputStream stream) throws JavaLayerException {
		this(stream, null);
	}

	public Player(InputStream stream, AudioDevice device)
			throws JavaLayerException {
		if (bitstream != null) {
			bitstream = null;
		}
		if (audio != null) {
			audio = null;
		}
		bitstream = new Bitstream(stream);
		decoder = new Decoder();

		if (device != null) {
			audio = device;
		} else {
			FactoryRegistry r = FactoryRegistry.systemRegistry();
			audio = r.createAudioDevice();
		}
		audio.open(decoder);
		sound = (JavaSoundAudioDevice) audio;
	}

	public void play() throws JavaLayerException, LineUnavailableException {
		play(Integer.MAX_VALUE);
	}

	/**
	 * Plays a number of MPEG audio frames.
	 * 
	 * @param frames
	 *            The number of frames to play.
	 * @return true if the last frame was played, or false if there are more
	 *         frames.
	 * @throws LineUnavailableException
	 */
	public boolean play(int frames) throws JavaLayerException,
			LineUnavailableException {
		boolean ret = true;

		while (frames-- > 0 && ret) {
			ret = decodeFrame();
		}

		if (!ret) {
			// last frame, ensure all data flushed to the audio device.
			AudioDevice out = audio;
			if (out != null) {
				out.flush();
				synchronized (this) {
					complete = (!closed);
					close();
				}
			}
		}
		return ret;
	}

	/**
	 * Get total duration of current audio data in milliseconds.
	 * 
	 * @param streamsize
	 *            in bytes
	 * @return
	 */
	public float getTotalDuration(int streamsize) {
		return bitstream.getAudioDuration(streamsize);
	}

	/**
	 * Get current JavaSoundAudioDevice instance.
	 */
	protected JavaSoundAudioDevice getJavaSoundAudioDevice() {
		return sound;
	}

	/**
	 * Get SourceDataLine instance
	 */
	protected SourceDataLine getSourceDataLine() {
		return line;
	}

	/**
	 * Closes this player. Any audio currently playing is stopped immediately.
	 */
	public synchronized void close() {
		AudioDevice out = audio;
		if (out != null) {
			closed = true;
			audio = null;
			// this may fail, so ensure object state is set up before
			// calling this method.
			out.close();
			lastPosition = out.getPosition();
			try {
				bitstream.close();
			} catch (BitstreamException ex) {
			}
		}
	}

	/**
	 * Returns the completed status of this player.
	 * 
	 * @return true if all available MPEG audio frames have been decoded, or
	 *         false otherwise.
	 */
	public synchronized boolean isComplete() {
		return complete;
	}

	/**
	 * Retrieves the position in microseconds of the current audio sample being
	 * played. This method delegates to the <code>
	 * AudioDevice</code> that is used by this player to sound the decoded audio
	 * samples.
	 */
	public int getPosition() {
		int position = (int) lastPosition / 1000;

		AudioDevice out = audio;
		if (out != null) {
			position = (int) out.getPosition();
		}
		return position;
	}

	/**
	 * Get the current position in audio data of SourceDataLine instance.
	 * 
	 * @return
	 */
	public static long getCurrentPosition() {
		lastPosition = 0;
		if (line != null) {
			lastPosition = line.getMicrosecondPosition();
		}
		return lastPosition;
	}

	/**
	 * Close current input stream and SourceDataLine.
	 * 
	 * @throws BitstreamException
	 */
	public static void closeStreaming() throws BitstreamException {
		if (line != null) {
			line.close();
			line = null;
		}
		if (bitstream != null) {
			bitstream.close();
			bitstream = null;
		}
		if (line == null && bitstream == null) {
			Logger.getGlobal().log(Level.INFO,
					"mp3 streaming has closed successfully.");
		} else {
			Logger.getGlobal().log(Level.INFO,
					"mp3 streaming has failed to close.");
		}
	}

	/**
	 * Decode one single frame header and initialize SourceDataLine if header is
	 * valid.
	 * 
	 * @return true if decoding and initializing is successful, false otherwise.
	 * @throws LineUnavailableException
	 * @throws BitstreamException
	 */
	public static Boolean decodeFrameHeander() throws LineUnavailableException,
			BitstreamException {
		try {
			h = bitstream.readFrame();

			bitstream.unreadFrame();

			bitstream.closeFrame();
			if (h == null) {
				Logger.getGlobal()
						.log(Level.WARNING, "No more frames to read!");
				bitstream.close();
				Logger.getGlobal().log(Level.WARNING, "mp3 stream closed!");
				return false;
			}
		} catch (BitstreamException e) {
			Logger.getGlobal().log(Level.WARNING, "No more frames to read!");
			bitstream.close();
			Logger.getGlobal().log(Level.WARNING, "mp3 stream closed!");
			return false;
		}
		if (line == null) {
			Logger.getGlobal().log(Level.INFO, "Initializing line.");
			sourceDataLineInitializer(h.frequency(), 16, h.mode() == 3 ? 1 : 2,
					true, false);
			if (line != null) {
				Logger.getGlobal().log(Level.INFO,
						"Line initialized successfully.");
			} else {
				Logger.getGlobal()
						.log(Level.INFO, "Line failed to initialize.");
			}
		}
		return true;
	}

	/**
	 * Get current Header instance.
	 * 
	 * @return Header instance.
	 */
	protected static Header getHeader() {
		return h;
	}

	/**
	 * Decode and play one single frame.
	 * 
	 * @return true if there are no more frames to decode, false otherwise.
	 * @throws LineUnavailableException
	 */
	protected static boolean decodeFrame() throws JavaLayerException,
			LineUnavailableException {
		try {
			h = bitstream.readFrame();

			if (h == null) {
				bitstream.close();
				bitstream = null;
				Logger.getGlobal().log(Level.WARNING, "mp3 stream has closed!");
				return false;
			}

			// sample buffer set when decoder constructed
			output = (SampleBuffer) decoder.decodeFrame(h, bitstream);
			samples = sound.toByteArray(output.getBuffer(), 0,
					output.getBufferLength());
			if (line != null) {
				line.write(samples, 0, output.getBufferLength() * 2);
			} else {
				Logger.getGlobal().log(Level.WARNING,
						"Line currently unavailable!");
			}

			bitstream.closeFrame();
		} catch (BitstreamException ex) {
			Logger.getGlobal().log(Level.WARNING, "No more frames to read!");
			bitstream.closeFrame();
			bitstream.close();
			bitstream = null;
			Logger.getGlobal().log(Level.WARNING, "mp3 stream has closed!");
			return false;
		}
		/*
		 * catch (IOException ex) {
		 * System.out.println("exception decoding audio frame: "+ex); return
		 * false; } catch (BitstreamException bitex) {
		 * System.out.println("exception decoding audio frame: "+bitex); return
		 * false; } catch (DecoderException decex) {
		 * System.out.println("exception decoding audio frame: "+decex); return
		 * false; }
		 */
		return true;
	}

	/**
	 * Get decoded SampleBuffer data from decoding one single frame.
	 * 
	 * @return
	 * @throws DecoderException
	 * @throws BitstreamException
	 */
	protected SampleBuffer getSampleBuffer() throws DecoderException,
			BitstreamException {
		h = bitstream.readFrame();
		if (h == null) {
			Logger.getGlobal().log(Level.WARNING, "No more frames to read!");
			bitstream.close();
			bitstream = null;
			Logger.getGlobal().log(Level.WARNING, "mp3 stream has closed!");
			return null;
		}
		output = (SampleBuffer) decoder.decodeFrame(h, bitstream);
		bitstream.closeFrame();
		// sample buffer set when decoder constructed
		return output;

	}

	/**
	 * Initialize current SourceDataLine instance if given parameters are valid.
	 * 
	 * @param frequency
	 * @param sampleDepth
	 * @param channels
	 * @param signed
	 * @param bigEndian
	 * @throws LineUnavailableException
	 */
	protected static void sourceDataLineInitializer(int frequency,
			int sampleDepth, int channels, Boolean signed, Boolean bigEndian)
			throws LineUnavailableException {
		AudioFormat format = new AudioFormat(frequency, sampleDepth, channels,
				signed, bigEndian);
		line = (SourceDataLine) AudioSystem.getLine(new DataLine.Info(
				SourceDataLine.class, format));
		line.open(format);
		line.start();
	}

	public static void closeBitStream() {
		bitstream.closeFrame();
	}

}
