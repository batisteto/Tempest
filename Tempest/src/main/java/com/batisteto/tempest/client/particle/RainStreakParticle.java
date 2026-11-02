package com.batisteto.tempest.client.particle;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.Mth;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * Long thin rain streak, stretched along its own velocity (so wind-slanted rain is drawn as slanted
 * lines, not round dots) and removed the moment it passes below the surface (roofs keep it dry).
 */
public class RainStreakParticle extends TextureSheetParticle {
    private final float streakLength;
    private final float streakWidth;

    protected RainStreakParticle(ClientLevel level, double x, double y, double z, double dx, double dy, double dz) {
        super(level, x, y, z, dx, dy, dz);
        this.gravity = 0.0f;
        this.friction = 1.0f;
        this.lifetime = 24 + this.random.nextInt(10);
        this.setColor(1f, 1f, 1f);
        this.setAlpha(0.55f);
        this.xd = dx;
        this.yd = dy;
        this.zd = dz;
        this.hasPhysics = false;
        this.streakLength = 2.4f + this.random.nextFloat() * 1.4f;
        this.streakWidth = 0.022f + this.random.nextFloat() * 0.012f;
    }

    @Override
    public void tick() {
        super.tick();
        if (this.isAlive()) {
            int gx = Mth.floor(this.x);
            int gz = Mth.floor(this.z);
            if (this.y < this.level.getHeight(Heightmap.Types.MOTION_BLOCKING, gx, gz)) {
                this.remove();
            }
        }
    }

    @Override
    public void render(VertexConsumer buffer, Camera camera, float partialTicks) {
        Vec3 cam = camera.getPosition();
        float x = (float) (Mth.lerp((double) partialTicks, this.xo, this.x) - cam.x());
        float y = (float) (Mth.lerp((double) partialTicks, this.yo, this.y) - cam.y());
        float z = (float) (Mth.lerp((double) partialTicks, this.zo, this.z) - cam.z());

        float ax = (float) this.xd, ay = (float) this.yd, az = (float) this.zd;
        float al = Mth.sqrt(ax * ax + ay * ay + az * az);
        if (al < 1.0e-4f) return;
        float dx = ax / al * this.streakLength;
        float dy = ay / al * this.streakLength;
        float dz = az / al * this.streakLength;

        // head leads (along velocity), tail trails behind it
        float hx = x, hy = y, hz = z;
        float tx = x - dx, ty = y - dy, tz = z - dz;

        // width direction: perpendicular to both the streak axis and the view direction
        float mx = (hx + tx) * 0.5f, my = (hy + ty) * 0.5f, mz = (hz + tz) * 0.5f;
        float cx = dy * (-mz) - dz * (-my);
        float cy = dz * (-mx) - dx * (-mz);
        float cz = dx * (-my) - dy * (-mx);
        float cl = Mth.sqrt(cx * cx + cy * cy + cz * cz);
        if (cl < 1.0e-5f) return;
        float k = this.streakWidth / cl;
        float rx = cx * k, ry = cy * k, rz = cz * k;

        float u0 = this.getU0(), u1 = this.getU1(), v0 = this.getV0(), v1 = this.getV1();
        int light = this.getLightColor(partialTicks);

        buffer.addVertex(tx - rx, ty - ry, tz - rz).setUv(u0, v1).setColor(this.rCol, this.gCol, this.bCol, this.alpha).setLight(light);
        buffer.addVertex(tx + rx, ty + ry, tz + rz).setUv(u1, v1).setColor(this.rCol, this.gCol, this.bCol, this.alpha).setLight(light);
        buffer.addVertex(hx + rx, hy + ry, hz + rz).setUv(u1, v0).setColor(this.rCol, this.gCol, this.bCol, this.alpha).setLight(light);
        buffer.addVertex(hx - rx, hy - ry, hz - rz).setUv(u0, v0).setColor(this.rCol, this.gCol, this.bCol, this.alpha).setLight(light);
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }

    public static class Provider implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprites;

        public Provider(SpriteSet sprites) {
            this.sprites = sprites;
        }

        @Nullable
        @Override
        public Particle createParticle(SimpleParticleType type, ClientLevel level, double x, double y, double z,
                                        double dx, double dy, double dz) {
            RainStreakParticle particle = new RainStreakParticle(level, x, y, z, dx, dy, dz);
            particle.pickSprite(sprites);
            return particle;
        }
    }
}
