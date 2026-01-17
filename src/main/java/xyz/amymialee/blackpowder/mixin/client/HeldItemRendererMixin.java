package xyz.amymialee.blackpowder.mixin.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.item.HeldItemRenderer;
import net.minecraft.client.render.item.ItemRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemDisplayContext;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Arm;
import net.minecraft.util.Hand;
import net.minecraft.util.math.MathHelper;
import org.joml.Quaternionf;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xyz.amymialee.blackpowder.client.GunAnimationHandler;
import xyz.amymialee.blackpowder.items.GunItem;

@Mixin(HeldItemRenderer.class)
public abstract class HeldItemRendererMixin {
    @Shadow protected abstract void applyEquipOffset(MatrixStack matrices, Arm arm, float equipProgress);
    @Shadow protected abstract void applySwingOffset(MatrixStack matrices, Arm arm, float swingProgress);
    
    // 移除不存在的renderItem方法声明
    // 在Minecraft 1.21.6中，HeldItemRenderer不再有renderItem方法

    @Inject(method = "renderFirstPersonItem", at = @At("HEAD"), cancellable = true)
    private void blackPowder$holdGunProperly(AbstractClientPlayerEntity player, float tickDelta, float pitch, Hand hand, float swingProgress, ItemStack stack, float equipProgress, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, CallbackInfo ci) {
        if (stack.getItem() instanceof GunItem gunItem) {
            matrices.push();
            // 修复：使用新的数据组件系统替代旧的NBT方法
            var nbtComponent = stack.get(net.minecraft.component.DataComponentTypes.CUSTOM_DATA);
            var nbt = nbtComponent != null ? nbtComponent.copyNbt() : null;
            boolean scoped = nbt != null && nbt.getBoolean("scoped").orElse(false);
            boolean mainHand = hand == Hand.MAIN_HAND;
            Arm arm = mainHand ? player.getMainArm() : player.getMainArm().getOpposite();
            boolean rightArm = arm == Arm.RIGHT;
            int armOffset = rightArm ? 1 : -1;
            // 修复：使用新的数据组件系统替代旧的NBT方法
            if (nbt != null && nbt.getBoolean("reloading").orElse(false)) {
                this.applyEquipOffset(matrices, arm, 0.0f);
                
                // 计算装填进度
                float timeRemaining = nbt.getInt("reloadProgress").orElse(0) - tickDelta + 1.0f;
                float reloadProgress = timeRemaining / gunItem.gunEntry.getReloadTime();
                if (reloadProgress > 1.0f) {
                    reloadProgress = 1.0f;
                }
                
                // 应用新的通用装填动画
                GunAnimationHandler.applyReloadAnimation(matrices, arm, reloadProgress, gunItem);
            } else {
                float f = -0.4f * MathHelper.sin(MathHelper.sqrt(swingProgress) * 3.1415927f);
                float g = 0.2f * MathHelper.sin(MathHelper.sqrt(swingProgress) * 6.2831855f);
                float h = -0.2f * MathHelper.sin(swingProgress * 3.1415927f);
                matrices.translate(armOffset * f, g, h);
                this.applyEquipOffset(matrices, arm, equipProgress);
                this.applySwingOffset(matrices, arm, swingProgress);
                if (scoped && swingProgress < 0.001f && mainHand) {
                    matrices.translate(armOffset * -0.641864f, 0.0, 0.0);
                    matrices.multiply(new Quaternionf().rotationY((float) armOffset * 10.0f * (float)Math.PI / 180));
                }
            }
            
            // 修复：使用更简单的ItemRenderer.renderItem方法
            ItemRenderer itemRenderer = MinecraftClient.getInstance().getItemRenderer();
            itemRenderer.renderItem(
                stack, 
                rightArm ? ItemDisplayContext.FIRST_PERSON_RIGHT_HAND : ItemDisplayContext.FIRST_PERSON_LEFT_HAND, 
                light, 
                0, 
                matrices, 
                vertexConsumers, 
                player.getWorld(), 
                0
            );
            
            matrices.pop();
            ci.cancel();
        }
    }
}