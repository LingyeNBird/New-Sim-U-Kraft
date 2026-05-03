package com.xiaoliang.simukraft.init;

import com.xiaoliang.simukraft.Simukraft;
import com.xiaoliang.simukraft.inventory.WarehouseGridMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/**
 * 菜单类型注册
 */
public class ModMenus {
    public static final DeferredRegister<MenuType<?>> MENUS =
            DeferredRegister.create(ForgeRegistries.MENU_TYPES, Simukraft.MOD_ID);

    // 仓库网格菜单
    public static final RegistryObject<MenuType<WarehouseGridMenu>> WAREHOUSE_GRID_MENU = MENUS.register("warehouse_grid_menu",
            () -> IForgeMenuType.create((windowId, inv, data) -> {
                // 读取仓库位置
                var pos = data.readBlockPos();
                return new WarehouseGridMenu(windowId, inv, pos);
            }));
}
