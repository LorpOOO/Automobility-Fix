package dev.automobilityfix.mixinplugin;

import net.neoforged.fml.loading.LoadingModList;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.util.List;
import java.util.Set;

/** Applies the mixins for optional mods (Open Parties and Claims, FTB Chunks) only when they are installed. */
public class FixMixinPlugin implements IMixinConfigPlugin {
    private boolean opacPresent;
    private boolean ftbChunksPresent;

    @Override
    public void onLoad(String mixinPackage) {
        var mods = LoadingModList.get();
        this.opacPresent = mods != null && mods.getModFileById("openpartiesandclaims") != null;
        this.ftbChunksPresent = mods != null && mods.getModFileById("ftbchunks") != null;
    }

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        if (mixinClassName.contains(".opac.")) {
            return this.opacPresent;
        }
        if (mixinClassName.contains(".ftbchunks.")) {
            return this.ftbChunksPresent;
        }
        return true;
    }

    @Override
    public String getRefMapperConfig() {
        return null;
    }

    @Override
    public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {
    }

    @Override
    public List<String> getMixins() {
        return null;
    }

    @Override
    public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
    }

    @Override
    public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
    }
}
