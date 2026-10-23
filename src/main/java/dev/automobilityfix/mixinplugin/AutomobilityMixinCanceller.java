package dev.automobilityfix.mixinplugin;

import com.bawnorton.mixinsquared.api.MixinCanceller;
import net.neoforged.fml.loading.FMLLoader;

import java.util.List;

/**
 * Automobility's {@code EntityMixin} is registered as a common mixin but references the client-only
 * {@code LocalPlayer} class. Some builds of Automobility then crash dedicated servers on startup with
 * "Attempted to load class net/minecraft/client/player/LocalPlayer for invalid dist DEDICATED_SERVER".
 * The mixin only does something for the local player, so it is safe to drop it on dedicated servers.
 */
public class AutomobilityMixinCanceller implements MixinCanceller {
    private static final String CLIENT_ONLY_ENTITY_MIXIN = "io.github.foundationgames.automobility.mixin.EntityMixin";

    @Override
    public boolean shouldCancel(List<String> targetClassNames, String mixinClassName) {
        return CLIENT_ONLY_ENTITY_MIXIN.equals(mixinClassName) && FMLLoader.getDist().isDedicatedServer();
    }
}
