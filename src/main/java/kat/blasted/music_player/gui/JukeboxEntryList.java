package kat.blasted.music_player.gui;

import kat.blasted.music_player.data.TrackData;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.ContainerObjectSelectionList;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class JukeboxEntryList extends ContainerObjectSelectionList<MusicEntry> {
	private final JukeboxScreen parent;
	private final List<MusicEntry> allEntries = new ArrayList<>();
	@Nullable
	private String filter;

	public JukeboxEntryList(JukeboxScreen parent, Minecraft minecraft, int width, int height, int y, int itemHeight) {
		super(minecraft, width, height, y, itemHeight);
		this.parent = parent;
	}

	@Override
	protected void extractListBackground(GuiGraphicsExtractor graphics) {}

	@Override
	protected void extractListSeparators(GuiGraphicsExtractor graphics) {}

	@Override
	public int getRowWidth() {
		return 220;
	}

	public void setTracks(Collection<TrackData> tracks, double scroll) {
		this.allEntries.clear();
		for (TrackData td : tracks) {
			this.allEntries.add(new MusicEntry(this.minecraft, this.parent, td));
		}
		this.applyFilter();
		this.setScrollAmount(scroll);
	}

	public void setFilter(@Nullable String filter) {
		this.filter = filter;
		this.applyFilter();
	}

	private void applyFilter() {
		if (this.filter == null || this.filter.isBlank()) {
			this.replaceEntries(this.allEntries);
		} else {
			String[] terms = this.filter.toLowerCase(Locale.ROOT).split(" ");
			List<MusicEntry> filtered = new ArrayList<>();
			for (MusicEntry entry : this.allEntries) {
				String albumText = entry.entry.album().map(Component::getString).orElse("");
				String combined = (entry.entry.title().getString() + " " + entry.entry.author().getString() + " " + albumText).toLowerCase(Locale.ROOT);
				boolean matches = Arrays.stream(terms).allMatch(combined::contains);
				if (matches) {
					filtered.add(entry);
				}
			}
			this.replaceEntries(filtered);
		}
	}

	public boolean isEmpty() {
		return this.children().isEmpty();
	}
}
