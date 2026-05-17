package com.xiaoliang.simukraft.client.preview;

import com.xiaoliang.simukraft.Simukraft;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraftforge.registries.ForgeRegistries;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Objects;

@SuppressWarnings("null")
public class SchematicNBTLoader {

    public record SchematicBlock(BlockPos pos, BlockState blockState) {
    }

    public record SchematicSize(int x, int y, int z) {
    }

    public static CompoundTag loadSchematicNBT(String filePath) throws IOException {
        File file = new File(filePath);
        if (!file.exists()) {
            throw new IOException(Component.translatable("message.preview.nbt_loader.file_not_found", filePath).getString());
        }

        try (FileInputStream fis = new FileInputStream(file)) {
            return NbtIo.readCompressed(fis);
        }
    }

    public static SchematicSize readSchematicSize(CompoundTag nbtData) {
        if (nbtData.contains("Schematic", 10)) {
            return readSchematicSize(nbtData.getCompound("Schematic"));
        }

        if (nbtData.contains("size", 9)) {
            ListTag size = nbtData.getList("size", 3);
            if (size.size() >= 3) {
                return new SchematicSize(Math.max(1, size.getInt(0)), Math.max(1, size.getInt(1)), Math.max(1, size.getInt(2)));
            }
        }

        if (nbtData.contains("Width") && nbtData.contains("Height") && nbtData.contains("Length")) {
            return new SchematicSize(Math.max(1, nbtData.getInt("Width")), Math.max(1, nbtData.getInt("Height")), Math.max(1, nbtData.getInt("Length")));
        }

        return null;
    }

    public static List<SchematicBlock> loadSchematicBlocks(String filePath) {
        File file = new File(filePath);
        if (!file.exists()) {
            Simukraft.LOGGER.error(Component.translatable("message.preview.nbt_loader.file_not_found", filePath).getString());
            return new ArrayList<>();
        }

        try (FileInputStream fis = new FileInputStream(file)) {
            CompoundTag nbtData = NbtIo.readCompressed(fis);
            Simukraft.LOGGER.info(Component.translatable("message.preview.nbt_loader.loading_file", filePath).getString());
            return parseStructureNBT(nbtData);
        } catch (IOException e) {
            Simukraft.LOGGER.error(Component.translatable("message.preview.nbt_loader.load_failed", e.getMessage()).getString());
            return new ArrayList<>();
        }
    }

    public static List<SchematicBlock> loadSchematicBlocksFromStream(InputStream inputStream) {
        try {
            CompoundTag nbtData = NbtIo.readCompressed(Objects.requireNonNull(inputStream));
            return parseStructureNBT(nbtData);
        } catch (IOException e) {
            Simukraft.LOGGER.error(Component.translatable("message.preview.nbt_loader.load_stream_failed", e.getMessage()).getString());
            return new ArrayList<>();
        }
    }

    public static List<SchematicBlock> parseStructureNBT(CompoundTag nbtData) {
        List<SchematicBlock> blocks = new ArrayList<>();

        Simukraft.LOGGER.info(Component.translatable("message.preview.nbt_loader.parse_start").getString());
        Simukraft.LOGGER.info(Component.translatable("message.preview.nbt_loader.nbt_keys", nbtData.getAllKeys()).getString());

        if (nbtData.contains("blocks", 9) && nbtData.contains("palette", 9)) {
            ListTag blocksList = nbtData.getList("blocks", 10);
            ListTag palette = nbtData.getList("palette", 10);

            Simukraft.LOGGER.info(Component.translatable("message.preview.nbt_loader.found_blocks", blocksList.size()).getString());
            Simukraft.LOGGER.info(Component.translatable("message.preview.nbt_loader.found_palette", palette.size()).getString());

            int validCount = 0;

            for (int i = 0; i < blocksList.size(); i++) {
                CompoundTag blockTag = blocksList.getCompound(i);

                try {
                    SchematicBlock block = parseSchematicBlock(blockTag, palette);
                    if (block != null) {
                        blocks.add(block);
                        validCount++;
                    } else {
                        Simukraft.LOGGER.warn(Component.translatable("message.preview.nbt_loader.state_parse_failed", blockTag).getString());
                    }
                } catch (Exception e) {
                    Simukraft.LOGGER.error(Component.translatable("message.preview.nbt_loader.block_parse_error", i, e.getMessage()).getString());
                }
            }

            Simukraft.LOGGER.info(Component.translatable("message.preview.nbt_loader.parse_success", validCount, blocksList.size()).getString());
        } else if (nbtData.contains("Schematic", 10)) {
            CompoundTag schematicTag = nbtData.getCompound("Schematic");
            Simukraft.LOGGER.info(Component.translatable("message.preview.nbt_loader.found_schematic_tag").getString());
            return parseStructureNBT(schematicTag);
        } else {
            Simukraft.LOGGER.error(Component.translatable("message.preview.nbt_loader.invalid_format").getString());
        }

        Simukraft.LOGGER.info(Component.translatable("message.preview.nbt_loader.parse_complete", blocks.size()).getString());
        return blocks;
    }

