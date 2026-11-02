package com.batisteto.tempest.command;

import com.batisteto.tempest.config.TempestConfig;
import com.batisteto.tempest.entity.TornadoEntity;
import com.batisteto.tempest.registry.ModEntities;
import com.batisteto.tempest.weather.EFScale;
import com.batisteto.tempest.weather.StormCell;
import com.batisteto.tempest.weather.StormType;
import com.batisteto.tempest.weather.WeatherManager;
import com.batisteto.tempest.weather.WindUtil;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.entity.EntityTypeTest;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

import java.util.List;

/**
 * /tempest clear | info | list | preset <name> | spawn storm <type> | spawn tornado [ef]
 *          | wind set <mph> <degrees> | autospawn <bool>
 */
public class TempestCommand {
    private static final String[] PRESETS = {"supercell", "ef4_wedge", "rope_tornado", "outbreak", "squall_line", "cyclone"};

    private static final SuggestionProvider<CommandSourceStack> STORM_TYPES = (ctx, builder) -> {
        for (StormType t : StormType.values()) builder.suggest(t.name().toLowerCase());
        return builder.buildFuture();
    };
    private static final SuggestionProvider<CommandSourceStack> PRESET_NAMES = (ctx, builder) -> {
        for (String p : PRESETS) builder.suggest(p);
        return builder.buildFuture();
    };

    @SubscribeEvent
    public void onRegisterCommands(RegisterCommandsEvent event) {
        var root = Commands.literal("tempest").requires(src -> src.hasPermission(2));

        root.then(Commands.literal("clear").executes(ctx -> clear(ctx.getSource())));
        root.then(Commands.literal("info").executes(ctx -> info(ctx.getSource())));
        root.then(Commands.literal("list").executes(ctx -> list(ctx.getSource())));

        root.then(Commands.literal("preset")
                .then(Commands.argument("name", StringArgumentType.word()).suggests(PRESET_NAMES)
                        .executes(ctx -> preset(ctx.getSource(), StringArgumentType.getString(ctx, "name")))));

        root.then(Commands.literal("spawn")
                .then(Commands.literal("storm")
                        .then(Commands.argument("type", StringArgumentType.word()).suggests(STORM_TYPES)
                                .executes(ctx -> spawnStorm(ctx.getSource(), StringArgumentType.getString(ctx, "type")))))
                .then(Commands.literal("tornado")
                        .executes(ctx -> spawnTornado(ctx.getSource(), 2))
                        .then(Commands.argument("ef", IntegerArgumentType.integer(0, 5))
                                .executes(ctx -> spawnTornado(ctx.getSource(), IntegerArgumentType.getInteger(ctx, "ef"))))));

        root.then(Commands.literal("wind")
                .then(Commands.literal("set")
                        .then(Commands.argument("speedMph", FloatArgumentType.floatArg(0, 300))
                                .then(Commands.argument("degrees", FloatArgumentType.floatArg(0, 360))
                                        .executes(ctx -> setWind(ctx.getSource(),
                                                FloatArgumentType.getFloat(ctx, "speedMph"),
                                                FloatArgumentType.getFloat(ctx, "degrees")))))));

        root.then(Commands.literal("autospawn")
                .then(Commands.argument("enabled", BoolArgumentType.bool())
                        .executes(ctx -> setAutoSpawn(ctx.getSource(), BoolArgumentType.getBool(ctx, "enabled")))));

        event.getDispatcher().register(root);
    }

    // ---------------------------------------------------------------- helpers

    /** World position forwardDist blocks in front of the source (and lateral blocks to its right). */
    private static double[] pos(CommandSourceStack src, double forward, double lateral) {
        double yaw = Math.toRadians(src.getRotation().y);
        double fx = -Math.sin(yaw), fz = Math.cos(yaw);
        double rx = Math.cos(yaw), rz = Math.sin(yaw);
        Vec3 p = src.getPosition();
        return new double[]{p.x + fx * forward + rx * lateral, p.z + fz * forward + rz * lateral};
    }

    private static StormCell storm(CommandSourceStack src, StormType type, double forward, double lateral) {
        ServerLevel level = src.getLevel();
        double[] at = pos(src, forward, lateral);
        Vec3 me = src.getPosition();
        StormCell cell = WeatherManager.get(level).spawnStorm(level, level.random, type, at[0], at[1], me.x, me.z);
        cell.forceMature();
        cell.motionX *= 0.3f; // drift slowly so a preset can actually be watched
        cell.motionZ *= 0.3f;
        return cell;
    }

