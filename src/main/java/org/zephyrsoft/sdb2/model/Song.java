/*
 * This file is part of the Song Database (SDB).
 * 
 * SDB is free software: you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version.
 * 
 * SDB is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with SDB. If not, see <http://www.gnu.org/licenses/>.
 */
package org.zephyrsoft.sdb2.model;

import java.io.Serializable;

import org.zephyrsoft.util.StringTools;

import com.thoughtworks.xstream.annotations.XStreamAlias;

/**
 * Representation of a song.
 * 
 * @author Mathis Dirksen-Thedens
 */
@XStreamAlias("song")
public class Song implements Serializable, Comparable<Song> {
	
	private static final long serialVersionUID = -7133402923581521674L;
	
	private String title;
	private String composer;
	private String authorText;
	private String authorTranslation;
	private String publisher;
	private String additionalCopyrightNotes;
	private LanguageEnum language;
	private String songNotes;
	private String tonality;
	private String uuid;
	private String chordSequence;
	private String lyrics;
	
	/**
	 * Create a song instance. By making this the only constructor, everyone (who doesn't use reflection) has to provide
	 * a UUID.
	 * 
	 * @param uuid
	 *            a UUID for this song
	 * @see StringTools#createUUID()
	 */
	public Song(String uuid) {
		this.uuid = uuid;
	}
	
	public String getTitle() {
		return title;
	}
	
	public String getComposer() {
		return composer;
	}
	
	public String getAuthorText() {
		return authorText;
	}
	
	public String getAuthorTranslation() {
		return authorTranslation;
	}
	
	public String getPublisher() {
		return publisher;
	}
	
	public String getAdditionalCopyrightNotes() {
		return additionalCopyrightNotes;
	}
	
	public LanguageEnum getLanguage() {
		return language;
	}
	
	public String getSongNotes() {
		return songNotes;
	}
	
	public String getLyrics() {
		return lyrics;
	}
	
	public String getTonality() {
		return tonality;
	}
	
	public String getChordSequence() {
		return chordSequence;
	}
	
	public void setTitle(String title) {
		this.title = title;
	}
	
	public void setComposer(String composer) {
		this.composer = composer;
	}
	
	public void setAuthorText(String authorText) {
		this.authorText = authorText;
	}
	
	public void setAuthorTranslation(String authorTranslation) {
		this.authorTranslation = authorTranslation;
	}
	
	public void setPublisher(String publisher) {
		this.publisher = publisher;
	}
	
	public void setAdditionalCopyrightNotes(String additionalCopyrightNotes) {
		this.additionalCopyrightNotes = additionalCopyrightNotes;
	}
	
	public void setLanguage(LanguageEnum language) {
		this.language = language;
	}
	
	public void setSongNotes(String songNotes) {
		this.songNotes = songNotes;
	}
	
	public void setLyrics(String lyrics) {
		this.lyrics = lyrics;
	}
	
	public void setTonality(String tonality) {
		this.tonality = tonality;
	}
	
	public void setChordSequence(String chordSequence) {
		this.chordSequence = chordSequence;
	}
	
	public String getUUID() {
		return uuid;
	}
	
	@Override
	public int compareTo(Song o) {
		int ret = 0;
		
		ret = StringTools.compareLocaleBasedWithNullFirst(getTitle(), o.getTitle());
		if (ret != 0) {
			return ret;
		}
		
		ret = StringTools.compareLocaleBasedWithNullFirst(getLyrics(), o.getLyrics());
		if (ret != 0) {
			return ret;
		}
		
		ret = StringTools.compareLocaleBasedWithNullFirst(getChordSequence(), o.getChordSequence());
		if (ret != 0) {
			return ret;
		}
		
		return ret;
	}
	
	@Override
	public String toString() {
		return "SONG[" + title + "|" + uuid + "]";
	}
	
}
