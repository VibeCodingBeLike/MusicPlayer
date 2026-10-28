package kat.blasted.music_player.gui;

import kat.blasted.music_player.MusicPlayerMod;
import kat.blasted.music_player.data.TrackData;
import kat.blasted.music_player.data.TrackDataManager;
import kat.blasted.music_player.data.TrackHistory;
import kat.blasted.music_player.util.MusicPlayerHelper;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.layouts.HeaderAndFooterLayout;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.options.SoundOptionsScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.CommonColors;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;

public class JukeboxScreen extends Screen {
	private static final Identifier BACKGROUND_TEXTURE = Identifier.fromNamespaceAndPath(MusicPlayerMod.MOD_ID, "jukebox/background");
	private static final Identifier SEARCH_ICON_TEXTURE = Identifier.fromNamespaceAndPath("minecraft", "icon/search");

	private static final Component TITLE = Component.translatable("gui.music_player.jukebox.title");
	private static final Component MUSIC_TAB_TITLE = Component.translatable("gui.music_player.jukebox.all_music");
	private static final Component HISTORY_TAB_TITLE = Component.translatable("gui.music_player.jukebox.history");
	private static final Component SELECTED_MUSIC_TAB = MUSIC_TAB_TITLE.plainCopy().withStyle(ChatFormatting.UNDERLINE);
	private static final Component SELECTED_HISTORY_TAB = HISTORY_TAB_TITLE.plainCopy().withStyle(ChatFormatting.UNDERLINE);

	private static final Component STOP_SOUND_BUTTON = Component.translatable("gui.music_player.jukebox.stop_button");
	private static final Component MASTER_VOLUME_ZERO = Component.translatable("gui.music_player.jukebox.master_volume_zero");
	private static final Component MUSIC_VOLUME_ZERO = Component.translatable("gui.music_player.jukebox.music_volume_zero");
	private static final Component CLEAR_HISTORY = Component.translatable("gui.music_player.jukebox.clear_history");

	private static final Component SEARCH_HINT = Component.translatable("gui.music_player.jukebox.search_hint").withStyle(ChatFormatting.ITALIC, ChatFormatting.GRAY);
	private static final Component EMPTY_SEARCH_TEXT = Component.translatable("gui.music_player.jukebox.empty_search").withStyle(ChatFormatting.GRAY);
	private static final Component EMPTY_HISTORY_TEXT = Component.translatable("gui.music_player.jukebox.empty_history").withStyle(ChatFormatting.GRAY);

	private final HeaderAndFooterLayout layout = new HeaderAndFooterLayout(this);
	private final @Nullable Screen parent;

	public enum Tab {
		MUSIC,
		HISTORY
	}

	Tab currentTab = Tab.MUSIC;
	private EditBox searchBox;
	private JukeboxEntryList entryList;
	private Button musicTabButton;
	private Button historyTabButton;
	private Button stopMusicButton;
	private Button clearHistoryButton;

	public JukeboxScreen(@Nullable Screen parent) {
		super(TITLE);
		this.parent = parent;
	}

	@Override
	public void init() {
		this.layout.addTitleHeader(TITLE, this.font);

		this.entryList = new JukeboxEntryList(this, this.minecraft, this.width, this.listEnd() - 88, 88, 36);

		int middle = this.entryList.getRowWidth() / 2;
		int rowLeft = this.entryList.getRowLeft();

		this.musicTabButton = this.addRenderableWidget(Button.builder(MUSIC_TAB_TITLE, b -> this.setCurrentTab(Tab.MUSIC)).bounds(rowLeft, 43, middle, 20).build());
		this.historyTabButton = this.addRenderableWidget(Button.builder(HISTORY_TAB_TITLE, b -> this.setCurrentTab(Tab.HISTORY)).bounds(rowLeft + middle + 1, 43, middle, 20).build());

		this.clearHistoryButton = this.addRenderableWidget(Button.builder(CLEAR_HISTORY, b -> {
			TrackHistory.getInstance().clear();
			this.setCurrentTab(Tab.HISTORY);
		}).bounds(10, 10, this.font.width(CLEAR_HISTORY) + 12, 20).build());
		this.clearHistoryButton.visible = false;

		this.stopMusicButton = this.addRenderableWidget(Button.builder(STOP_SOUND_BUTTON, b -> {
			if (MusicPlayerHelper.isVolumeZero()) {
				this.minecraft.gui.setScreen(new SoundOptionsScreen(this, this.minecraft.options));
			} else {
				MusicPlayerHelper.stopMusic(this.minecraft);
				this.updateStopButtonText();
			}
		}).bounds(this.entryList.getRowLeft() - 1, this.listEnd() + 10, this.entryList.getRowRight() - this.entryList.getRowLeft() + 1, 20).build());

		this.searchBox = new EditBox(this.font, this.marginX() + 28, 74, 200, 15, SEARCH_HINT);
		this.searchBox.setMaxLength(255);
		this.searchBox.setHint(SEARCH_HINT);
		this.searchBox.setTextColor(CommonColors.WHITE);
		this.searchBox.setResponder(this::onSearchChanged);
		this.addRenderableWidget(this.searchBox);

		this.addWidget(this.entryList);
		this.layout.addToFooter(Button.builder(CommonComponents.GUI_BACK, b -> this.onClose()).build());
		this.layout.visitWidgets(this::addRenderableWidget);

		this.updateStopButtonText();
		this.setCurrentTab(Tab.MUSIC);
		this.repositionElements();
	}

