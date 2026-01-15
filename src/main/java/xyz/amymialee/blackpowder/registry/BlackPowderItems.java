package xyz.amymialee.blackpowder.registry;

import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import xyz.amymialee.blackpowder.BlackPowder;
import xyz.amymialee.blackpowder.items.GunItem;

import java.util.ArrayList;

@SuppressWarnings("unused")
public class BlackPowderItems {
    public static final ArrayList<ArrayList<ItemStack>> MOD_ITEMS = new ArrayList<>();
    public static final ArrayList<ItemStack> ITEMS_MATERIALS = new ArrayList<>();
    public static final ArrayList<ItemStack> ITEMS_AMMO = new ArrayList<>();
    public static final ArrayList<ItemStack> ITEMS_GUNS = new ArrayList<>();
    public static final ArrayList<ItemStack> ITEMS_CREATIVE_GUNS = new ArrayList<>();
    
    // 将物品组改为静态变量，在init()方法中初始化
    public static ItemGroup BLACKPOWDER_GROUP;

    //Materials
    public static Item GRIP = registerItem("grip", new Item(createItemSettings("grip").component(DataComponentTypes.MAX_STACK_SIZE, 64)), ITEMS_MATERIALS);
    public static Item FIRING_MECHANISM = registerItem("firing_mechanism", new Item(createItemSettings("firing_mechanism").component(DataComponentTypes.MAX_STACK_SIZE, 64)), ITEMS_MATERIALS);
    public static Item BARREL = registerItem("barrel", new Item(createItemSettings("barrel").component(DataComponentTypes.MAX_STACK_SIZE, 64)), ITEMS_MATERIALS);
    public static Item RIFLED_BARREL = registerItem("rifled_barrel", new Item(createItemSettings("rifled_barrel").component(DataComponentTypes.MAX_STACK_SIZE, 64)), ITEMS_MATERIALS);
    //Ammo
    public static Item MUSKET_BALL = registerItem("musket_ball", new Item(createItemSettings("musket_ball").component(DataComponentTypes.MAX_STACK_SIZE, 64)), ITEMS_AMMO);
    public static Item BLUNDER_BALL = registerItem("blunder_ball", new Item(createItemSettings("blunder_ball").component(DataComponentTypes.MAX_STACK_SIZE, 64)), ITEMS_AMMO);
    //Guns
    public static Item BRASS_PISTOL = registerItem("brass_pistol", new GunItem(BlackPowderGunEntries.ENTRY_FLINTLOCK_PISTOL, createGunItemSettings("brass_pistol")), ITEMS_GUNS);
    public static Item FLINTLOCK_PISTOL = registerItem("flintlock_pistol", new GunItem(BlackPowderGunEntries.ENTRY_FLINTLOCK_PISTOL, createGunItemSettings("flintlock_pistol")), ITEMS_GUNS);
    public static Item BLUNDERBUSS = registerItem("blunderbuss", new GunItem(BlackPowderGunEntries.ENTRY_BLUNDERBUSS, createGunItemSettings("blunderbuss")), ITEMS_GUNS);
    public static Item BRASS_BLUNDERBUSS = registerItem("brass_blunderbuss", new GunItem(BlackPowderGunEntries.ENTRY_BLUNDERBUSS, createGunItemSettings("brass_blunderbuss")), ITEMS_GUNS);
    public static Item RIFLE = registerItem("rifle", new GunItem(BlackPowderGunEntries.ENTRY_RIFLE, createGunItemSettings("rifle")), ITEMS_GUNS);
    public static Item MUSKET = registerItem("musket", new GunItem(BlackPowderGunEntries.ENTRY_MUSKET, createGunItemSettings("musket")), ITEMS_GUNS);
    public static Item CHASSPOT_NEEDLE_GUN = registerItem("chesspot_needle_gun", new GunItem(BlackPowderGunEntries.ENTRY_CHASSPOT_NEEDLE_GUN, createGunItemSettings("chesspot_needle_gun")), ITEMS_GUNS);
    public static Item WERNDL_NEEDLE_GUN = registerItem("werndl_needle_gun", new GunItem(BlackPowderGunEntries.ENTRY_WERNDL_NEEDLE_GUN, createGunItemSettings("werndl_needle_gun")), ITEMS_GUNS);
    public static Item DREYSE_NEEDLE_GUN = registerItem("dreyse_needle_gun", new GunItem(BlackPowderGunEntries.ENTRY_DREYSE_NEEDLE_GUN, createGunItemSettings("dreyse_needle_gun")), ITEMS_GUNS);

//    public static Item FLINTLOCK_CARBINE = registerItem("flintlock_carbine", new GunItem(BlackPowderGunEntries.ENTRY_FLINTLOCK_CARBINE, createGunItemSettings("flintlock_carbine")), ITEMS_CREATIVE_GUNS);
//    public static Item BLUNDERBEHEMOTH = registerItem("blunderbehemoth", new GunItem(BlackPowderGunEntries.ENTRY_BLUNDERBEHEMOTH, createGunItemSettings("blunderbehemoth")), ITEMS_CREATIVE_GUNS);
//    public static Item RESOLUTE_RIFLE = registerItem("resolute_rifle", new GunItem(BlackPowderGunEntries.ENTRY_RESOLUTE_RIFLE, createGunItemSettings("resolute_rifle")), ITEMS_CREATIVE_GUNS);
//    public static Item BOUNDLESS_MUSKET = registerItem("boundless_musket", new GunItem(BlackPowderGunEntries.ENTRY_BOUNDLESS_MUSKET, createGunItemSettings("boundless_musket")), ITEMS_CREATIVE_GUNS);

