package com.googlecode.loveemu.petitemm;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class MMLNoteConverter {
	
	/**
	 * Constant number for rest.
	 */
	public static final int KEY_REST = MMLNoteInfo.KEY_REST;
	
	/**
	 * Constant number for ties.
	 */
	public static final int KEY_TIE = MMLNoteInfo.KEY_TIE;
	
	/**
	 * Tick to MML note conversion table.
	 */
	private MMLNoteInfo[] notes;
	
	/**
	 * Tick to premitive note lengths table.
	 */
	private List<List<Integer>> noteLengths;
	
	/**
	 * Tick to premitive note lengths table. (dotted notes disassembled)
	 */
	private List<List<Integer>> noteLengthsDotsDisassembled;
	
	/**
	 * Ticks per quarter note of MML.
	 */
	private int tpqn;
	
	/**
	 * Maximum dot count used in note.
	 */
	private int maxDotCount;
	
	/**
	 * MML symbol set.
	 */
	private MMLSymbol mmlSymbol;
	
	/**
	 * True if MML tick notation should be used.
	 */
	private boolean useTicks;
	
	/**
	 * Construct new MML note converter.
	 * 
	 * @param tpqn Tick per quarter note of MML.
	 */
	public MMLNoteConverter(MMLSymbol mmlSymbol, int tpqn, boolean useTicks) {
		this(mmlSymbol, tpqn, -1, useTicks);
	}
	
	/**
	 * Construct new MML note converter.
	 * 
	 * @param tpqn        Tick per quarter note of MML.
	 * @param maxDotCount Maximum count of dots of dotted-note allowed.
	 */
	public MMLNoteConverter(MMLSymbol mmlSymbol, int tpqn, int maxDotCount, boolean useTicks) {
		this.mmlSymbol = mmlSymbol;
		this.useTicks = useTicks;
		setTPQN(tpqn);
		initNoteTable(tpqn, maxDotCount);
	}
	
	/**
	 * Get the MML text of a note.
	 * 
	 * @param length Note length in tick(s).
	 * @return MML text for the note. (octave will not be included)
	 */
	public String getNote(int length) {
		if(length < 0) {
			throw new IllegalArgumentException("Note length is negative.");
		}
		
		StringBuilder sb = new StringBuilder();
		
		if(useTicks) {
			int ticks = Midi2MML.MML_TPQN * length / tpqn;
			sb.append("$N");
			sb.append(mmlSymbol.getTicks());
			sb.append(ticks);
			MMLNoteInfo note = new MMLNoteInfo(mmlSymbol, sb.toString());
			return note.getText();
		} else {
			int len = length;
			while(len > (tpqn * 8)) {
				sb.append(notes[tpqn * 8].getText());
				sb.append(mmlSymbol.getTie());
				len -= tpqn * 8;
			}
			sb.append(notes[len].getText());
			return sb.toString();
		}
	}
	
	/**
	 * Get the MML text of a note.
	 * 
	 * @param length Note length in tick(s).
	 * @param key    Key of note, specify KEY_REST for a rest.
	 * @return MML text for the note. (octave will not be included)
	 */
	public String getNote(int length, int key) {
		if(length < 0) {
			throw new IllegalArgumentException("Note length must be a positive number.");
		}
		
		StringBuilder sb = new StringBuilder();
		
		if(useTicks) {
			int ticks = Midi2MML.MML_TPQN * length / tpqn;
			sb.append("$N");
			sb.append(mmlSymbol.getTicks());
			sb.append(ticks);
			MMLNoteInfo note = new MMLNoteInfo(mmlSymbol, sb.toString());
			return note.getText(key);
		} else {
			int len = length;
			while(len > (tpqn * 8)) {
				sb.append(notes[tpqn * 8].getText(key));
				sb.append(mmlSymbol.getTie());
				len -= tpqn * 8;
			}
			sb.append(notes[len].getText(key));
			return sb.toString();
		}
	}
	
	/**
	 * Get list of notes which are necessary to express a certain length note.
	 * 
	 * @param length           Length of note to be expressed.
	 * @param dotsDisassembled true if dotted note must be expressed by two or more
	 *                         elements.
	 * @return List of note lengths.
	 */
	public List<Integer> getPrimitiveNoteLengths(int length, boolean dotsDisassembled) {
		List<Integer> lengths = new ArrayList<>();
		
		if(length < 0)
			throw new IllegalArgumentException("Note length must be a positive number.");
		else if(length == 0) {
			return lengths; // blank list
		}
		
		int len = length;
		
		List<List<Integer>> targetNoteLengths;
		if(dotsDisassembled)
			targetNoteLengths = noteLengthsDotsDisassembled;
		else
			targetNoteLengths = noteLengths;
		
		// construct the final length list
		while(len > (tpqn * 8)) {
			lengths.addAll(targetNoteLengths.get(tpqn * 8));
			len -= tpqn * 8;
		}
		lengths.addAll(targetNoteLengths.get(len));
		return lengths;
	}
	
	/**
	 * Get maximum dot count used in note.
	 * 
	 * @return Number of dots.
	 */
	public int getMaxDotCount() {
		return maxDotCount;
	}
	
	/**
	 * Set maximum dot count used in note.
	 * 
	 * @param maxDotCount Number of dots.
	 */
	private void setMaxDotCount(int maxDotCount) {
		this.maxDotCount = maxDotCount;
	}
	
	/**
	 * Get if the given note is a simple note.
	 * 
	 * @param length Note length in tick(s).
	 * @return true if the note is simple enough, false otherwise.
	 */
	public boolean isSimpleNote(int length) {
		if(length < 0)
			throw new IllegalArgumentException("Note length must be a positive number.");
		else if(length == 0)
			return true;
		
		List<Integer> lengths = noteLengths.get(length % (tpqn * 4));
		return (lengths.size() <= 1);
	}
	
	/**
	 * Get timebase of MML.
	 * 
	 * @return Ticks per quarter note.
	 */
	public int getTPQN() {
		return tpqn;
	}
	
	/**
	 * Set timebase of MML.
	 * 
	 * @param tpqn Ticks per quarter note.
	 */
	public void setTPQN(int tpqn) {
		if(tpqn < 0) {
			throw new IllegalArgumentException("TPQN is negative.");
		}
		this.tpqn = tpqn;
	}
	
	/**
	 * Initialize the table for note conversion.
	 * 
	 * @param tpqn        Tick per quarter note of MML.
	 * @param maxDotCount Maximum count of dots of dotted-note allowed.
	 */
	private void initNoteTable(int tpqn, int maxDotCount) {
		int tick;
		
		if(tpqn < 0) {
			throw new IllegalArgumentException("TPQN is negative.");
		}
		
		// construct the note table
		notes = new MMLNoteInfo[tpqn * 8 + 1];
		notes[0] = new MMLNoteInfo(mmlSymbol, "");
		
		// initialize length table
		List<List<Integer>> singleNoteLengths = new ArrayList<>(tpqn * 8 + 1);
		noteLengths = new ArrayList<>(tpqn * 8 + 1);
		noteLengthsDotsDisassembled = new ArrayList<>(tpqn * 8 + 1);
		for(int mmlNoteLen = 0; mmlNoteLen <= (tpqn * 8); mmlNoteLen++) {
			singleNoteLengths.add(null);
			noteLengths.add(null);
			noteLengthsDotsDisassembled.add(null);
		}
		
		// set single notes
		int maxDotCountUsed = 0;
		MMLNoteInfo[] singleNotes = new MMLNoteInfo[tpqn * 8 + 1];
		for(int mmlNoteLen = 1; mmlNoteLen <= (tpqn * 4); mmlNoteLen++) {
			if((tpqn * 4) % mmlNoteLen != 0) {
				continue;
			}
			
			// simple note
			tick = (tpqn * 4) / mmlNoteLen;
			
			// create length table
			List<Integer> simpleNoteLength = new ArrayList<>();
			simpleNoteLength.add(tick);
			
			// add new note
			notes[tick] = new MMLNoteInfo(mmlSymbol, "$N" + mmlNoteLen);
			noteLengths.set(tick, simpleNoteLength);
			noteLengthsDotsDisassembled.set(tick, simpleNoteLength);
			singleNotes[tick] = notes[tick];
			singleNoteLengths.set(tick, simpleNoteLength);
			
			// dotted notes
			int dot = 1;
			int baseNoteTick = tick;
			StringBuilder mml = new StringBuilder(notes[tick].getText());
			List<Integer> dottedNoteLengthDotsDisassembled = new ArrayList<>();
			dottedNoteLengthDotsDisassembled.add(baseNoteTick);
			while(baseNoteTick % (1 << dot) == 0) {
				// limit the maximum dot count
				if(maxDotCount >= 0 && dot > maxDotCount) {
					break;
				}
				
				mml.append(".");
				tick += (baseNoteTick >> dot);
				
				// quit if the note length exceeds c1^c1
				if(tick > (tpqn * 8)) {
					break;
				}
				
				// skip existing definitions
				// c6. == c4, for example.
				if(notes[tick] != null) {
					break;
				}
				
				// create length table
				List<Integer> dottedNoteLength = new ArrayList<>();
				dottedNoteLength.add(tick);
				dottedNoteLengthDotsDisassembled.add(baseNoteTick >> dot);
				
				// add new note
				notes[tick] = new MMLNoteInfo(mmlSymbol, mml.toString());
				noteLengths.set(tick, dottedNoteLength);
				noteLengthsDotsDisassembled.set(tick, new ArrayList<>(dottedNoteLengthDotsDisassembled));
				singleNotes[tick] = notes[tick];
				singleNoteLengths.set(tick, dottedNoteLength);
				maxDotCountUsed = dot;
				
				dot++;
			}
		}
		this.setMaxDotCount(maxDotCountUsed);
		
		// search for combinations such as c4^c16
		// having less notes (shorter text) is preferred
		boolean tableIsFilled = false;
		while(!tableIsFilled) {
			// make a shallow copy the note table to prevent recursive update
			MMLNoteInfo[] prevNotes = Arrays.copyOf(notes, notes.length);
			
			// process for all items
			for(tick = 1; tick < notes.length; tick++) {
				// skip existing definitions
				if(notes[tick] != null) {
					continue;
				}
				
				// search the combination
				String mml = null;
				List<Integer> multipleNoteLengths = null;
				List<Integer> multipleNoteLengthsDotsDisassembled = null;
				for(int tickSub = tick - 1; tickSub > 0; tickSub--) {
					if(prevNotes[tickSub] != null && singleNotes[tick - tickSub] != null) {
						String newMML = prevNotes[tickSub].getText() + mmlSymbol.getTie()
								+ singleNotes[tick - tickSub].getText();
						if(mml == null || mml.length() > newMML.length()) {
							mml = newMML;
							multipleNoteLengths = new ArrayList<>(noteLengths.get(tickSub));
							multipleNoteLengths.addAll(noteLengths.get(tick - tickSub));
							multipleNoteLengthsDotsDisassembled = new ArrayList<>(
									noteLengthsDotsDisassembled.get(tickSub));
							multipleNoteLengthsDotsDisassembled.addAll(noteLengthsDotsDisassembled.get(tick - tickSub));
						}
					}
				}
				// add new note if available
				if(mml != null) {
					notes[tick] = new MMLNoteInfo(mmlSymbol, mml);
					noteLengths.set(tick, multipleNoteLengths);
					noteLengthsDotsDisassembled.set(tick, multipleNoteLengthsDotsDisassembled);
				}
			}
			
			// quit if all items are set
			tableIsFilled = true;
			for(tick = 1; tick < notes.length; tick++) {
				if(notes[tick] == null) {
					tableIsFilled = false;
					break;
				}
			}
		}
		
		for(tick = 1; tick < tpqn * 8; tick++) {
			List<Integer> lengths;
			
			lengths = noteLengths.get(tick);
			Collections.sort(lengths);
			Collections.reverse(lengths);
			
			lengths = noteLengthsDotsDisassembled.get(tick);
			Collections.sort(lengths);
			Collections.reverse(lengths);
		}
	}
}
