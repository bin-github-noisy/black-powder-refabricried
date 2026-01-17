package xyz.amymialee.blackpowder.client;

import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Arm;
import net.minecraft.util.Hand;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.MathHelper;
import org.joml.Quaternionf;
import xyz.amymialee.blackpowder.items.GunItem;
import xyz.amymialee.blackpowder.registry.BlackPowderItems;

/**
 * 处理枪械的通用动画逻辑，特别是装填动画
 */
public class GunAnimationHandler {
    
    /**
     * 应用装填动画变换到矩阵栈
     * 实现您提供的复杂装填动画效果
     *
     * @param matrices 矩阵栈
     * @param arm 使用的手臂
     * @param reloadProgress 装填进度 (0.0-1.0)
     * @param gunItem 枪械物品
     * @param swingProgress 挥动进度
     * @param stack 物品栈
     * @param hand 使用的手
     */
    public static void applyReloadAnimation(MatrixStack matrices, Arm arm, float reloadProgress, GunItem gunItem, float swingProgress, ItemStack stack, Hand hand) {
        int sign = (arm == Arm.RIGHT) ? 1 : -1;
        
        // 首先检查是否有挥动动画，如果有则应用挥动动画
        if (swingProgress > 0) {
            float swingSharp = MathHelper.sin(MathHelper.sqrt(swingProgress) * (float) Math.PI);
            float swingNormal = MathHelper.sin(swingProgress * (float) Math.PI);
            
            // 检查是否是特定类型的枪械，这里暂时注释掉具体比较，因为需要根据实际项目中的枪类型进行调整
            // if (gunItem == Items.MUSKET_WITH_BAYONET) {
            //     matrices.translate(sign * -0.05 * swingNormal, 0, 0.05 - 0.3 * swingSharp);
            //     matrices.multiply(new Quaternionf().rotationY((float) Math.toRadians(5 * swingSharp)));
            // } else {
                // 调整挥动动画参数，避免过度旋转或移出视野
                matrices.translate(sign * 0.02 * (1 - swingNormal), 0.01 * (1 - swingNormal), 0.02 - 0.1 * swingSharp);
                // 使用更温和的旋转，避免枪械完全翻转
                matrices.multiply(new Quaternionf().rotationX((float) Math.toRadians(20 + sign * 10 * (1 - swingSharp))));
            // }
        } else {
            // 没有挥动动画，检查是否处于装填状态
            // 这里的reloadProgress是装填进度，范围0.0-1.0
            
            // 根据装填进度计算枪管上仰角度，开始时装填进度为0，结束时为1
            float tiltAngle = 60.0f; // 开始时60度上仰，结束时0度
            
            // 应用位置调整，确保枪械保持在视野内
            matrices.translate(0, -0.05f, 0.05f);  // 微小的调整，避免枪械移出视野

            if (gunItem == BlackPowderItems.FLINTLOCK_PISTOL || gunItem == BlackPowderItems.BRASS_PISTOL) {
                 tiltAngle = 45.0f;
             }
            if (gunItem == BlackPowderItems.BRASS_SHOTGUN) {
                tiltAngle = 20.0f;
            }
            
            // 应用旋转：让枪管竖直向上
            matrices.multiply(new Quaternionf().rotationX((float) Math.toRadians(tiltAngle)));  // 根据装填进度调整旋转角度
            matrices.multiply(new Quaternionf().rotationZ((float) Math.toRadians(2)));   // 极小的旋转角度
            
            // 基于总装填时间的比例计算装填阶段时间
            int reloadDuration = gunItem.gunEntry.getReloadTime();
            float actualReloadProgress = Math.max(0, Math.min(1, reloadProgress)); // 确保进度在0-1之间
            int actualTimeElapsed = (int) (actualReloadProgress * reloadDuration);
            
            int loadingStage1 = reloadDuration / 6;      // 总装填时间的1/6
            int loadingStage2 = reloadDuration / 3;     // 总装填时间的1/3
            int loadingStage3 = reloadDuration * 2 / 3;  // 总装填时间的2/3
            
            // 减缓装填动画速度：延长动画持续时间，减小移动幅度
            if ((actualTimeElapsed >= loadingStage1 && actualTimeElapsed <= loadingStage1 + 10) ||
                (actualTimeElapsed >= loadingStage2 && actualTimeElapsed <= loadingStage2 + 10) ||
                (actualTimeElapsed >= loadingStage3 && actualTimeElapsed <= loadingStage3 + 10)) {
                float t;
                if (actualTimeElapsed >= loadingStage1 && actualTimeElapsed <= loadingStage1 + 10) {
                    float stageProgress = (actualTimeElapsed - loadingStage1) / 10.0f; // 在该阶段内的进度
                    t = MathHelper.sin((float) Math.PI / 2 * MathHelper.sqrt(stageProgress));
                } else if (actualTimeElapsed >= loadingStage2 && actualTimeElapsed <= loadingStage2 + 10) {
                    float stageProgress = (actualTimeElapsed - loadingStage2) / 10.0f; // 在该阶段内的进度
                    t = MathHelper.sin((float) Math.PI / 2 * MathHelper.sqrt(stageProgress));
                } else if (actualTimeElapsed >= loadingStage3 && actualTimeElapsed <= loadingStage3 + 10) {
                    float stageProgress = (actualTimeElapsed - loadingStage3) / 10.0f; // 在该阶段内的进度
                    t = MathHelper.sin((float) Math.PI / 2 * MathHelper.sqrt(stageProgress));
                } else {
                    t = 0; // 不在关键阶段，无动画效果
                }
                matrices.translate(0.003f * t, 0, 0.3f * t);  // 进一步大幅减小移动幅度
            }
            
            // 检查是否是特定类型的枪械，这里暂时注释掉，因为需要根据实际项目中的枪类型进行调整
            // if (gunItem == Items.PISTOL) {
            //     matrices.translate(0, 0, -0.12);
            // }
        }
    }
    
    /**
     * 适用于HeldItemRendererMixin的简化方法
     */
    public static void applyReloadAnimation(MatrixStack matrices, Arm arm, float reloadProgress, GunItem gunItem) {
        // 调用完整版本，传入默认值
        applyReloadAnimation(matrices, arm, reloadProgress, gunItem, 0.0f, ItemStack.EMPTY, null);
    }
    
    /**
     * 应用第三/第一人称射击后座力动画
     */
    public static void applyRecoilAnimation(MatrixStack matrices, Arm arm, float recoilProgress, GunItem gunItem) {
        if (recoilProgress <= 0) return;

        int armOffset = (arm == Arm.RIGHT) ? 1 : -1;

        // 后座力效果：向后移动并稍微向下
        float recoilX = -recoilProgress * 0.1f;
        float recoilY = -recoilProgress * 0.05f;
        matrices.translate(armOffset * recoilX, recoilY, recoilProgress * 0.02f);

        // 微小的角度调整以增强后座力感
        matrices.multiply(new Quaternionf().rotationX((float) Math.toRadians(-recoilProgress * 2f)));
        matrices.multiply(new Quaternionf().rotationZ((float) Math.toRadians(armOffset * recoilProgress * 1f)));
    }
}