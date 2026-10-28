package kat.blasted.music_player.mixin;

import kat.blasted.music_player.MusicPlayerMod;
import kat.blasted.music_player.gui.JukeboxScreen;
import net.minecraft.client.gui.components.SpriteIconButton;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PauseScreen.class)
public abstract class PauseScreenMixin extends Screen {
	protected PauseScreenMixin(Component title) {
		super(title);
	}

	@Inject(method = "createPauseMenu", at = @At("TAIL"))
	private void addMusicPlayerButton(CallbackInfo ci) {
		SpriteIconButton button = SpriteIconButton.builder(
			Component.translatable("gui.music_player.jukebox.menu"),
			b -> this.minecraft.gui.setScreen(new JukeboxScreen(this)),
			true
		)
		.width(20)
		.sprite(Identifier.fromNamespaceAndPath(MusicPlayerMod.MOD_ID, "jukebox/jukebox"), 14, 14)
		.build();
		button.setPosition(12, 12);
		this.addRenderableWidget(button);
	}
}