    public static void init() {
        MOD_ITEMS.add(ITEMS_MATERIALS);
        MOD_ITEMS.add(ITEMS_AMMO);
        MOD_ITEMS.add(ITEMS_GUNS);
        MOD_ITEMS.add(ITEMS_CREATIVE_GUNS);
        
        // 在init()方法中初始化物品组，此时所有物品字段都已定义
        BLACKPOWDER_GROUP = FabricItemGroup.builder()
            .icon(() -> new ItemStack(FLINTLOCK_PISTOL))
            .displayName(Text.translatable("itemGroup.blackpowder.blackpowder_group"))
            .entries((displayContext, entries) -> {
                for (ArrayList<ItemStack> stacks : MOD_ITEMS) {
                    for (ItemStack stack : stacks) {
                        // 确保堆叠大小为1
                        if (stack.getCount() != 1) {
                            ItemStack singleStack = stack.copy();
                            singleStack.setCount(1);
                            entries.add(singleStack);
                        } else {
                            entries.add(stack);
                        }
                    }
                    // 添加空位来分隔不同的物品类别
                    for (int i = 0; i < 9 - (stacks.size() % 9); i++) {
                        // 修复：创建堆叠大小为1的空物品堆叠，并确保堆叠大小为1
                        ItemStack emptyStack = new ItemStack(Items.AIR, 1);
                        // 确保空堆叠的大小也为1
                        if (emptyStack.getCount() != 1) {
                            ItemStack singleEmptyStack = emptyStack.copy();
                            singleEmptyStack.setCount(1);
                        } else {
                            entries.add(emptyStack);
                        }
                    }
                }
            })
            .build();
        
        // 注册物品组
        Registry.register(Registries.ITEM_GROUP, Identifier.of("blackpowder", "blackpowder_group"), BLACKPOWDER_GROUP);
    }

    @SafeVarargs
    public static Item registerItem(String name, Item item, ArrayList<ItemStack> ... group) {
        Registry.register(Registries.ITEM, BlackPowder.id(name), item);
        for (ArrayList<ItemStack> list : group) {
            list.add(new ItemStack(item, 1));  // 修复：指定堆叠大小为1
        }
        return item;
    }

    // 创建普通物品的Settings，包含RegistryKey
    private static Item.Settings createItemSettings(String name) {
        Identifier itemId = BlackPowder.id(name);
        RegistryKey<Item> itemKey = RegistryKey.of(Registries.ITEM.getKey(), itemId);
        return new Item.Settings().registryKey(itemKey);
    }

    // 创建枪械物品的Settings，包含RegistryKey
    private static Item.Settings createGunItemSettings(String name) {
        Identifier itemId = BlackPowder.id(name);
        RegistryKey<Item> itemKey = RegistryKey.of(Registries.ITEM.getKey(), itemId);
        return new Item.Settings().registryKey(itemKey).maxCount(1).component(DataComponentTypes.MAX_STACK_SIZE, 1);
    }

    public static ItemStack getRecipeKindIcon() {
        return new ItemStack(FLINTLOCK_PISTOL);
    }
}