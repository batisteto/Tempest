package com.batisteto.tempest.weather;

import com.batisteto.tempest.config.TempestConfig;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Server-authoritative weather state for one level: active storm cells + ambient wind.
 * Active cells are intentionally NOT persisted across a server restart (weather is
 * ephemeral, same philosophy vanilla uses) - only the ambient wind carries over.
 */
public class WeatherManager extends SavedData {
    public static final SavedData.Factory<WeatherManager> FACTORY =
            new SavedData.Factory<>(WeatherManager::new, WeatherManager::load, null);

    private final List<StormCell> cells = new ArrayList<>();

    public float windX;
    public float windZ;
    private float windTargetX;
    private float windTargetZ;
    private int windRetargetCooldown;
    private int autoSpawnTicks;

    private boolean vanillaWeatherLocked;

    public static WeatherManager get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(FACTORY, "tempest_weather");
    }

    public static WeatherManager load(CompoundTag tag, HolderLookup.Provider provider) {
        WeatherManager mgr = new WeatherManager();
        mgr.windX = tag.getFloat("windX");
        mgr.windZ = tag.getFloat("windZ");
        return mgr;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider provider) {
        tag.putFloat("windX", windX);
        tag.putFloat("windZ", windZ);
        return tag;
    }

    public List<StormCell> getCells() {
        return cells;
    }

    public void tick(ServerLevel level) {
        RandomSource random = level.random;
        tickWind(random);

        Iterator<StormCell> it = cells.iterator();
        while (it.hasNext()) {
            StormCell cell = it.next();
            cell.tick(level, random);
            if (cell.dead) {
                it.remove();
            }
        }
        lockVanillaWeatherClear(level);

        if (TempestConfig.AUTO_SPAWN.get()) {
            autoSpawnTicks++;
            if (autoSpawnTicks >= TempestConfig.AUTO_SPAWN_CHECK_TICKS.get()) {
                autoSpawnTicks = 0;
                if (cells.size() < TempestConfig.MAX_ACTIVE_CELLS.get()
                        && random.nextDouble() < TempestConfig.AUTO_SPAWN_CHANCE.get()) {
                    spawnRandomStorm(level, random);
                }
            }
        }
        setDirty();
    }

    private void tickWind(RandomSource random) {
        if (windRetargetCooldown-- <= 0) {
            windRetargetCooldown = 400 + random.nextInt(800);
            double angle = random.nextDouble() * Math.PI * 2;
            float speed = 1f + random.nextFloat() * 8f;
            windTargetX = (float) (Math.cos(angle) * speed);
            windTargetZ = (float) (Math.sin(angle) * speed);
        }
        windX += (windTargetX - windX) * 0.005f;
        windZ += (windTargetZ - windZ) * 0.005f;
    }

    /**
     * Vanilla has no concept of localized weather - its rain/thunder flags are dimension-wide,
     * so leaving them on would rain on every player the instant any storm cell exists anywhere
     * on the map. Tempest storms are a fully separate, localized system (clouds, rain-streak
     * and hail particles, wind), so vanilla's own weather cycle is force-locked clear here and
     * never used as a visual.
     */
    private void lockVanillaWeatherClear(ServerLevel level) {
        if (!vanillaWeatherLocked) {
            level.setWeatherParameters(Integer.MAX_VALUE, 0, false, false);
            vanillaWeatherLocked = true;
        }
    }

    public void spawnRandomStorm(ServerLevel level, RandomSource random) {
        List<ServerPlayer> players = level.players();
        if (players.isEmpty()) return;
        ServerPlayer anchor = players.get(random.nextInt(players.size()));
        double angle = random.nextDouble() * Math.PI * 2;
        double dist = 300 + random.nextDouble() * 400;
        double sx = anchor.getX() + Math.cos(angle) * dist;
        double sz = anchor.getZ() + Math.sin(angle) * dist;
        spawnStorm(level, random, rollStormType(random), sx, sz, anchor.getX(), anchor.getZ());
    }

    public StormCell spawnStorm(ServerLevel level, RandomSource random, StormType type,
                                 double sx, double sz, double towardX, double towardZ) {
        double dx = towardX - sx;
        double dz = towardZ - sz;
        double len = Math.max(1.0, Math.sqrt(dx * dx + dz * dz));
        float speed = 0.15f + random.nextFloat() * 0.15f;
        float mx = (float) (dx / len * speed) + windX * 0.02f;
        float mz = (float) (dz / len * speed) + windZ * 0.02f;
        StormCell cell = new StormCell(type, sx, sz, mx, mz, random);
        cells.add(cell);
        setDirty();
        return cell;
    }

    private StormType rollStormType(RandomSource random) {
        float r = random.nextFloat();
        if (r < 0.45f) return StormType.RAIN;
        if (r < 0.65f) return StormType.SQUALL;
        if (r < 0.85f) return StormType.THUNDERSTORM;
        if (r < 0.97f) return StormType.SUPERCELL;
        return StormType.CYCLONE;
    }

    public void clear(ServerLevel level) {
        cells.clear();
        setDirty();
    }

    public List<StormCellData> networkSnapshot() {
        List<StormCellData> list = new ArrayList<>(cells.size());
        for (StormCell cell : cells) list.add(cell.toNetworkData());
        return list;
    }
}
