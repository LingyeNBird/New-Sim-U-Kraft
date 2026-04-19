package com.xiaoliang.simukraft.utils;

import com.xiaoliang.simukraft.Simukraft;
import com.xiaoliang.simukraft.entity.Gender;
import net.minecraft.resources.ResourceLocation;

import java.util.*;

/**
 * NPC皮肤管理器
 * 负责管理男性和女性皮肤的加载和随机分配
 * 确保60个男性和60个女性皮肤都能正确读取和随机分配
 * 实现智能分配算法，减少皮肤重复
 */
public class SkinManager {

    // 男性皮肤数量
    public static final int MALE_SKIN_COUNT = 60;
    // 女性皮肤数量
    public static final int FEMALE_SKIN_COUNT = 60;

    // 男性皮肤路径模板
    private static final String MALE_SKIN_TEMPLATE = "entity/male/custom_male_entity_%d";
    // 女性皮肤路径模板
    private static final String FEMALE_SKIN_TEMPLATE = "entity/female/custom_female_entity_%d";

    // 默认男性皮肤
    private static final String DEFAULT_MALE_SKIN = "entity/male/custom_male_entity_0";
    // 默认女性皮肤
    private static final String DEFAULT_FEMALE_SKIN = "entity/female/custom_female_entity_0";

    // 每个性别的可用皮肤索引列表（用于确保随机不重复，直到用完所有皮肤）
    private static final Map<Gender, List<Integer>> availableSkinIndices = new EnumMap<>(Gender.class);

    // 每个性别已使用的皮肤索引集合
    private static final Map<Gender, Set<Integer>> usedSkinIndices = new EnumMap<>(Gender.class);

    // 每个性别的使用计数器（用于轮换）
    private static final Map<Gender, Integer> skinRotationCounter = new EnumMap<>(Gender.class);

    // 初始化
    static {
        initializeSkinIndices();
    }

    /**
     * 初始化皮肤索引列表
     */
    private static void initializeSkinIndices() {
        // 男性皮肤索引 0-59
        List<Integer> maleIndices = new ArrayList<>();
        for (int i = 0; i < MALE_SKIN_COUNT; i++) {
            maleIndices.add(i);
        }
        availableSkinIndices.put(Gender.MALE, maleIndices);
        usedSkinIndices.put(Gender.MALE, new HashSet<>());
        skinRotationCounter.put(Gender.MALE, 0);

        // 女性皮肤索引 0-59
        List<Integer> femaleIndices = new ArrayList<>();
        for (int i = 0; i < FEMALE_SKIN_COUNT; i++) {
            femaleIndices.add(i);
        }
        availableSkinIndices.put(Gender.FEMALE, femaleIndices);
        usedSkinIndices.put(Gender.FEMALE, new HashSet<>());
        skinRotationCounter.put(Gender.FEMALE, 0);
    }

    /**
     * 获取随机皮肤路径 - 使用智能分配算法减少重复
     * 策略：
     * 1. 优先使用未使用过的皮肤
     * 2. 当所有皮肤都使用过后，打乱顺序重新分配
     * 3. 使用UUID作为随机种子确保同一NPC总是获得相同皮肤
     *
     * @param gender 性别
     * @param random 随机数生成器
     * @param uuid   NPC的UUID，用于确保唯一性和一致性
     * @return 皮肤路径
     */
    public static String getRandomSkinPath(Gender gender, Random random, UUID uuid) {
        if (gender == null) {
            gender = Gender.getRandom();
        }

        // 使用UUID作为种子创建确定性随机，确保同一UUID总是获得相同结果
        Random uuidRandom = new Random(uuid.getMostSignificantBits() ^ uuid.getLeastSignificantBits());

        Set<Integer> used = usedSkinIndices.get(gender);
        List<Integer> available = availableSkinIndices.get(gender);

        int skinIndex;

        // 如果还有未使用的皮肤，优先使用
        if (used.size() < getSkinCount(gender)) {
            // 获取所有未使用的皮肤索引
            List<Integer> unused = new ArrayList<>();
            for (int i = 0; i < getSkinCount(gender); i++) {
                if (!used.contains(i)) {
                    unused.add(i);
                }
            }

            // 从未使用的皮肤中随机选择一个
            skinIndex = unused.get(uuidRandom.nextInt(unused.size()));
            used.add(skinIndex);
        } else {
            // 所有皮肤都已使用，重置并重新分配
            used.clear();

            // 打乱可用皮肤顺序
            Collections.shuffle(available, uuidRandom);

            // 选择第一个
            skinIndex = available.get(0);
            used.add(skinIndex);
        }

        return getSkinPath(gender, skinIndex);
    }

    /**
     * 获取轮换皮肤路径 - 确保每个皮肤都被均匀使用
     * 适用于需要严格控制皮肤分配均匀性的场景
     *
     * @param gender 性别
     * @return 皮肤路径
     */
    public static String getRotatedSkinPath(Gender gender) {
        if (gender == null) {
            gender = Gender.getRandom();
        }

        int counter = skinRotationCounter.get(gender);
        int skinIndex = counter % getSkinCount(gender);

        skinRotationCounter.put(gender, counter + 1);

        // 记录已使用
        usedSkinIndices.get(gender).add(skinIndex);

        return getSkinPath(gender, skinIndex);
    }