    public static SchematicBlock parseSchematicBlock(CompoundTag blockTag, ListTag palette) {
        BlockPos pos = readBlockPos(blockTag);
        BlockState state = readBlockState(blockTag, palette);
        return state != null ? new SchematicBlock(pos, state) : null;
    }

    private static BlockPos readBlockPos(CompoundTag blockTag) {
        int x = 0, y = 0, z = 0;

        if (blockTag.contains("x", 3)) {
            x = blockTag.getInt("x");
        } else if (blockTag.contains("posX", 3)) {
            x = blockTag.getInt("posX");
        } else if (blockTag.contains("X", 3)) {
            x = blockTag.getInt("X");
        }

        if (blockTag.contains("y", 3)) {
            y = blockTag.getInt("y");
        } else if (blockTag.contains("posY", 3)) {
            y = blockTag.getInt("posY");
        } else if (blockTag.contains("Y", 3)) {
            y = blockTag.getInt("Y");
        }

        if (blockTag.contains("z", 3)) {
            z = blockTag.getInt("z");
        } else if (blockTag.contains("posZ", 3)) {
            z = blockTag.getInt("posZ");
        } else if (blockTag.contains("Z", 3)) {
            z = blockTag.getInt("Z");
        }

        if (blockTag.contains("pos", 9)) {
            ListTag posList = blockTag.getList("pos", 3);
            x = posList.getInt(0);
            y = posList.getInt(1);
            z = posList.getInt(2);
        }

        return new BlockPos(x, y, z);
    }

    private static BlockState readBlockState(CompoundTag blockTag, ListTag palette) {
        try {
            if (blockTag.contains("state", 3) && !palette.isEmpty()) {
                int stateIndex = blockTag.getInt("state");
                if (stateIndex >= 0 && stateIndex < palette.size()) {
                    return parseBlockState(palette.getCompound(stateIndex));
                }
            } else if (blockTag.contains("state", 8)) {
                return parseBlockName(blockTag.getString("state"));
            } else if (blockTag.contains("nbt", 10)) {
                CompoundTag nbt = blockTag.getCompound("nbt");
                if (nbt.contains("id", 8)) {
                    return parseBlockName(nbt.getString("id"));
                }
            } else if (blockTag.contains("state", 10)) {
                return parseBlockState(blockTag.getCompound("state"));
            } else if (blockTag.contains("Name", 8)) {
                CompoundTag stateTag = new CompoundTag();
                stateTag.putString("Name", Objects.requireNonNull(blockTag.getString("Name")));
                if (blockTag.contains("Properties", 10)) {
                    stateTag.put("Properties", Objects.requireNonNull(blockTag.getCompound("Properties")));
                }
                return parseBlockState(stateTag);
            }
        } catch (Exception e) {
            Simukraft.LOGGER.error(Component.translatable("message.preview.nbt_loader.state_parse_error", e.getMessage()).getString());
        }
        return null;
    }

    private static BlockState parseBlockState(CompoundTag stateTag) {
        String blockName = stateTag.getString("Name");
        Block block = ForgeRegistries.BLOCKS.getValue(ResourceLocation.parse(blockName));

        if (block == null) {
            Simukraft.LOGGER.warn(Component.translatable("message.preview.nbt_loader.block_not_found", blockName).getString());
            return null;
        }

        BlockState blockState = block.defaultBlockState();

        if (stateTag.contains("Properties", 10)) {
            CompoundTag propertiesTag = stateTag.getCompound("Properties");
            for (String propertyName : propertiesTag.getAllKeys()) {
                Property<?> property = blockState.getBlock().getStateDefinition().getProperty(propertyName);
                if (property != null) {
                    String value = propertiesTag.getString(propertyName);
                    blockState = setValue(blockState, property, value);
                }
            }
        }

        return blockState;
    }

    private static BlockState parseBlockName(String blockName) {
        try {
            ResourceLocation blockId = ResourceLocation.parse(blockName);
            Block block = ForgeRegistries.BLOCKS.getValue(blockId);
            return block != null ? block.defaultBlockState() : null;
        } catch (Exception e) {
            Simukraft.LOGGER.error(Component.translatable("message.preview.nbt_loader.block_name_parse_error", blockName, e.getMessage()).getString());
            return null;
        }
    }

    private static <T extends Comparable<T>> BlockState setValue(BlockState blockState, Property<T> property, String value) {
        Optional<T> parsedValue = property.getValue(value);
        return parsedValue.map(v -> blockState.setValue(property, v)).orElse(blockState);
    }
}
