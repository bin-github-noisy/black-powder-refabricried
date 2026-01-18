package xyz.amymialee.blackpowder.client;

import net.minecraft.client.model.ModelPart;
import net.minecraft.client.render.entity.model.BipedEntityModel;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Arm;
import net.minecraft.util.Hand;
import net.minecraft.util.math.MathHelper;
import org.joml.Quaternionf;
import xyz.amymialee.blackpowder.items.GunItem;
import xyz.amymialee.blackpowder.registry.BlackPowderItems;
import xyz.amymialee.blackpowder.util.GunHelper;

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

            // 基于总装填时间的比例计算装填阶段时间，延长动画持续时间
            int reloadDuration = gunItem.gunEntry.getReloadTime();
            int extendedReloadDuration = reloadDuration * 2; // 延长两倍时间
            float actualReloadProgress = Math.max(0, Math.min(1, reloadProgress)); // 确保进度在0-1之间
            int actualTimeElapsed = (int) (actualReloadProgress * extendedReloadDuration);

            int loadingStage1 = extendedReloadDuration / 6;      // 延长后装填时间的1/6
            int loadingStage2 = extendedReloadDuration / 3;     // 延长后装填时间的1/3
            int loadingStage3 = extendedReloadDuration * 2 / 3;  // 延长后装填时间的2/3

            // 减缓装填动画速度：延长动画持续时间，减小移动幅度
            int extendedWindow = 20; // 延长窗口时间以匹配更长的动画
            if ((actualTimeElapsed >= loadingStage1 && actualTimeElapsed <= loadingStage1 + extendedWindow) ||
                (actualTimeElapsed >= loadingStage2 && actualTimeElapsed <= loadingStage2 + extendedWindow) ||
                (actualTimeElapsed >= loadingStage3 && actualTimeElapsed <= loadingStage3 + extendedWindow)) {
                float t;
                int windowSize = extendedWindow; // 使用扩展的时间窗口大小
                if (actualTimeElapsed >= loadingStage1 && actualTimeElapsed <= loadingStage1 + windowSize) {
                    float stageProgress = (actualTimeElapsed - loadingStage1) / (float)windowSize; // 在该阶段内的进度
                    t = MathHelper.sin((float) Math.PI / 2 * MathHelper.sqrt(stageProgress));
                } else if (actualTimeElapsed >= loadingStage2 && actualTimeElapsed <= loadingStage2 + windowSize) {
                    float stageProgress = (actualTimeElapsed - loadingStage2) / (float)windowSize; // 在该阶段内的进度
                    t = MathHelper.sin((float) Math.PI / 2 * MathHelper.sqrt(stageProgress));
                } else if (actualTimeElapsed >= loadingStage3 && actualTimeElapsed <= loadingStage3 + windowSize) {
                    float stageProgress = (actualTimeElapsed - loadingStage3) / (float)windowSize; // 在该阶段内的进度
                    t = MathHelper.sin((float) Math.PI / 2 * MathHelper.sqrt(stageProgress));
                } else {
                    t = 0; // 不在关键阶段，无动画效果
                }
                matrices.translate(0.07f * t, 0.1, 0.7f * t);  // 进一步大幅减小移动幅度
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

        // 后座力效果：向后移动并稍微向下，降低幅度减少抽搐
        float recoilX = -recoilProgress * 0.05f;
        float recoilY = -recoilProgress * 0.025f;
        matrices.translate(armOffset * recoilX, recoilY, recoilProgress * 0.01f);

        // 微小的角度调整以增强后座力感，降低幅度减少抽搐
        matrices.multiply(new Quaternionf().rotationX((float) Math.toRadians(-recoilProgress * 1f)));
        matrices.multiply(new Quaternionf().rotationZ((float) Math.toRadians(armOffset * recoilProgress * 0.5f)));
    }

    /**
     * 应用第一人称视角下的动画效果
     * 包括装填和射击动画
     * 
     * @param matrices 矩阵栈
     * @param entity 实体
     * @param arm 使用的手臂
     * @param f 动画进度参数1
     * @param f1 动画进度参数2
     * @param delta 时间增量
     * @param reloading 是否正在装填
     * @param leftHanded 是否为左撇子
     * @param isFirstPerson 是否为第一人称
     */
    public static void applyFirstPersonAnimation(MatrixStack matrices, LivingEntity entity, Arm arm, float f, float f1, float delta, boolean reloading, boolean leftHanded, boolean isFirstPerson) {
        if (isFirstPerson) {             // 第一人称动画逻辑（基于1.21.1版本）             
            float sin = (float) Math.sin((f * 1 - 0.5) * Math.PI) * 0.3F + 0.5F;             // 降低频率和幅度
            float sin2 = (float) Math.sin((f1 * 1 - 0.5) * Math.PI) * 0.3F + 0.5F;           // 降低频率和幅度
            float sin3 = reloading ? sin2 : (float) Math.sin(1 - f);                           

            double d = (Math.sin(((float) entity.age + delta) / 4) * (reloading ? sin2 : f1)) * 15;  // 延长周期，降低幅度
            
            // 调整Z轴位置，减少模型过于靠前的问题             
            float zOffset = reloading ? 0 : (sin / 3 + f1 / 6) * 0.1F; // 进一步减少偏移量
            // 同时调整Y轴偏移来优化整体位置
            float yOffset = -0.025F;  // 减少偏移量
            matrices.translate(0, yOffset, zOffset);             
            matrices.multiply(net.minecraft.util.math.RotationAxis.POSITIVE_Z.rotationDegrees((float) (leftHanded ? -7.5 + d : 7.5 + d)));  // 减少旋转幅度
            matrices.multiply(net.minecraft.util.math.RotationAxis.POSITIVE_X.rotationDegrees((sin3 * 5) * 0.25F)); // 降低后坐力强度                      
        }
    }
    public static void applyFirstPersonReloadAnimation(MatrixStack matrices, LivingEntity entity, Arm arm, float reloadProgress, float entitySwingProgress, float delta, boolean isReloading, boolean isLeftHanded, boolean isFirstPerson) {
        if (isFirstPerson && isReloading) {
            // 计算装填阶段的动画参数
            float reloadSin = (float) Math.sin(reloadProgress * Math.PI); // 0到π的正弦波，用于平滑动画
            float reloadCos = (float) Math.cos(reloadProgress * Math.PI); // 余弦波，用于补充动画效果
            
            // 装填过程中的摆动效果，模拟手部动作，进一步降低频率和幅度避免抽搐
            float handShake = (float) Math.sin(reloadProgress * 1.5f) * 0.005f * reloadProgress; // 随着装填进行增加轻微震动，进一步降低频率和幅度避免抽搐
            
            // 根据装填进度执行不同的动画阶段，适度调整持续时间平衡流畅性和动作幅度
            float extendedReloadProgress = reloadProgress * 1.8f; // 适度延长总体动画时间
            float phase1 = Math.min(extendedReloadProgress * 1.3f, 1.0f); // 适度调整动画阶段
            float phase2 = Math.max(0, Math.min((extendedReloadProgress - 0.33f) * 1.3f, 1.0f)); // 适度调整动画阶段
            float phase3 = Math.max(0, Math.min((extendedReloadProgress - 0.66f) * 1.3f, 1.0f)); // 适度调整动画阶段
            
            // X轴偏移：装填初期向侧面移动，中期回中，后期推回，适度增加幅度
            float xOffset = (isLeftHanded ? -1 : 1) * (
                phase1 * 0.15f - // 初期向侧面拉开，适度增加幅度
                phase2 * 0.075f + // 中期小幅回中，适度增加幅度
                phase3 * 0.375f  // 后期轻微复位，适度增加幅度
            );
            
            // Y轴偏移：垂直方向的手部动作，适度增加幅度
            float yOffset = 
                phase1 * 0.6f +   // 初期上提，适度增加幅度
                phase2 * 0.15f + // 中期轻微上提，适度增加幅度
                phase3 * 0.225f;   // 后期上提，适度增加幅度
            
            // Z轴偏移：前后深度调整，模拟装填动作，适度增加幅度
            float zOffset = 
                -0.075f +           // 整体向后拉，适度增加幅度
                phase1 * 0.375f +  // 初期向前，适度增加幅度
                phase2 * 1.125f +  // 中期向后推（装弹动作），适度增加幅度
                phase3 * -0.075f;   // 后期向前复位，适度增加幅度
            
            // 应用位置变换
            matrices.translate(xOffset + handShake, yOffset, zOffset);
            
            // 旋转动画：模拟手部旋转装填动作，让枪口向上倾斜
            float xRotation = 
                phase1 * 1.125f +   // 初期顺时针旋转（枪口向上），适度增加幅度
                phase2 * 0.75f +    // 中期继续向上倾斜，适度增加幅度
                phase3 * 0.375f;     // 后期维持向上倾斜，适度增加幅度
            
            float yRotation = 
                phase1 * 0.375f +     // 初期轻微Y轴调整，适度增加幅度
                phase2 * -0.75f +   // 中期反向调整，适度增加幅度
                phase3 * 0.375f;      // 后期复位，适度增加幅度
            
            float zRotation = (isLeftHanded ? -1 : 1) * (
                phase1 * 1.5f -    // 初期Z轴旋转（打开枪膛），适度增加幅度
                phase2 * 1.125f +    // 中期反向旋转（装弹），适度增加幅度
                phase3 * 0.75f      // 后期复位（关闭枪膛），适度增加幅度
            );
            
            // 应用旋转变换
            matrices.multiply(net.minecraft.util.math.RotationAxis.POSITIVE_X.rotationDegrees(xRotation));
            matrices.multiply(net.minecraft.util.math.RotationAxis.POSITIVE_Y.rotationDegrees(yRotation));
            matrices.multiply(net.minecraft.util.math.RotationAxis.POSITIVE_Z.rotationDegrees(zRotation));
            
            // 添加细微的随机抖动，使动画更自然
        }
    }
    /**
     * 应用双臂动画到玩家模型
     * 
     * @param leftArm 左臂模型部分
     * @param rightArm 右臂模型部分
     * @param head 头部模型部分
     * @param entity 实体
     * @param cooldownProgress 冷却进度
     * @param gunTicks 枪械计时
     * @param gunState 枪械状态
     */
    public static void applyTwoArmsAnimation(ModelPart leftArm, ModelPart rightArm, ModelPart head, 
                                            LivingEntity entity, float cooldownProgress, int gunTicks, GunHelper.GunStates gunState) {         
        boolean isLeftHanded = entity.getMainArm().equals(Arm.LEFT);         
        ModelPart mainArm = isLeftHanded ? leftArm : rightArm;         
        ModelPart secondaryArm = isLeftHanded ? rightArm : leftArm;          

        float animationProgress = Math.max(cooldownProgress - 0.15F, 0);         
        boolean reloading = gunState.equals(GunHelper.GunStates.RELOADING);         
        float kick = 2.5F;          

        animationProgress = Math.max(animationProgress - 0.15F, 0);         
        float f = (((float)gunTicks/48) + animationProgress)/3; // 适度缩短动画周期，增加动作幅度
        float f1 = (float) (MathHelper.sin(f)/Math.PI) * (kick/4); // 适度增加振幅，平衡流畅性和动作幅度          

        float l = isLeftHanded ? -1 : 1;          

        // 获取实体角度（转换为弧度）         
        float p = entity.getPitch() * 0.01745329F;         
        float y = entity.getYaw() * 0.01745329F;         
        float bodyYaw = entity.getBodyYaw() * 0.01745329F;          

        float f2 = f1*kick/2.5f;          // 适度调整振幅

        float fr = ((float) MathHelper.sin((float) ((animationProgress * 1.8f - 0.5) * MathHelper.PI)) * 0.4F + 0.5F); // 适度调整频率和幅度
        float f3 = reloading ? fr/3 : f2/1.5f ;         // 适度增加幅度
        float f4 = reloading ? (isLeftHanded ? -fr/3 : fr/3) : f2 * l/1.5f;          // 适度增加幅度

        // 主手臂动画 - 相对于躯干         
        mainArm.yaw = isLeftHanded ? (y - bodyYaw) + 0.7853982F : (y - bodyYaw) - 0.7853982F;         
        mainArm.pitch = 0.2181662F + p + f3/1.8f;         // 适度增加幅度
        mainArm.roll += f4/1.5f;          // 适度增加幅度

        // 副手臂动画 - 相对于躯干         
        secondaryArm.pitch = -0.6981317F + p/3 - f3/1.8f - (reloading ? fr/1.5f:0);         // 适度增加幅度
        secondaryArm.yaw = (isLeftHanded ? -1.090831F - (y - bodyYaw) : 1.090831F + (y - bodyYaw)) + (p/2) * l + f3/2.5f; // 适度增加幅度          

        // 头部动画 - 相对于躯干         
        head.yaw = (y - bodyYaw) - 0.7853982F * l;         
        head.pitch = p; // 头部跟随实体俯仰角度     
    }
}