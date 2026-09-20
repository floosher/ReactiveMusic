package circuitlord.reactivemusic.mixin;

import circuitlord.reactivemusic.ReactiveMusic;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.sound.SoundInstance;
import net.minecraft.client.sound.SoundManager;
//? if >=1.21.9 {
/*import net.minecraft.client.sound.SoundSystem;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
*///?} else {
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
//?}
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;

@Mixin(SoundManager.class)
public class SoundManagerMixin {

    @Unique
    private static boolean reactiveMusic$isLoggingSound = false;

    //? if >=1.21.9 {
    /*@Inject(method = "play(Lnet/minecraft/client/sound/SoundInstance;)Lnet/minecraft/client/sound/SoundSystem$PlayResult;", at = @At("HEAD"), cancellable = true)
    private void play(SoundInstance soundInstance, CallbackInfoReturnable<SoundSystem.PlayResult> ci) {
    *///?} else {
    @Inject(method = "Lnet/minecraft/client/sound/SoundManager;play(Lnet/minecraft/client/sound/SoundInstance;)V", at = @At("HEAD"), cancellable = true)
    private void play(SoundInstance soundInstance, CallbackInfo ci) {
        //?}

        String path = soundInstance.getId().getPath();
        String id = soundInstance.getId().toString();

        MinecraftClient mc = MinecraftClient.getInstance();

        if (mc.player != null && ReactiveMusic.printSoundEvents && !reactiveMusic$isLoggingSound) {
            reactiveMusic$isLoggingSound = true;
            try {
                mc.player.sendMessage(Text.of("[ReactiveMusic]: Sound: " + id + " Attenuation: " + soundInstance.getAttenuationType()), false);
            } finally {
                reactiveMusic$isLoggingSound = false;
            }
        }

        if (ReactiveMusic.config == null
                || ReactiveMusic.config.soundsMuteMusic == null
                || ReactiveMusic.config.soundsMuteMusicIgnoreDistance == null) {
            return;
        }

        if (path.contains("music_disc")) {
            ReactiveMusic.trackSoundMuteMusic(soundInstance, false);
            return;
        }

        if (path.contains("battle.pv")) {
            ReactiveMusic.trackSoundMuteMusic(soundInstance, false);
            ReactiveMusic.LOGGER.info("Detected cobblemon battle event, adding to list!");
            return;
        }

        if (reactiveMusic$isMusicSound(path, id)) {
            if (reactiveMusic$matchesMuteSoundList(path, id, ReactiveMusic.config.soundsMuteMusic)) {
                ReactiveMusic.trackSoundMuteMusic(soundInstance, false);
                return;
            }

            if (reactiveMusic$matchesMuteSoundList(path, id, ReactiveMusic.config.soundsMuteMusicIgnoreDistance)) {
                ReactiveMusic.trackSoundMuteMusic(soundInstance, true);
                return;
            }

            ci.cancel();
            return;
        }

        for (String muteSound : ReactiveMusic.config.soundsMuteMusic) {
            if (reactiveMusic$matchesMuteSound(path, id, muteSound)) {
                ReactiveMusic.trackSoundMuteMusic(soundInstance, false);
                break;
            }
        }

        for (String muteSound : ReactiveMusic.config.soundsMuteMusicIgnoreDistance) {
            if (reactiveMusic$matchesMuteSound(path, id, muteSound)) {
                ReactiveMusic.trackSoundMuteMusic(soundInstance, true);
                break;
            }
        }
    }

    @Unique
    private boolean reactiveMusic$isMusicSound(String path, String id) {
        return reactiveMusic$isMusicId(path) || reactiveMusic$isMusicId(id);
    }

    @Unique
    private boolean reactiveMusic$isMusicId(String id) {
        int namespaceSeparator = id.indexOf(':');
        String idPath = namespaceSeparator >= 0 ? id.substring(namespaceSeparator + 1) : id;

        return idPath.startsWith("music.");
    }

    @Unique
    private boolean reactiveMusic$matchesMuteSoundList(String path, String id, Iterable<String> muteSounds) {
        for (String muteSound : muteSounds) {
            if (reactiveMusic$matchesMuteSound(path, id, muteSound)) {
                return true;
            }
        }

        return false;
    }


    @Unique
    private boolean reactiveMusic$matchesMuteSound(String path, String id, String muteSound) {
        if (muteSound == null) {
            return false;
        }

        String normalizedMuteSound = muteSound.trim().toLowerCase();
        if (normalizedMuteSound.isEmpty()) {
            return false;
        }

        String normalizedPath = path.toLowerCase();
        String normalizedId = id.toLowerCase();

        return normalizedPath.contains(normalizedMuteSound) || normalizedId.contains(normalizedMuteSound);
    }
}
