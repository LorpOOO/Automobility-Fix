package dev.automobilityfix;

import com.mojang.logging.LogUtils;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.NeoForge;
import org.slf4j.Logger;

@Mod(AutomobilityFix.MOD_ID)
public class AutomobilityFix {
    public static final String MOD_ID = "automobility_fix";
    public static final String OPAC_ID = "openpartiesandclaims";
    public static final Logger LOGGER = LogUtils.getLogger();

    public static final String FTB_TEAMS_ID = "ftbteams";

    private static boolean opacLoaded;
    private static boolean ftbTeamsLoaded;

    public AutomobilityFix(IEventBus modBus, ModContainer container) {
        container.registerConfig(ModConfig.Type.SERVER, FixConfig.SPEC);
        opacLoaded = ModList.get().isLoaded(OPAC_ID);
        ftbTeamsLoaded = ModList.get().isLoaded(FTB_TEAMS_ID);

        NeoForge.EVENT_BUS.register(FixEvents.class);

        LOGGER.info("Automobility Fix loaded (Open Parties and Claims: {}, FTB Teams: {})",
                opacLoaded ? "on" : "off", ftbTeamsLoaded ? "on" : "off");
    }

    public static boolean isOpacLoaded() {
        return opacLoaded;
    }

    public static boolean isFtbTeamsLoaded() {
        return ftbTeamsLoaded;
    }
}
