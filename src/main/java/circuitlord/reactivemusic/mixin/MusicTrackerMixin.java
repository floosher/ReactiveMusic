package circuitlord.reactivemusic.mixin;

//import circuitlord.reactivemusic.SongLoader;
import circuitlord.reactivemusic.ReactiveMusic;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.sound.MusicTracker;
import net.minecraft.client.sound.SoundInstance;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Environment(EnvType.CLIENT)
@Mixin(MusicTracker.class)
public abstract class MusicTrackerMixin {
    @Shadow
    private SoundInstance current;

    @Shadow
    public abstract void stop();

    @Inject(method = "tick", at = @At("HEAD"), cancellable = true)
    private void reactivemusic$tick(CallbackInfo ci) {

        if (ReactiveMusic.isInactive()) {
            return;
        }

        if (ReactiveMusic.config != null && reactiveMusic$hasMusicMuteEntry()) {
            return;
        }

        if (current != null) {
            stop();
        }

        ci.cancel();

    }

    @Unique
    private boolean reactiveMusic$hasMusicMuteEntry() {
        return reactiveMusic$hasMusicMuteEntry(ReactiveMusic.config.soundsMuteMusic)
                || reactiveMusic$hasMusicMuteEntry(ReactiveMusic.config.soundsMuteMusicIgnoreDistance);
    }

    @Unique
    private boolean reactiveMusic$hasMusicMuteEntry(Iterable<String> entries) {
        for (String entry : entries) {
            if (entry == null) {
                continue;
            }

            String normalized = entry.trim().toLowerCase();
            if (reactiveMusic$isMusicId(normalized)) {
                return true;
            }
        }

        return false;
    }

    @Unique
    private boolean reactiveMusic$isMusicId(String id) {
        int namespaceSeparator = id.indexOf(':');
        String path = namespaceSeparator >= 0 ? id.substring(namespaceSeparator + 1) : id;

        return path.startsWith("music.");
    }

}
