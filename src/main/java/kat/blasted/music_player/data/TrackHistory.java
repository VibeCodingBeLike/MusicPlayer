package kat.blasted.music_player.data;

import java.util.LinkedList;

public class TrackHistory {
	private static TrackHistory instance;
	private final LinkedList<TrackData> history = new LinkedList<>();
	private static final int MAX_HISTORY = 50;

	public static TrackHistory getInstance() {
		if (instance == null) {
			instance = new TrackHistory();
		}
		return instance;
	}

	public void addTrack(TrackData track) {
		history.remove(track);
		if (history.size() >= MAX_HISTORY) {
			history.removeLast();
		}
		history.addFirst(track);
	}

	public void clear() {
		history.clear();
	}

	public LinkedList<TrackData> getHistory() {
		return history;
	}
}