    private static TornadoEntity tornado(CommandSourceStack src, int ef, double forward, double lateral) {
        ServerLevel level = src.getLevel();
        double[] at = pos(src, forward, lateral);
        TornadoEntity t = ModEntities.TORNADO.get().create(level);
        if (t == null) return null;
        int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING, (int) Math.floor(at[0]), (int) Math.floor(at[1]));
        t.moveTo(at[0], y, at[1], 0, 0);
        t.setEfScale(EFScale.byOrdinalClamped(ef));
        double yaw = Math.toRadians(src.getRotation().y);
        t.setMotion((float) (-Math.sin(yaw) * 0.12), (float) (Math.cos(yaw) * 0.12));
        level.addFreshEntity(t);
        return t;
    }

    private static List<? extends TornadoEntity> tornadoes(ServerLevel level) {
        return level.getEntities(EntityTypeTest.forClass(TornadoEntity.class), e -> true);
    }

    // --------------------------------------------------------------- commands

    private int clear(CommandSourceStack source) {
        ServerLevel level = source.getLevel();
        WeatherManager.get(level).clear(level);
        int removed = 0;
        for (TornadoEntity t : tornadoes(level)) {
            t.discard();
            removed++;
        }
        int n = removed;
        source.sendSuccess(() -> Component.literal("Cleared all storms and " + n + " tornado(es)."), true);
        return 1;
    }

    private int info(CommandSourceStack source) {
        ServerLevel level = source.getLevel();
        WeatherManager mgr = WeatherManager.get(level);
        String msg = String.format("Wind %.1f mph from the %s | %d storm cell(s) | %d tornado(es) | autospawn %s",
                WindUtil.speedMph(mgr.windX, mgr.windZ), WindUtil.compassDirection(mgr.windX, mgr.windZ),
                mgr.getCells().size(), tornadoes(level).size(), TempestConfig.AUTO_SPAWN.get() ? "on" : "off");
        source.sendSuccess(() -> Component.literal(msg), false);
        return 1;
    }

    private int list(CommandSourceStack source) {
        ServerLevel level = source.getLevel();
        List<StormCell> cells = WeatherManager.get(level).getCells();
        if (cells.isEmpty()) {
            source.sendSuccess(() -> Component.literal("No active storms."), false);
            return 0;
        }
        Vec3 me = source.getPosition();
        for (StormCell c : cells) {
            double dist = Math.sqrt((c.x - me.x) * (c.x - me.x) + (c.z - me.z) * (c.z - me.z));
            String line = String.format("#%d %s (%s) intensity %d%%, radius %d, %d blocks away",
                    c.id, c.type.name(), c.stage.name().toLowerCase(), Math.round(c.intensity * 100),
                    Math.round(c.radius), Math.round(dist));
            source.sendSuccess(() -> Component.literal(line), false);
        }
        return cells.size();
    }

    private int preset(CommandSourceStack source, String name) {
        switch (name) {
            case "supercell" -> storm(source, StormType.SUPERCELL, 240, 0);
            case "ef4_wedge" -> {
                storm(source, StormType.SUPERCELL, 170, 0);
                tornado(source, 4, 95, 0);
            }
            case "rope_tornado" -> {
                storm(source, StormType.THUNDERSTORM, 150, 0);
                tornado(source, 1, 70, 0);
            }
            case "outbreak" -> {
                storm(source, StormType.SUPERCELL, 260, -180);
                storm(source, StormType.SUPERCELL, 300, 0);
                storm(source, StormType.SUPERCELL, 260, 180);
                tornado(source, 3, 110, -40);
                tornado(source, 2, 140, 60);
            }
            case "squall_line" -> {
                for (int i = -2; i <= 2; i++) storm(source, StormType.SQUALL, 200, i * 90);
            }
            case "cyclone" -> storm(source, StormType.CYCLONE, 380, 0);
            default -> {
                source.sendFailure(Component.literal("Unknown preset. Try: " + String.join(", ", PRESETS)));
                return 0;
            }
        }
        source.sendSuccess(() -> Component.literal("Started preset '" + name + "'."), true);
        return 1;
    }

    private int spawnStorm(CommandSourceStack source, String typeName) {
        StormType type;
        try {
            type = StormType.valueOf(typeName.toUpperCase());
        } catch (IllegalArgumentException ex) {
            source.sendFailure(Component.literal("Unknown storm type: " + typeName));
            return 0;
        }
        storm(source, type, 220, 0);
        source.sendSuccess(() -> Component.literal("Spawned a " + type.name() + " storm."), true);
        return 1;
    }

    private int spawnTornado(CommandSourceStack source, int ef) {
        if (tornado(source, ef, 45, 0) == null) return 0;
        source.sendSuccess(() -> Component.literal("Spawned an EF" + ef + " tornado."), true);
        return 1;
    }

    private int setWind(CommandSourceStack source, float speedMph, float degrees) {
        WeatherManager mgr = WeatherManager.get(source.getLevel());
        float speedMs = speedMph / 2.237f;
        double rad = Math.toRadians(degrees);
        mgr.windX = (float) (Math.sin(rad) * speedMs);
        mgr.windZ = (float) (-Math.cos(rad) * speedMs);
        source.sendSuccess(() -> Component.literal(String.format("Wind set to %.1f mph @ %.0f deg", speedMph, degrees)), true);
        return 1;
    }

    private int setAutoSpawn(CommandSourceStack source, boolean enabled) {
        TempestConfig.AUTO_SPAWN.set(enabled);
        source.sendSuccess(() -> Component.literal("Auto-spawn " + (enabled ? "enabled" : "disabled") + "."), true);
        return 1;
    }
}
