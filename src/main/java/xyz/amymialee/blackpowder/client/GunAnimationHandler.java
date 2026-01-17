package xyz.amymialee.blackpowder.client;

import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Arm;
import org.joml.Quaternionf;
import xyz.amymialee.blackpowder.items.GunItem;

/**
 * 处理枪械的通用动画逻辑，特别是装填动画
 */
public class GunAnimationHandler {

    /**
     * 平滑插值函数，使动画更流畅
     */
    private static float easeInOutQuad(float t) {
        return t < 0.5f ? 2.0f * t * t : -1.0f + (4.0f - 2.0f * t) * t;
    }
    
    /**
     * 应用装填动画变换到矩阵栈
     * 实现枪口90度上扬后上下摆动的效果
     *
     * @param matrices 矩阵栈
     * @param arm 使用的手臂
     * @param reloadProgress 装填进度 (0.0-1.0)
     * @param gunItem 枪械物品
     */
    public static void applyReloadAnimation(MatrixStack matrices, Arm arm, float reloadProgress, GunItem gunItem) {
        int armOffset = (arm == Arm.RIGHT) ? 1 : -1;

        // 初始位置偏移
        matrices.translate(armOffset * -0.4785682f, -0.0943870022892952f, 0.05731530860066414f);

        // 计算上扬和摆动进度
        // 将整个动画分为两部分：前半部分上扬，后半部分摆动
        float upswingProgress = Math.min(reloadProgress * 2.0f, 1.0f); // 前半段用于上扬
        float swayProgress = Math.max(0.0f, (reloadProgress - 0.5f) * 2.0f); // 后半段用于摆动

        // 使用平滑插值函数使动画更流畅
        float smoothUpswing = easeInOutQuad(upswingProgress);
        float smoothSway = easeInOutQuad(swayProgress);

        // 应用上扬旋转：枪口向上抬升，枪托向下，绕X轴旋转（这是枪管方向）
        float upswingAngle = smoothUpswing * 70f; // 从0到70度，减少最大角度以避免过度旋转
        matrices.multiply(new Quaternionf().rotationX((float) Math.toRadians(upswingAngle)));

        // 应用水平旋转 (轻微的Z轴旋转，让枪更自然地抬起)
//        matrices.multiply(new Quaternionf().rotationZ((float) Math.toRadians(armOffset * smoothUpswing * 10f)));

        // 在摆动阶段添加更流畅的上下摆动效果
        if (smoothSway > 0) {
            // 使用更平滑的函数来减少抽搐，降低摆动频率和幅度
            float swayAmount = (float) (Math.sin(smoothSway * Math.PI * 10) * 2f * (1.0f - smoothSway * 0.5f)); // 2个周期，较小幅度，逐渐减弱
            matrices.multiply(new Quaternionf().rotationY((float) Math.toRadians(swayAmount * 0.7f))); // 更小的旋转角度
            
            // 添加平滑的位置变化来增强真实感，减少幅度
            float positionSway = (float) (Math.sin(smoothSway * Math.PI * 1.5) * 0.005f * (1.0f - smoothSway * 0.3f));
            matrices.translate(0.0f, positionSway * 0.5f, -positionSway * 0.2f);
        }

        // 根据装填进度调整缩放，模拟装填过程中握持的变化
        float scaleAdjust = 1.0f - (reloadProgress * 0.05f);
        matrices.scale(scaleAdjust, scaleAdjust, scaleAdjust);
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