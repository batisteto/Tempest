package com.batisteto.tempest.client.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.core.particles.SimpleParticleType;
import org.jetbrains.annotations.Nullable;

/** Swirling dust/debris used around tornado bases and violent wind gusts. */
public class DebrisParticle extends TextureSheetParticle {
    protected DebrisParticle(ClientLevel level, double x, double y, double z, double dx, double dy, double dz) {
        super(level, x, y, z, dx, dy, dz);
        this.gravity = 0.02f;
        this.lifetime = 20 + this.random.nextInt(15);
        this.quadSize = 0.25f + this.random.nextFloat() * 0.3f;
        float shade = 0.5f + this.random.nextFloat() * 0.25f;
        this.setColor(shade * 0.7f, shade * 0.6f, shade * 0.5f);
        this.setAlpha(0.65f);
        this.xd = dx;
        this.yd = dy;
        this.zd = dz;
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
            DebrisParticle particle = new DebrisParticle(level, x, y, z, dx, dy, dz);
            particle.pickSprite(sprites);
            return particle;
        }
    }
}
