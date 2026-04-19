package com.xiaoliang.simukraft.client;

import com.xiaoliang.simukraft.Simukraft;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.resources.ResourceLocation;

@SuppressWarnings("null")
public class ModModelLayers {
    public static final ModelLayerLocation CUSTOM_ENTITY = 
            new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath(Simukraft.MOD_ID, "custom_entity"), "main");
    
    public static final ModelLayerLocation FLOATING_BUILD_BOX = 
            new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath(Simukraft.MOD_ID, "floating_build_box"), "main");
}
