package xyz.amymialee.blackpowder.mixin.client;

import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.entity.PlayerEntityRenderer;
import net.minecraft.client.render.entity.model.BipedEntityModel;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Arm;
import net.minecraft.util.Hand;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import xyz.amymialee.blackpowder.items.GunItem;

@Mixin(PlayerEntityRenderer.class)
public class PlayerEntityRendererMixin {
    @Inject(method = "getArmPose*", at = @At("HEAD"), cancellable = true)
    private static void blackPowder$holdGun(AbstractClientPlayerEntity abstractClientPlayerEntity, Arm arm, CallbackInfoReturnable<BipedEntityModel.ArmPose> cir) {
        // 根据手臂获取对应的手
        Hand hand = (arm == Arm.LEFT) ? Hand.OFF_HAND : Hand.MAIN_HAND;
        ItemStack itemStack = abstractClientPlayerEntity.getStackInHand(hand);
        if (itemStack.getItem() instanceof GunItem && !abstractClientPlayerEntity.handSwinging) {
            if (GunItem.isCharged(itemStack)) {
                cir.setReturnValue(BipedEntityModel.ArmPose.CROSSBOW_HOLD);
            } else {
                // 修复：使用新的数据组件系统替代旧的 getOrCreateTag 方法
                var nbtComponent = itemStack.get(net.minecraft.component.DataComponentTypes.CUSTOM_DATA);
                var nbt = nbtComponent != null ? nbtComponent.copyNbt() : null;
                if (nbt != null && nbt.getBoolean("reloading").orElse(false)) {
                    cir.setReturnValue(BipedEntityModel.ArmPose.CROSSBOW_CHARGE);
                }
            }
        }
    }
}