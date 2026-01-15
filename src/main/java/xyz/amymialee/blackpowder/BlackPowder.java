package xyz.amymialee.blackpowder;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;
import xyz.amymialee.blackpowder.items.GunItem;
import xyz.amymialee.blackpowder.registry.BlackPowderItems;
import xyz.amymialee.blackpowder.registry.BlackPowderSounds;
import net.minecraft.network.packet.CustomPayload;

public class BlackPowder implements ModInitializer {
    public static final String MOD_ID = "blackpowder";
    public static final Identifier clickConsume = id("click_consume");
    public static final Identifier fireParticle = id("fire_particle");

    @Override
    public void onInitialize() {
        BlackPowderItems.init();
        BlackPowderSounds.init();

        // Register the payload type for server-bound packets
        PayloadTypeRegistry.playC2S().register(ClickConsumePayload.ID, ClickConsumePayload.CODEC);
        
        // Register the server receiver
        ServerPlayNetworking.registerGlobalReceiver(ClickConsumePayload.ID, ClickConsumePayload::receive);
    }
    
    public record ClickConsumePayload(Vec3d pos, Vec3d rot) implements CustomPayload {
        public static final CustomPayload.Id<ClickConsumePayload> ID = new CustomPayload.Id<>(BlackPowder.clickConsume);
        public static final net.minecraft.network.codec.PacketCodec<net.minecraft.network.PacketByteBuf, ClickConsumePayload> CODEC = 
            CustomPayload.codecOf(ClickConsumePayload::write, ClickConsumePayload::new);
        
        public ClickConsumePayload(net.minecraft.network.PacketByteBuf buf) {
            this(buf.readVec3d(), buf.readVec3d());
        }
        
        public void write(net.minecraft.network.PacketByteBuf buf) {
            buf.writeVec3d(this.pos);
            buf.writeVec3d(this.rot);
        }
        
        public static void receive(ClickConsumePayload payload, net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking.Context context) {
            // 改进：添加简单的防抖动机制，防止过快连续点击
            var player = context.player();
            var stack = player.getMainHandStack();
            
            if (stack.getItem() instanceof GunItem gunItem) {
                // 检查玩家是否处于冷却状态，如果是则忽略请求
                if (player.getItemCooldownManager().isCoolingDown(stack)) {
                    return; // 防止过快的重复请求
                }
                
                // 在主线程上安全地执行枪械攻击逻辑
                context.server().execute(() -> {
                    if (player.getMainHandStack().getItem() instanceof GunItem currentGunItem) {  // 修改这里，使用不同的变量名
                        currentGunItem.blackPowder$doAttack(context.player(), payload.pos(), payload.rot());
                    }
                });
            }
        }
        
        @Override
        public Id<? extends CustomPayload> getId() {
            return ID;
        }
    }
    
    public record FireParticlePayload(int entityId) implements CustomPayload {
        public static final CustomPayload.Id<FireParticlePayload> ID = new CustomPayload.Id<>(BlackPowder.fireParticle);
        public static final net.minecraft.network.codec.PacketCodec<net.minecraft.network.PacketByteBuf, FireParticlePayload> CODEC = 
            CustomPayload.codecOf(FireParticlePayload::write, FireParticlePayload::new);
        
        public FireParticlePayload(net.minecraft.network.PacketByteBuf buf) {
            this(buf.readInt());
        }
        
        public void write(net.minecraft.network.PacketByteBuf buf) {
            buf.writeInt(this.entityId);
        }
        
        @Override
        public Id<? extends CustomPayload> getId() {
            return ID;
        }
    }

    public static Identifier id(String... path) {
        return Identifier.of(MOD_ID, String.join(".", path));
    }
}