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

package com.mp3.codec.player;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;

import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.UnsupportedAudioFileException;
import javax.swing.JOptionPane;

import com.mp3.codec.decoder.BitstreamException;
import com.mp3.codec.decoder.Header;
import com.mp3.codec.decoder.JavaLayerException;

/**
 * The <code>MP3Codec</code> class implements a simple command-line player for
 * MPEG audio files.
 *
 * @author Mat McGowan (mdm@techie.com)
 */
public class Accessor {

	private boolean remote = false;
	private static Accessor mp3 = null;
	private Player player = null;
	private RandomAccessFile fileAccessor = null;
	private BufferedInputStream in;
	private Header header = null;

	/**
	 * Create An instance of MP3 in singleton pattern.
	 * 
	 * @param songPath
	 * @return an instance of MP3, namely mp3Codec.
	 * @throws IOException
	 * @throws JavaLayerException
	 */
	public static Accessor createInstance(Path filePath) throws IOException,
			JavaLayerException {
		if (mp3 == null) {
			mp3 = new Accessor();
		}
		return mp3.initialize(filePath);
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

	public Player getPlayerInstance() {
		return mp3.player;
	}

	/**
	 * Cause current active SourceDataLine and input stream to close.
	 * 
	 * @throws BitstreamException
	 */

	public void closeStreaming() throws BitstreamException {
		Player.closeStreaming();
	}

	/**
	 * Decode a single frame header and initialize the SourceDataLine.
	 * 
	 * @return
	 * @throws BitstreamException
	 * @throws LineUnavailableException
	 */

	public Boolean decodeFrameHeader() throws BitstreamException,
			LineUnavailableException {
		return Player.decodeFrameHeander();
	}

	private Accessor() {
	}

	/**
	 * Initialize attributes of mp3Codec if it is not null.
	 */
	private Accessor initialize(Path filePath) {
		if (!mp3.parsePath(filePath))
			mp3 = null;
		else {
			try {
				if (mp3.remote == true)
					mp3.getURLInputStream(filePath);
				else {
					mp3.initializeByteStreams(filePath);
					mp3.player = new Player(mp3.in);
				}
			} catch (IOException | JavaLayerException e) {
				e.printStackTrace();
			}
		}
		return mp3;
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
	public float getTotoalDuration() throws IOException {
		mp3.getHeader();
		return (float) (1e3 * mp3.fileAccessor.length() / (mp3.header.bitrate() / 8));
	}

	/**
	 * set the filePath as given file path if file exists.
	 * 
	 * @param path
	 * @return
	 */
	protected boolean parsePath(Path path) {
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
		System.out.println(" e.g. : java javazoom.jl.player.jlp localfile.mp3");
		System.out
				.println("        java javazoom.jl.player.jlp -url http://www.server.com/remotefile.mp3");
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
	protected void getURLInputStream(Path filePath) throws IOException {

		URL url = new URL(filePath.toAbsolutePath().toString());
		InputStream fin = url.openStream();
		in = new BufferedInputStream(fin);
	}

	/**
	 * Initialize byte streams from given file path.
	 */
	protected void initializeByteStreams(Path filePath) throws IOException {
		if (mp3.fileAccessor != null) {
			mp3.fileAccessor.close();
			mp3.fileAccessor = null;
		}
		// Construct a new input stream based on the RandomAccessFile in respect
		// to given file path.
		mp3.fileAccessor = new RandomAccessFile(filePath.toFile(), "r");
		mp3.in = new BufferedInputStream(new InputStream() {

			@Override
			public int read() throws IOException {
				return mp3.fileAccessor.read();
			}

			@Override
			public int read(byte[] b, int off, int len) throws IOException {
				return mp3.fileAccessor.read(b, off, len);
			}
		});
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
	private void seekTo(long pos) throws IOException, JavaLayerException {
		if (mp3 == null || fileAccessor == null) {
			return;
		}
		mp3.fileAccessor.seek(pos);
		in = new BufferedInputStream(new InputStream() {

			@Override
			public int read() throws IOException {
				// TODO Auto-generated method stub
				return mp3.fileAccessor.read();
			}

			@Override
			public int read(byte[] b, int off, int len) throws IOException {
				return mp3.fileAccessor.read(b, off, len);
			}
		});
		mp3.player = new Player(in);
	}

	/**
	 * Initialize current Header instance with that of Class Player's.
	 * 
	 * @throws IOException
	 */
	private void getHeader() throws IOException {
		mp3.header = Player.getHeader();
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
		if (mp3 == null) {
			return 0;
		}
		mp3.getHeader();
		long filePos = Math.round(mp3.header.bitrate() / 8 * request
				* mp3.getTotoalDuration() / 1e3);
		mp3.seekTo(filePos);
		Player.closeBitStream();
		Player.decodeFrame();
		return mp3.fileAccessor.getFilePointer() * 1e3
				/ (mp3.header.bitrate() / 8) - mp3.header.ms_per_frame() * 5;
	}
}