    /**
     * 获取纯随机皮肤路径（可能重复，但使用UUID种子确保一致性）
     *
     * @param gender 性别
     * @param uuid   NPC的UUID
     * @return 皮肤路径
     */
    public static String getPureRandomSkinPath(Gender gender, UUID uuid) {
        if (gender == null) {
            gender = Gender.getRandom();
        }

        // 使用UUID作为种子确保同一NPC总是获得相同皮肤
        Random uuidRandom = new Random(uuid.getMostSignificantBits() ^ uuid.getLeastSignificantBits());
        int skinIndex = uuidRandom.nextInt(getSkinCount(gender));

        return getSkinPath(gender, skinIndex);
    }

    /**
     * 获取指定性别和索引的皮肤路径
     *
     * @param gender 性别
     * @param index  皮肤索引 (0-59)
     * @return 皮肤路径
     */
    public static String getSkinPath(Gender gender, int index) {
        if (gender == Gender.MALE) {
            int validIndex = Math.max(0, Math.min(index, MALE_SKIN_COUNT - 1));
            return String.format(MALE_SKIN_TEMPLATE, validIndex);
        } else {
            int validIndex = Math.max(0, Math.min(index, FEMALE_SKIN_COUNT - 1));
            return String.format(FEMALE_SKIN_TEMPLATE, validIndex);
        }
    }

    /**
     * 获取默认皮肤路径
     *
     * @param gender 性别
     * @return 默认皮肤路径
     */
    public static String getDefaultSkinPath(Gender gender) {
        return gender == Gender.MALE ? DEFAULT_MALE_SKIN : DEFAULT_FEMALE_SKIN;
    }

    /**
     * 获取指定性别的皮肤数量
     *
     * @param gender 性别
     * @return 皮肤数量
     */
    public static int getSkinCount(Gender gender) {
        return gender == Gender.MALE ? MALE_SKIN_COUNT : FEMALE_SKIN_COUNT;
    }

    /**
     * 构建完整的纹理资源位置
     *
     * @param skinPath 皮肤路径
     * @return 资源位置
     */
    public static ResourceLocation getTextureResourceLocation(String skinPath) {
        if (skinPath == null || skinPath.isEmpty()) {
            return null;
        }
        return ResourceLocation.fromNamespaceAndPath(Simukraft.MOD_ID, "textures/" + skinPath + ".png");
    }

    /**
     * 验证皮肤路径是否有效
     *
     * @param skinPath 皮肤路径
     * @return 是否有效
     */
    public static boolean isValidSkinPath(String skinPath) {
        if (skinPath == null || skinPath.isEmpty()) {
            return false;
        }

        // 检查路径格式是否正确
        if (skinPath.contains("entity/male/")) {
            try {
                String numberPart = skinPath.substring(skinPath.lastIndexOf('_') + 1);
                int index = Integer.parseInt(numberPart);
                return index >= 0 && index < MALE_SKIN_COUNT;
            } catch (NumberFormatException e) {
                return false;
            }
        } else if (skinPath.contains("entity/female/")) {
            try {
                String numberPart = skinPath.substring(skinPath.lastIndexOf('_') + 1);
                int index = Integer.parseInt(numberPart);
                return index >= 0 && index < FEMALE_SKIN_COUNT;
            } catch (NumberFormatException e) {
                return false;
            }
        }

        return false;
    }

    /**
     * 从皮肤路径解析性别
     *
     * @param skinPath 皮肤路径
     * @return 性别，如果无法解析则返回null
     */
    public static Gender getGenderFromSkinPath(String skinPath) {
        if (skinPath == null || skinPath.isEmpty()) {
            return null;
        }

        if (skinPath.contains("entity/male/")) {
            return Gender.MALE;
        } else if (skinPath.contains("entity/female/")) {
            return Gender.FEMALE;
        }

        return null;
    }

    /**
     * 从皮肤路径解析皮肤索引
     *
     * @param skinPath 皮肤路径
     * @return 皮肤索引，如果无法解析则返回-1
     */
    public static int getSkinIndexFromPath(String skinPath) {
        if (skinPath == null || skinPath.isEmpty()) {
            return -1;
        }

        try {
            String numberPart = skinPath.substring(skinPath.lastIndexOf('_') + 1);
            return Integer.parseInt(numberPart);
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    /**
     * 重置已使用的皮肤记录（用于新世界或调试）
     */
    public static void resetUsedSkins() {
        usedSkinIndices.get(Gender.MALE).clear();
        usedSkinIndices.get(Gender.FEMALE).clear();
        skinRotationCounter.put(Gender.MALE, 0);
        skinRotationCounter.put(Gender.FEMALE, 0);
        initializeSkinIndices();
    }

    /**
     * 获取已使用的皮肤数量
     *
     * @param gender 性别
     * @return 已使用的皮肤数量
     */
    public static int getUsedSkinCount(Gender gender) {
        return usedSkinIndices.get(gender).size();
    }

    /**
     * 获取所有可用的男性皮肤路径
     *
     * @return 男性皮肤路径列表
     */
    public static List<String> getAllMaleSkinPaths() {
        List<String> paths = new ArrayList<>();
        for (int i = 0; i < MALE_SKIN_COUNT; i++) {
            paths.add(String.format(MALE_SKIN_TEMPLATE, i));
        }
        return paths;
    }

    /**
     * 获取所有可用的女性皮肤路径
     *
     * @return 女性皮肤路径列表
     */
    public static List<String> getAllFemaleSkinPaths() {
        List<String> paths = new ArrayList<>();
        for (int i = 0; i < FEMALE_SKIN_COUNT; i++) {
            paths.add(String.format(FEMALE_SKIN_TEMPLATE, i));
        }
        return paths;
    }
}
