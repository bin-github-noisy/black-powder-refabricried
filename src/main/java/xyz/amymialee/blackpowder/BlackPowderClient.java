package xyz.amymialee.blackpowder;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.entity.Entity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.util.math.Vec3d;

import java.util.Objects;

@Environment(EnvType.CLIENT)
public class BlackPowderClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        // Register the payload type for client-bound packets
        PayloadTypeRegistry.playS2C().register(BlackPowder.FireParticlePayload.ID, BlackPowder.FireParticlePayload.CODEC);
        
        ClientPlayNetworking.registerGlobalReceiver(BlackPowder.FireParticlePayload.ID, BlackPowderClient::receiveFireParticle);
    }
    
    private static void receiveFireParticle(BlackPowder.FireParticlePayload payload, ClientPlayNetworking.Context context) {
        var client = context.client();
        // 安全检查：确保客户端和世界对象存在
        if (client.world == null) {
            return;
        }
        
        Entity entity = client.world.getEntityById(payload.entityId());
        // 优化：减少粒子数量以降低性能影响，仅在主线程执行
        client.execute(() -> {
            if (entity != null && client.world != null) {
                Vec3d pos = entity.getEyePos();
                Vec3d rot = entity.getRotationVector();
                // 进一步减少粒子数量，从4个减少到2个，以减轻渲染压力
                for (int i = 0; i < 2; i++) {
                    if (client.world != null) {
                        client.world.addParticleClient(ParticleTypes.SMOKE, pos.x, pos.y, pos.z, 
                            rot.x * ((client.world.random.nextFloat() * 0.1f) + 0.05f), 
                            rot.y * ((client.world.random.nextFloat() * 0.1f) + 0.05f), 
                            rot.z * ((client.world.random.nextFloat() * 0.1f) + 0.05f));
                    }
                }
            }
        });
    }
}