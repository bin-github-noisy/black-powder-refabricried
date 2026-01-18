package xyz.amymialee.blackpowder.mixin.client;

import net.minecraft.client.model.ModelPart;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.entity.model.BipedEntityModel;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xyz.amymialee.blackpowder.items.GunItem;
import xyz.amymialee.blackpowder.util.GunHelper;

@Mixin(BipedEntityModel.class)
public class BipedEntityModelMixin<T extends LivingEntity> {
    @Final
    @Shadow public ModelPart leftArm;
    @Final
    @Shadow public ModelPart rightArm;
    @Final
    @Shadow public ModelPart head;

    /**
     * 在设置模型角度时注入双臂动画逻辑
     */
    @Inject(method = "setAngles*", at = @At("TAIL"), allow = 1)
    private void blackPowder$applyTwoArmsAnimation(T livingEntity, float limbAngle, float limbDistance, float animationProgress, CallbackInfo ci) {
        // 检查是否为玩家实体且持有枪械
        if (livingEntity instanceof AbstractClientPlayerEntity player) {
            ItemStack mainHandStack = player.getMainHandStack();
            ItemStack offHandStack = player.getOffHandStack();
            
            boolean hasMainGun = mainHandStack.getItem() instanceof GunItem;
            boolean hasOffGun = offHandStack.getItem() instanceof GunItem;
            
            // 确定当前状态
            GunHelper.GunStates gunState = GunHelper.GunStates.IDLE;
            float cooldownProgress = 0.0f;
            int gunTicks = 0;
            
            // 检查是否正在装填
            var nbtComponent = mainHandStack.get(net.minecraft.component.DataComponentTypes.CUSTOM_DATA);
            var nbt = nbtComponent != null ? nbtComponent.copyNbt() : null;
            boolean isReloading = nbt != null && nbt.getBoolean("reloading").orElse(false);
            
            if (isReloading) {
                gunState = GunHelper.GunStates.RELOADING;
                // 设置冷却进度和枪械计时
                cooldownProgress = 1.0f - (float)nbt.getInt("reloadProgress").orElse(0) / ((GunItem) mainHandStack.getItem()).gunEntry.getReloadTime();
                gunTicks = nbt.getInt("reloadProgress").orElse(0);
            } else if (player.isUsingItem() && hasMainGun) {
                // 如果正在使用枪械（例如瞄准）
                gunState = GunHelper.GunStates.AIMING;
            }
            
            // 如果玩家持有一把或多把枪，则应用双臂动画
            if (hasMainGun || hasOffGun) {
                // 调用新的双臂动画方法
                xyz.amymialee.blackpowder.client.GunAnimationHandler.applyTwoArmsAnimation(
                    this.leftArm, 
                    this.rightArm, 
                    this.head, 
                    livingEntity, 
                    cooldownProgress, 
                    gunTicks, 
                    gunState
                );
            }
        }
    }
}