	@Override
	public void added() {
		super.added();
		this.updateStopButtonText();
	}

	private void updateStopButtonText() {
		if (this.stopMusicButton != null) {
			if (this.minecraft.options.getSoundSourceVolume(SoundSource.MASTER) == 0f) {
				this.stopMusicButton.setMessage(MASTER_VOLUME_ZERO);
			} else if (this.minecraft.options.getSoundSourceVolume(SoundSource.MUSIC) == 0f) {
				this.stopMusicButton.setMessage(MUSIC_VOLUME_ZERO);
			} else {
				this.stopMusicButton.setMessage(STOP_SOUND_BUTTON);
			}
		}
	}

	@Override
	protected void repositionElements() {
		this.layout.arrangeElements();
		this.entryList.updateSizeAndPosition(this.width + 5, this.listEnd() - 88, 88);

		int middle = this.entryList.getRowWidth() / 2;
		int rowLeft = this.entryList.getRowLeft();

		this.searchBox.setPosition(this.marginX() + 28, 74);
		this.stopMusicButton.setPosition(this.entryList.getRowLeft() - 1, this.listEnd() + 12);
		this.musicTabButton.setPosition(rowLeft, 45);
		this.historyTabButton.setPosition(rowLeft + middle + 1, 45);
		this.clearHistoryButton.setPosition(10, 10);
	}

	@Override
	public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
		super.extractBackground(graphics, mouseX, mouseY, partialTick);
		int x = this.marginX() + 3;
		graphics.blitSprite(RenderPipelines.GUI_TEXTURED, BACKGROUND_TEXTURE, x, 64, 236, this.getScreenHeight() + 16);
		graphics.blitSprite(RenderPipelines.GUI_TEXTURED, SEARCH_ICON_TEXTURE, x + 10, 76, 12, 12);
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
		super.extractRenderState(graphics, mouseX, mouseY, delta);
		if (!this.entryList.isEmpty()) {
			this.entryList.extractRenderState(graphics, mouseX, mouseY, delta);
		} else if (!this.searchBox.getValue().isEmpty()) {
			graphics.centeredText(this.minecraft.font, EMPTY_SEARCH_TEXT, this.width / 2, (72 + this.listEnd()) / 2, CommonColors.WHITE);
		} else if (this.currentTab == Tab.HISTORY) {
			graphics.centeredText(this.minecraft.font, EMPTY_HISTORY_TEXT, this.width / 2, (72 + this.listEnd()) / 2, CommonColors.WHITE);
		}
	}

	private void setCurrentTab(Tab tab) {
		this.currentTab = tab;
		this.searchBox.setValue("");
		this.musicTabButton.setMessage(tab == Tab.MUSIC ? SELECTED_MUSIC_TAB : MUSIC_TAB_TITLE);
		this.historyTabButton.setMessage(tab == Tab.HISTORY ? SELECTED_HISTORY_TAB : HISTORY_TAB_TITLE);
		this.clearHistoryButton.visible = (tab == Tab.HISTORY);

		Collection<TrackData> items = (tab == Tab.MUSIC)
			? TrackDataManager.getInstance().tracks.values()
			: TrackHistory.getInstance().getHistory();

		this.entryList.setTracks(items, this.entryList.scrollAmount());
	}

	private void onSearchChanged(String text) {
		this.entryList.setFilter(text);
	}

	@Override
	public void onClose() {
		this.minecraft.gui.setScreen(this.parent);
	}

	@Override
	public boolean isPauseScreen() {
		return true;
	}

	private int marginX() {
		return (this.width - 236) / 2;
	}

	private int getScreenHeight() {
		return Math.max(52, this.height - 128 - 16);
	}

	private int listEnd() {
		return 80 + this.getScreenHeight() - 8;
	}
}
