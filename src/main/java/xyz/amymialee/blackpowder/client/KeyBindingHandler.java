package xyz.amymialee.blackpowder.client;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.item.ItemStack;
import org.lwjgl.glfw.GLFW;
import xyz.amymialee.blackpowder.items.GunItem;

public class KeyBindingHandler {
    private static KeyBinding reloadKeyBinding;
    private static KeyBinding rotateModelKeyBinding;
    
    // 旋转动画状态变量
    private static boolean isRotating = false;
    private static float rotationProgress = 0.0f; // 0.0 to 1.0
    private static final float ROTATION_SPEED = 0.05f; // 控制旋转速度

    public static void init() {
        // 注册R键作为重新装弹按键
        reloadKeyBinding = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.blackpowder.reload", // 翻译键
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_R, // R键
                "category.blackpowder.keys" // 键位类别
        ));

        // 注册H键作为模型旋转按键
        rotateModelKeyBinding = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.blackpowder.rotate_model", // 翻译键
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_H, // H键
                "category.blackpowder.keys" // 键位类别
        ));
        
        // 注册客户端每刻(tick)事件处理器
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (reloadKeyBinding.wasPressed()) {
                onReloadKeyPressed(client);
            }
            
            while (rotateModelKeyBinding.wasPressed()) {
                onRotateModelKeyPressed(client);
            }
            
            // 更新旋转动画进度
            if (isRotating) {
                rotationProgress += ROTATION_SPEED;
                if (rotationProgress >= 1.0f) {
                    rotationProgress = 0.0f;
                    isRotating = false;
                }
            }
        });
    }

    private static void onReloadKeyPressed(MinecraftClient client) {
        if (client.player != null && client.player.getMainHandStack() != null) {
            ItemStack stack = client.player.getMainHandStack();
            if (stack.getItem() instanceof GunItem) {
                // 触发装弹逻辑 - 右键点击效果
                client.options.useKey.setPressed(true);
                client.interactionManager.interactItem(client.player, client.player.getActiveHand());
                client.options.useKey.setPressed(false);
            }
        }
    }
    
    private static void onRotateModelKeyPressed(MinecraftClient client) {
        if (client.player != null && client.player.getMainHandStack() != null) {
            ItemStack stack = client.player.getMainHandStack();
            if (stack.getItem() instanceof GunItem) {
                // 开始旋转动画
                if (!isRotating) {  // 防止重复触发
                    isRotating = true;
                    rotationProgress = 0.0f;
                }
            }
        }
    }
    
    public static float getRotationProgress() {
        return rotationProgress;
    }
    
    public static boolean isRotating() {
        return isRotating;
    }
}