package arekkuusu.enderskills.client.render.skill;

import arekkuusu.enderskills.client.proxy.ClientProxy;
import arekkuusu.enderskills.common.entity.throwable.EntityThrowableData;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.Vec3d;

import javax.annotation.Nullable;

public class ProjectileFire extends Render<EntityThrowableData> {

    protected ProjectileFire(RenderManager renderManager) {
        super(renderManager);
    }

    @Override
    public void doRender(EntityThrowableData entity, double x, double y, double z, float entityYaw, float partialTicks) {
        for (int i = 0; i < 4; i++) {
            if (entity.world.rand.nextDouble() < 0.6D && ClientProxy.canParticleSpawn()) {
                Vec3d vec = entity.getPositionEyes(1F);
                Vec3d motion = new Vec3d(entity.prevPosX, entity.prevPosY + entity.getEyeHeight(), entity.prevPosZ).subtract(vec);
                double offset = entity.world.rand.nextDouble();
                double posX = vec.x + (entity.width / 2) * (entity.world.rand.nextDouble() - 0.5) + motion.x * offset;
                double posY = vec.y + (entity.height / 2) * (entity.world.rand.nextDouble() - 0.5) + motion.y * offset;
                double posZ = vec.z + (entity.width / 2) * (entity.world.rand.nextDouble() - 0.5) + motion.z * offset;
                motion = new Vec3d(0, 0, 0);
                entity.world.spawnParticle(EnumParticleTypes.FLAME, posX, posY, posZ, motion.x, motion.y, motion.z);
            }
        }
    }

    @Override
    @Nullable
    protected ResourceLocation getEntityTexture(EntityThrowableData entity) {
        return null;
    }
}
