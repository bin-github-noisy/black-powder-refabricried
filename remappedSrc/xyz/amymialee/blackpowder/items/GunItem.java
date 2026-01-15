package xyz.amymialee.blackpowder.items;

import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.block.BlockState;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.boss.dragon.EnderDragonPart;
import net.minecraft.entity.passive.TameableEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.ProjectileUtil;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import xyz.amymialee.blackpowder.BlackPowder;
import xyz.amymialee.blackpowder.util.GunEntry;
import xyz.amymialee.blackpowder.util.IClickConsumingItem;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class GunItem extends Item implements IClickConsumingItem {
    public final GunEntry gunEntry;

    public GunItem(GunEntry gunEntry, Item.Settings settings) {
        super(settings.maxCount(1));
        this.gunEntry = gunEntry;
    }

    @Override
    public void blackPowder$doAttack(ServerPlayerEntity player, Vec3d pos, Vec3d rot) {
        ItemStack stack = player.getMainHandStack();
        if (!(player.getWorld() instanceof ServerWorld serverWorld) || player.getItemCooldownManager().isCoolingDown(this) || !this.hasAmmo(stack)) {
            return;
        }
        if (this.gunEntry.getFireSound() != null) {
            serverWorld.playSoundFromEntity(null, player, this.gunEntry.getFireSound(), player.getSoundCategory(), 1.0F, 1.0F);
        }

        // 弩箭式检测：创建虚拟投射物进行检测
        Map<LivingEntity, Float> hitEntities = new HashMap<>();

        // 模拟弩箭的投射物检测逻辑
        Vec3d startPos = player.getEyePos();
        Vec3d direction = player.getRotationVec(1.0f);
        double maxDistance = 4096.0; // 最大检测距离

        // 创建检测区域（类似弩箭的碰撞箱）
        Box detectionBox = player.getBoundingBox().stretch(direction.multiply(maxDistance)).expand(1.0, 1.0, 1.0);

        // 获取可能被命中的实体列表
        List<Entity> potentialTargets = serverWorld.getOtherEntities(player, detectionBox,
            entity -> this.isValidTarget(player, entity));

        // 对每个弹丸进行检测（模拟霰弹效果）
        for (int i = 0; i < this.gunEntry.getPelletCount(); i++) {
            Vec3d pelletDirection = direction;

            // 添加弹丸散布（第一个弹丸精确，后续弹丸有散布）
            if (i != 0) {
                pelletDirection = direction.rotateX(player.getRandom().nextFloat() * this.gunEntry.getBulletSpread());
                pelletDirection = pelletDirection.rotateY(player.getRandom().nextFloat() * this.gunEntry.getBulletSpread());
                pelletDirection = pelletDirection.rotateZ(player.getRandom().nextFloat() * this.gunEntry.getBulletSpread());
            }

            // 计算弹丸终点
            Vec3d endPos = startPos.add(pelletDirection.multiply(maxDistance));

            // 使用ProjectileUtil进行投射物检测（与弩箭相同的方式）
            EntityHitResult entityHitResult = ProjectileUtil.raycast(player, startPos, endPos,
                detectionBox, entity -> this.isValidTarget(player, entity), maxDistance);

            if (entityHitResult != null) {
                Entity entity = entityHitResult.getEntity();
                // 处理末影龙部件
                if (entity instanceof EnderDragonPart part) {
                    entity = part.owner;
                }

                if (entity instanceof LivingEntity livingEntity) {
                    // 计算距离
                    double distance = entityHitResult.getPos().distanceTo(startPos);
                    // 计算伤害（包含距离衰减）
                    float damage = this.gunEntry.getDamage(distance);

                    // 累加伤害（多个弹丸命中同一实体）
                    if (hitEntities.containsKey(livingEntity)) {
                        damage += hitEntities.get(livingEntity);
                    }
                    hitEntities.put(livingEntity, damage);

                    // 播放命中效果
                    Vec3d hitPos = entityHitResult.getPos();
                    serverWorld.spawnParticles(ParticleTypes.SMOKE, hitPos.x, hitPos.y, hitPos.z, 1, 0.0D, 0.0D, 0.0D, 0.0D);
                    if (this.gunEntry.getHitSound() != null) {
                        serverWorld.playSound(hitPos.x, hitPos.y, hitPos.z, this.gunEntry.getHitSound(),
                            player.getSoundCategory(), 1f, 0.75f + player.getRandom().nextFloat() * 0.5f, true);
                    }
                }
            }
        }

        // 应用伤害（与弩箭相同的方式）
        float total = 0f;
        for (Map.Entry<LivingEntity, Float> entry : hitEntities.entrySet()) {
            // 重置伤害免疫时间
            entry.getKey().timeUntilRegen = 0;
            // 应用伤害
            entry.getKey().damage(this.gunEntry.getSource(player), entry.getValue());
            total += entry.getValue();
        }

        // 消耗弹药
        this.consumeAmmo(stack);
        // 设置冷却时间
        player.getItemCooldownManager().set(stack.getItem(), this.gunEntry.getFireRate());

        // 播放命中反馈音效
        if (total > 0) {
            float averageDamage = Math.max(1, this.gunEntry.getDamage() * this.gunEntry.getPelletCount());
            float pitch = (total / averageDamage) * 0.45f;
            serverWorld.playSoundFromEntity(null, player, SoundEvents.ENTITY_ARROW_HIT_PLAYER,
                player.getSoundCategory(), 1.0F, pitch);
        }

        // 发送粒子效果数据包
        // 在 Minecraft 1.21.1 中，使用新的 CustomPayload 系统
        for(int j = 0; j < serverWorld.getPlayers().size(); j++) {
            ServerPlayerEntity targetPlayer = serverWorld.getPlayers().get(j);
            // 发送 FireParticlePayload 到客户端
            ServerPlayNetworking.send(targetPlayer, new BlackPowder.FireParticlePayload(player.getId()));
        }
    }

    @Override
    public net.minecraft.util.ActionResult use(World world, PlayerEntity user, Hand hand) {
        ItemStack stack = user.getStackInHand(hand);
        if (world instanceof ServerWorld) {
            if (!user.isSneaking()) {
                // 检查是否已在装弹状态
                NbtCompound nbt = getOrCreateNbt(stack);
                if (nbt.contains("reloading") && nbt.getBoolean("reloading")) {
                    // 如果已在装弹状态，则不允许重新开始装弹
                    return net.minecraft.util.ActionResult.FAIL;
                }
                if (this.isFullyLoaded(stack)) {
                    user.getItemCooldownManager().set(stack.getItem(), 2);
                    return net.minecraft.util.ActionResult.FAIL;
                }
                if (this.getAmmoStack(user).isEmpty()) {
                    return net.minecraft.util.ActionResult.FAIL;
                }
                nbt.putBoolean("reloading", true);
                nbt.putInt("reloadProgress", 0);
                // 确保创建新的NbtComponent来强制更新
                NbtComponent startReloadNbtComponent = NbtComponent.of(nbt);
                stack.set(DataComponentTypes.CUSTOM_DATA, startReloadNbtComponent);
                // 触发一个短暂的冷却以强制客户端刷新状态
                user.getItemCooldownManager().set(stack.getItem(), 1);
            } else if (this.gunEntry.hasScope()) {
                boolean isScoped = this.shouldScope(stack);
                NbtCompound nbt = getOrCreateNbt(stack);
                nbt.putBoolean("scoped", !isScoped);
                // 确保创建新的NbtComponent来强制更新
                NbtComponent newNbtComponent = NbtComponent.of(nbt);
                stack.set(DataComponentTypes.CUSTOM_DATA, newNbtComponent);
                if (isScoped) {
                    user.playSound(SoundEvents.ITEM_SPYGLASS_USE, 1.0F, 1.0F);
                } else {
                    user.playSound(SoundEvents.ITEM_SPYGLASS_STOP_USING, 1.0F, 1.0F);
                }
            }
        }
        return net.minecraft.util.ActionResult.SUCCESS;
    }

    @Override
    public void inventoryTick(ItemStack stack, World world, Entity entity, int slot, boolean selected) {
        if (world instanceof ServerWorld serverWorld && entity instanceof ServerPlayerEntity player) {
            // 每次都重新获取最新的NBT数据，确保状态是最新的
            NbtCompound nbt = getOrCreateNbt(stack);
            boolean isCurrentlyReloading = nbt.getBoolean("reloading");
            
            if (isCurrentlyReloading) {
                ItemStack ammoStack = this.getAmmoStack(player);
                if (ammoStack.isEmpty()) {
                    // 没有弹药时取消装弹
                    nbt.putBoolean("reloading", false);
                    nbt.putInt("reloadProgress", 0);
                    // 确保创建新的NbtComponent来强制更新
                    NbtComponent stopReloadNbtComponent = NbtComponent.of(nbt);
                    stack.set(DataComponentTypes.CUSTOM_DATA, stopReloadNbtComponent);
                    return;
                }
                
                if (selected) {
                    // 只有在选中状态下才增加进度
                    int currentProgress = nbt.getInt("reloadProgress");
                    int nextProgress = currentProgress + 1;
                    
                    // 检查是否达到装弹完成条件
                    if (nextProgress >= this.gunEntry.getReloadTime()) {
                        // 装弹完成逻辑
                        int currentAmmo = this.getAmmo(stack);
                        int numToLoad = Math.min(this.gunEntry.getAmmoCount() - currentAmmo, ammoStack.getCount());
                        
                        // 一次性更新所有状态
                        NbtCompound updatedNbt = getOrCreateNbt(stack);
                        if (numToLoad > 0) {
                            int newAmmo = currentAmmo + numToLoad;
                            updatedNbt.putInt("ammo", newAmmo);
                            if (!player.isCreative()) ammoStack.decrement(numToLoad);
                            if (this.gunEntry.getReloadSoundEnd() != null) {
                                serverWorld.playSoundFromEntity(null, player, this.gunEntry.getReloadSoundEnd(), player.getSoundCategory(), 1.0F, 1.0F);
                            }
                        }
                        
                        // 结束装弹过程
                        updatedNbt.putBoolean("reloading", false);
                        updatedNbt.putInt("reloadProgress", 0);
                        
                        // 确保创建新的NbtComponent来强制更新
                        NbtComponent finishReloadNbtComponent = NbtComponent.of(updatedNbt);
                        stack.set(DataComponentTypes.CUSTOM_DATA, finishReloadNbtComponent);
                        
                        // 触发玩家的物品冷却以强制刷新客户端模型状态
                        player.getItemCooldownManager().set(stack.getItem(), 1);
                    } else {
                        // 装弹未完成，更新进度并播放音效
                        nbt.putInt("reloadProgress", nextProgress);
                        
                        // 检查是否需要播放中间音效
                        if (nextProgress == 1) {
                            if (this.gunEntry.getReloadSoundStart() != null) {
                                serverWorld.playSoundFromEntity(null, player, this.gunEntry.getReloadSoundStart(), player.getSoundCategory(), 1.0F, 1.0F);
                            }
                        } else if (nextProgress == this.gunEntry.getReloadTime() / 2) {
                            if (this.gunEntry.getReloadSoundMiddle() != null) {
                                serverWorld.playSoundFromEntity(null, player, this.gunEntry.getReloadSoundMiddle(), player.getSoundCategory(), 1.0F, 1.0F);
                            }
                        }
                        
                        // 更新进度后保存状态
                        NbtComponent reloadNbtComponent = NbtComponent.of(nbt);
                        stack.set(DataComponentTypes.CUSTOM_DATA, reloadNbtComponent);
                    }
                } else {
                    // 当不在选中槽位时，检查是否已经积累了足够的进度来完成装弹
                    // 如果当前进度已经满足完成条件，也应该完成装弹
                    int currentProgress = nbt.getInt("reloadProgress");
                    if (currentProgress >= this.gunEntry.getReloadTime()) {
                        // 即使未选中，如果进度已满，也要完成装弹
                        int currentAmmo = this.getAmmo(stack);
                        ItemStack unselectedAmmoStack = this.getAmmoStack(player); // 重新获取弹药堆
                        int numToLoad = Math.min(this.gunEntry.getAmmoCount() - currentAmmo, unselectedAmmoStack.getCount());
                        
                        // 一次性更新所有状态
                        NbtCompound updatedNbt = getOrCreateNbt(stack);
                        if (numToLoad > 0) {
                            int newAmmo = currentAmmo + numToLoad;
                            updatedNbt.putInt("ammo", newAmmo);
                            if (!player.isCreative()) unselectedAmmoStack.decrement(numToLoad);
                        }
                        
                        // 结束装弹过程
                        updatedNbt.putBoolean("reloading", false);
                        updatedNbt.putInt("reloadProgress", 0);
                        
                        // 确保创建新的NbtComponent来强制更新
                        NbtComponent finishReloadNbtComponent = NbtComponent.of(updatedNbt);
                        stack.set(DataComponentTypes.CUSTOM_DATA, finishReloadNbtComponent);
                        
                        // 触发玩家的物品冷却以强制刷新客户端模型状态
                        player.getItemCooldownManager().set(stack.getItem(), 1);
                    }
                    // 注意：不取消装弹或重置进度，保持当前状态等待玩家切换回来
                }
            }

            if (nbt.getBoolean("scoped")) {
                if (!selected) {
                    nbt.putBoolean("scoped", false);
                    // 确保创建新的NbtComponent来强制更新
                    NbtComponent scopedNbtComponent = NbtComponent.of(nbt);
                    stack.set(DataComponentTypes.CUSTOM_DATA, scopedNbtComponent);
                }
            }
        }
        super.inventoryTick(stack, world, entity, slot, selected);
    }

    public boolean shouldScope(ItemStack stack) {
        NbtCompound nbt = getOrCreateNbt(stack);
        return nbt.getBoolean("scoped") && !nbt.getBoolean("reloading");
    }

    private boolean addAmmo(ItemStack stack, int amount) {
        NbtCompound nbt = getOrCreateNbt(stack);
        int current = nbt.getInt("ammo");
        if (current + amount > this.gunEntry.getAmmoCount()) {
            return false;
        }
        nbt.putInt("ammo", current + amount);
        // 确保创建新的NbtComponent来强制更新
        NbtComponent addAmmoNbtComponent = NbtComponent.of(nbt);
        stack.set(DataComponentTypes.CUSTOM_DATA, addAmmoNbtComponent);
        return true;
    }

    private void consumeAmmo(ItemStack stack) {
        NbtCompound nbt = getOrCreateNbt(stack);
        int newAmmo = Math.max(0, nbt.getInt("ammo") - 1);
        nbt.putInt("ammo", newAmmo);
        // 确保创建新的NbtComponent来强制更新
        NbtComponent consumeAmmoNbtComponent = NbtComponent.of(nbt);
        stack.set(DataComponentTypes.CUSTOM_DATA, consumeAmmoNbtComponent);
    }

    private int getAmmo(ItemStack stack) {
        NbtCompound nbt = getOrCreateNbt(stack);
        return nbt.getInt("ammo");
    }
    

    // 辅助方法：获取或创建NBT数据
    private static NbtCompound getOrCreateNbt(ItemStack stack) {
        NbtComponent nbtComponent = stack.get(DataComponentTypes.CUSTOM_DATA);
        NbtCompound nbt;
        if (nbtComponent != null) {
            nbt = nbtComponent.copyNbt();
        } else {
            nbt = new NbtCompound();
        }
        
        // 确保ammo字段存在，如果不存在则初始化为0
        if (!nbt.contains("ammo")) {
            nbt.putInt("ammo", 0);
        }
        
        return nbt;
    }

    private ItemStack getAmmoStack(PlayerEntity player) {
        for(int i = 0; i < player.getInventory().size(); ++i) {
            ItemStack itemStack = player.getInventory().getStack(i);
            if (this.gunEntry.getAmmoPredicate().test(itemStack)) {
                return itemStack;
            }
        }
        return ItemStack.EMPTY;
    }

    private boolean isFullyLoaded(ItemStack stack) {
        return this.getAmmo(stack) >= this.gunEntry.getAmmoCount();
    }

    private boolean hasAmmo(ItemStack stack) {
        return this.getAmmo(stack) > 0;
    }

    @Override
    public boolean isItemBarVisible(ItemStack stack) {
        return true;
    }

@Override
    public int getItemBarStep(ItemStack stack) {
        NbtComponent nbtComponent = stack.get(DataComponentTypes.CUSTOM_DATA);
        if (nbtComponent == null) {
            // 没有自定义数据的物品，返回默认值
            return (int) (14 * ((float) 0 / this.gunEntry.getAmmoCount()));
        }
        NbtCompound nbt = nbtComponent.copyNbt();
        if (nbt.getBoolean("reloading")) {      
            return (int) (14 * ((float) nbt.getInt("reloadProgress") / this.gunEntry.getReloadTime()));
        } else {
            return (int) (14 * ((float) this.getAmmo(stack) / this.gunEntry.getAmmoCount()));
        }
    }

    @Override
    public int getItemBarColor(ItemStack stack) {
        if (this.isFullyLoaded(stack)) {
            return 6711039;
        }
        NbtComponent nbtComponent = stack.get(DataComponentTypes.CUSTOM_DATA);
        if (nbtComponent != null && nbtComponent.copyNbt().getBoolean("reloading")) {
            return 16737894;
        }
        return 6750054;
    }

    public boolean isValidTarget(LivingEntity attacker, Entity target) {
        if (target == null) return true;
        if (attacker == null) return true;
        if (target == attacker) return true;
        if (target.getWorld() != attacker.getWorld()) return true;

        // 增强兼容性：允许攻击任何活着的实体，包括瓦尔基里模组中的特殊实体
        if (target instanceof LivingEntity livingEntity) {
            // 检查实体是否有效且未被移除
            if (livingEntity.isDead() || target.isRemoved()) return true;

            // 检查是否为队友或驯服生物（可选，根据需求调整）
            if (attacker instanceof PlayerEntity player) {
                if (target instanceof TameableEntity tameable && tameable.isTamed()) {
                    // 如果是驯服生物且主人是玩家自己，则不攻击
                    return tameable.getOwner() != player;
                }
            }

            return true;
        }

        // 允许攻击其他特殊实体类型
        return target instanceof EnderDragonPart;
    }

    public static boolean isCharged(ItemStack itemStack) {
        NbtComponent nbtComponent = itemStack.get(DataComponentTypes.CUSTOM_DATA);
        if (nbtComponent == null) {
            return false; // 没有自定义数据的物品默认不是已装弹状态
        }
        NbtCompound nbt = nbtComponent.copyNbt();
        int ammo = nbt.getInt("ammo");
        return ammo > 0 && !nbt.getBoolean("reloading");
    }

    @Override
    public boolean canMine(BlockState state, World world, BlockPos pos, PlayerEntity miner) {
        return false;
    }

    @Override
    public Text getName(ItemStack stack) {
        Text text = super.getName(stack);
        List<Text> list = text.getWithStyle(text.getStyle().withColor(this.gunEntry.getTextColor()));
        if (!list.isEmpty()) {
            return list.get(0);
        }
        return text;
    }


    @Override
    public boolean hasGlint(ItemStack stack) {
        return this.gunEntry.hasGlint();
    }
}