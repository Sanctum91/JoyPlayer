package com.audioplayer.GraphAndSound;

public enum FormatExtension {
	MP3(".mp3"), FLAC(".flac"), LRC(".lrc");

	private FormatExtension(String ext) {
		this.ext = ext;
	}

	public String getExtension() {
		return this.ext;
	}

	private final String ext;
}
