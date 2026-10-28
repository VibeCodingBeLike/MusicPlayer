package kat.blasted.music_player.gui;

import kat.blasted.music_player.MusicPlayerMod;
import kat.blasted.music_player.data.AlbumCover;
import kat.blasted.music_player.data.TrackData;
import kat.blasted.music_player.util.MusicPlayerHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.ContainerObjectSelectionList;
import net.minecraft.client.gui.components.ImageButton;
import net.minecraft.client.gui.components.WidgetSprites;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.resources.sounds.Sound;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.util.CommonColors;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class MusicEntry extends ContainerObjectSelectionList.Entry<MusicEntry> {
	public static final int GRAY_COLOR = ARGB.color(255, 74, 74, 74);
	private static final WidgetSprites PLAY_BUTTON_TEXTURE = new WidgetSprites(
		Identifier.fromNamespaceAndPath(MusicPlayerMod.MOD_ID, "jukebox/play_button"),
		Identifier.fromNamespaceAndPath(MusicPlayerMod.MOD_ID, "jukebox/play_button_disabled"),
		Identifier.fromNamespaceAndPath(MusicPlayerMod.MOD_ID, "jukebox/play_button_focused")
	);
	private static final WidgetSprites STOP_BUTTON_TEXTURE = new WidgetSprites(
		Identifier.fromNamespaceAndPath(MusicPlayerMod.MOD_ID, "jukebox/stop_button"),
		Identifier.fromNamespaceAndPath(MusicPlayerMod.MOD_ID, "jukebox/stop_button_focused")
	);

	private final Minecraft client;
	private final JukeboxScreen parentScreen;
	public final TrackData entry;
	private final List<AbstractWidget> children = new ArrayList<>();
	private final ImageButton playButton;
	private final ImageButton stopButton;

	public MusicEntry(Minecraft client, JukeboxScreen parentScreen, TrackData entry) {
		this.client = client;
		this.parentScreen = parentScreen;
		this.entry = entry;

		this.playButton = new ImageButton(0, 0, 20, 20, PLAY_BUTTON_TEXTURE, b -> {
			MusicPlayerHelper.playTrack(this.client, entry);
		}, Component.translatable("gui.music_player.jukebox.play"));

		this.stopButton = new ImageButton(0, 0, 20, 20, STOP_BUTTON_TEXTURE, b -> {
			MusicPlayerHelper.stopMusic(this.client);
		}, Component.translatable("gui.music_player.jukebox.stop"));

		this.children.add(this.playButton);
		this.children.add(this.stopButton);
	}

	@Override
	public void extractContent(GuiGraphicsExtractor graphics, int mouseX, int mouseY, boolean isHovering, float partialTick) {
		int contentX = this.getContentX();
		int contentY = this.getContentY();
		int contentWidth = this.getContentWidth();
		int contentHeight = this.getContentHeight();
		int contentRight = this.getContentRight();
		int contentBottom = this.getContentBottom();

		graphics.fill(contentX, contentY, contentRight, contentBottom, GRAY_COLOR);

		AlbumCover albumCover = this.entry.getAlbumCover();
		int coverX = contentX + 4;
		int coverY = contentY + (contentHeight - albumCover.getWidth()) / 2;
		albumCover.drawCover(graphics, coverX, coverY);

		int textStartX = coverX + 24 + 4;
		int textStopX = contentX + (contentWidth - this.playButton.getWidth()) - 8;
		int textStartY = contentY;
		int textStopY = contentY + contentHeight / 2;

		Font font = this.client.font;
		Component firstLine = entry.title().copy().append(" - ").withColor(CommonColors.WHITE)
			.append(entry.author().copy().withColor(CommonColors.LIGHT_GRAY));

		drawScrollingText(graphics, font, firstLine, textStartX, textStopX, textStartY, textStopY, true);

		if (entry.album().isPresent()) {
			textStartY += contentHeight / 2;
			textStopY = contentY + contentHeight;
			Component secondLine = entry.album().get().copy().setStyle(Style.EMPTY.withColor(CommonColors.LIGHT_GRAY));
			drawScrollingText(graphics, font, secondLine, textStartX, textStopX, textStartY, textStopY, false);
		}

		boolean isPlaying = isPlaying();

		this.playButton.setX(contentRight - this.playButton.getWidth() - 4);
		this.playButton.setY(contentY + contentHeight / 2 - this.playButton.getHeight() / 2);
		this.playButton.active = !isPlaying && !MusicPlayerHelper.isVolumeZero();
		this.playButton.visible = !isPlaying;
		this.playButton.extractRenderState(graphics, mouseX, mouseY, partialTick);

		this.stopButton.setX(contentRight - this.stopButton.getWidth() - 4);
		this.stopButton.setY(contentY + contentHeight / 2 - this.stopButton.getHeight() / 2);
		this.stopButton.active = isPlaying;
		this.stopButton.visible = isPlaying;
		this.stopButton.extractRenderState(graphics, mouseX, mouseY, partialTick);
	}

	private void drawScrollingText(GuiGraphicsExtractor graphics, Font font, Component text, int left, int right, int top, int bottom, boolean shadow) {
		if (!shadow) {
			text = text.copy().withoutShadow();
		}
		if (right > left && bottom > top) {
			graphics.textRenderer().acceptScrolling(
				text,
				left + font.width(text) / 2,
				left,
				right,
				top,
				bottom
			);
		}
	}

	private boolean isPlaying() {
		SoundInstance playing = MusicPlayerMod.currentlyPlaying;
		if (playing != null) {
			if (!client.getSoundManager().isActive(playing)) {
				MusicPlayerMod.currentlyPlaying = null;
				return false;
			}
			if (playing.getIdentifier().equals(entry.getResolvedId()) || playing.getIdentifier().equals(entry.key())) {
				return true;
			}
			Sound sound = playing.getSound();
			if (sound != null && sound.getLocation().equals(entry.key())) {
				return true;
			}
		}
		return false;
	}

	@Override
	public @NotNull List<? extends GuiEventListener> children() {
		return this.children;
	}

	@Override
	public @NotNull List<? extends NarratableEntry> narratables() {
		return this.children;
	}
}
