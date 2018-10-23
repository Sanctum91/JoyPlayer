/*
 * 11/19/04		1.0 moved to LGPL.
 * 
 * 06/04/01		Streaming support added. javalayer@javazoom.net
 * 
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

package com.codec.player;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;

import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.UnsupportedAudioFileException;
import javax.swing.JOptionPane;

import com.audioplayer.GraphAndSound.Codec;
import com.mp3.codec.decoder.BitstreamException;
import com.mp3.codec.decoder.Header;
import com.mp3.codec.decoder.JavaLayerException;

/**
 * The <code>MP3Codec</code> class implements a simple command-line player for
 * MPEG audio files.
 *
 * @author Mat McGowan (mdm@techie.com)
 */
public final class MP3Codec extends Codec {

	private static Player player = null;
	private static Header header = null;
	private static boolean remote;
	private final static int bitsPerSample = 16;

	/**
	 * Create An instance of MP3 in singleton pattern.
	 * 
	 * @param songPath
	 * @return an instance of MP3, namely codecCodec.
	 * @throws IOException
	 */
	public static MP3Codec createInstance(Path filePath) throws IOException {
		isReady = false;
		if (codec == null) {
			try {
				codec = new MP3Codec(filePath);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		return (MP3Codec) initialize(filePath);
	}

	private MP3Codec(Path filePath) throws IOException, InterruptedException {
		super(filePath);
	}

	public static MP3Codec getInstance() {
		return (MP3Codec) codec;
	}

	/**
	 * get sample rate
	 * 
	 * @return
	 */
	public static int getSampleRateInHz() {
		if (header == null) {
			try {
				setHeader();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return header.frequency();
	}

	public static int getBitsPerSample() {
		return bitsPerSample;
	}

	public static int getNumOfChannels() {
		if (header == null) {
			try {
				setHeader();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return header.mode() == 3 ? 1 : 2;
	}

	/**
	 * Get current position in audio data, in milliseconds.
	 * 
	 * @return
	 */

	public long getPosition() {
		return Player.getCurrentPosition();
	}

	/**
	 * Get current Player instance of MP3 instance.
	 * 
	 * @return
	 */

	public static Player getPlayerInstance() {
		return player;
	}

	/**
	 * Cause current active SourceDataLine and input stream to close.
	 * 
	 * @throws BitstreamException
	 */

	public static void closeFile() {
		if (codec != null) {
			codec = null;
		}
		try {
			Player.closeFile();
		} catch (BitstreamException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Decode a single frame header and initialize the SourceDataLine.
	 * 
	 * @return
	 * @throws BitstreamException
	 * @throws LineUnavailableException
	 */

	public static Boolean decodeFrameHeader() throws BitstreamException,
			LineUnavailableException {
		return Player.decodeFrameHeander();
	}

	/**
	 * Initialize attributes of codecCodec if it is not null.
	 */
	public static MP3Codec initialize(Path filePath) {
		if (!parsePath(filePath))
			codec = null;
		else {
			try {
				if (remote == true)
					getURLInputStream(filePath);
				else {
					if (player != null) {
						player = null;
					}
					player = new Player(byteBuffer);
					while (!Player.isReady()) {
					}
					if (decodeFrameHeader()) {
						isReady = true;
					}
				}
			} catch (JavaLayerException | LineUnavailableException
					| IOException e) {
				e.printStackTrace();
			}
		}
		return (MP3Codec) codec;
	}

	/**
	 * Get total duration of current audio data if song path exists, measured in
	 * milliseconds.
	 * 
	 * @param songPath
	 * @return the total duration
	 * @throws IOException
	 * @throws UnsupportedAudioFileException
	 */
	@Override
	public double getAudioLength() throws IOException {
		setHeader();
		return header.total_ms((int) raf.length());
	}

	/**
	 * set the filePath as given file path if file exists.
	 * 
	 * @param path
	 * @return
	 */
	public static boolean parsePath(Path path) {
		boolean parsed = false;
		if (Files.exists(path)) {
			parsed = true;
			remote = false;
		} else {
			JOptionPane.showMessageDialog(null, "File doesn't exist!",
					"I/O Error(s) Occurred!", JOptionPane.ERROR_MESSAGE);
			// showUsage();
		}
		return parsed;
	}

	public void showUsage() {
		System.out.println("Usage: jlp [-url] <filename>");
		System.out.println("");
		System.out
				.println(" e.g. : java javazoom.jl.player.jlp localfile.codec");
		System.out
				.println("        java javazoom.jl.player.jlp -url http://www.server.com/remotefile.codec");
		System.out
				.println("        java javazoom.jl.player.jlp -url http://www.shoutcastserver.com:8000");
	}

	/**
	 * Decode and play next single frame.
	 * 
	 * @return
	 * @throws JavaLayerException
	 * @throws LineUnavailableException
	 */
	public Boolean play() throws JavaLayerException, LineUnavailableException {
		return Player.decodeFrame();
	}

	/**
	 * Playing file from URL (Streaming).
	 * 
	 * @throws IOException
	 */
	protected static void getURLInputStream(Path filePath) throws IOException {

		URL url = new URL(filePath.toAbsolutePath().toString());
		InputStream fin = url.openStream();
		byteBuffer = new BufferedInputStream(fin);
	}

	protected AudioDevice getAudioDevice() throws JavaLayerException {
		return FactoryRegistry.systemRegistry().createAudioDevice();
	}

	/**
	 * Seek to given byte position, and initialize input stream
	 * 
	 * @param pos
	 * @throws IOException
	 * @throws JavaLayerException
	 */
	public void seekTo(long pos) throws IOException, JavaLayerException {
		if (codec == null || raf == null) {
			return;
		}
		raf.seek(pos);
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
		player = new Player(byteBuffer);
	}

	/**
	 * Initialize current Header instance with that of Class Player's.
	 * 
	 * @throws IOException
	 */
	private static void setHeader() throws IOException {
		header = Player.getHeader();
	}

	/**
	 * Seek to the requested position and return the approximate audio position,
	 * measured in milliseconds.
	 * 
	 * @param request
	 * @return
	 * @throws IOException
	 * @throws JavaLayerException
	 * @throws LineUnavailableException
	 */
	public double readNextDecodebaleFrame(double request) throws IOException,
			JavaLayerException, LineUnavailableException {
		if (codec == null) {
			return 0;
		}
		setHeader();
		long filePos = Math.round(header.bitrate() / 8 * request
				* codec.getAudioLength() / 1e3);
		seekTo(filePos);
		Player.closeBitStream();
		Player.decodeFrame();
		return raf.getFilePointer() * 1e3 / (header.bitrate() / 8)
				- header.ms_per_frame() * 5;
	}

}
