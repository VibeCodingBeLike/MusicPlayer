package kat.blasted.music_player.data;

import kat.blasted.music_player.MusicPlayerMod;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.Nullable;

public class AlbumCover {
	private final Identifier sprite;

	private AlbumCover(String namespace, @Nullable String spriteId) {
		if (spriteId == null || spriteId.isEmpty()) {
			spriteId = namespace.equalsIgnoreCase("minecraft") ? "generic" : "modded";
		}
		this.sprite = Identifier.fromNamespaceAndPath(MusicPlayerMod.MOD_ID, "cover/" + spriteId);
	}

	public static AlbumCover fromSprite(String namespace, @Nullable String spriteId) {
		return new AlbumCover(namespace, spriteId);
	}

	public int getWidth() {
		return 20;
	}

	public int getHeight() {
		return 20;
	}

	public void drawCover(GuiGraphicsExtractor graphics, int x, int y) {
		graphics.blitSprite(RenderPipelines.GUI_TEXTURED, this.sprite, x, y, getWidth(), getHeight());
	}
}
