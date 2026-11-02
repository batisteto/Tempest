package com.batisteto.tempest.client.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.core.particles.SimpleParticleType;
import org.jetbrains.annotations.Nullable;

/** Small, fast-falling icy chunk with a slight bounce - used for dense hail cores. */
public class HailParticle extends TextureSheetParticle {
    protected HailParticle(ClientLevel level, double x, double y, double z, double dx, double dy, double dz) {
        super(level, x, y, z, dx, dy, dz);
        this.gravity = 1.1f;
        this.lifetime = 40 + this.random.nextInt(25);
        this.quadSize = 0.2f + this.random.nextFloat() * 0.1f;
        this.setColor(0.92f, 0.95f, 1.0f);
        this.xd = dx;
        this.yd = dy - 0.6;
        this.zd = dz;
        this.hasPhysics = true;
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
            HailParticle particle = new HailParticle(level, x, y, z, dx, dy, dz);
            particle.pickSprite(sprites);
            return particle;
        }
    }
}
