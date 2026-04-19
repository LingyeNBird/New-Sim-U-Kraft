package com.xiaoliang.simukraft.utils;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.storage.DimensionDataStorage;

import javax.annotation.Nonnull;
import java.util.*;

public class NameManager extends SavedData {
    private static final String DATA_NAME = "simukraft_names";
    private final Set<String> usedFullNames = new HashSet<>();
    private final Map<UUID, String> entityNames = new HashMap<>();
    private int nextNPCId = 1;
    //姓
    private static final String[] SURNAMES = {
            "王", "李", "张", "刘", "陈", "杨", "黄", "赵", "欧阳", "诸葛",
            "周", "吴", "徐", "孙", "朱", "马", "胡", "郭", "林", "何",
            "高", "郑", "罗", "梁", "谢", "宋", "唐", "许", "韩", "冯",
            "邓", "曹", "彭", "曾", "萧", "田", "董", "袁", "潘", "于",
            // 新增姓氏 （2026/1/1）
            "蒋", "蔡", "魏", "薛", "叶", "阎", "余", "杜", "夏", "钟",
            "汪", "田", "任", "姜", "范", "方", "石", "姚", "谭", "廖",
            "邹", "熊", "金", "陆", "郝", "孔", "白", "崔", "康", "毛",
            "邱", "秦", "江", "史", "顾", "侯", "邵", "孟", "龙", "万",
            "段", "雷", "钱", "汤", "尹", "黎", "易", "常", "武", "乔",
            "贺", "赖", "龚", "文", "庞", "樊", "兰", "殷", "施", "陶",
            "洪", "翟", "安", "颜", "倪", "严", "牛", "温", "芦", "季",
            "俞", "章", "鲁", "葛", "伍", "韦", "申", "尤", "毕", "聂",
            "焦", "向", "柳", "邢", "路", "岳", "齐", "沿", "梅", "莫",
            "庄", "辛", "管", "祝", "左", "涂", "谷", "祁", "时", "舒",
            "耿", "牟", "卜", "路", "詹", "关", "苗", "凌", "费", "纪",
            "靳", "盛", "童", "欧", "甄", "项", "曲", "成", "游", "阳",
            "裴", "席", "卫", "查", "屈", "鲍", "位", "覃", "霍", "翁",
            "隋", "植", "甘", "景", "薄", "单", "包", "司", "柏", "宁",
            "柯", "阮", "桂", "闵", "上官", "司马", "东方", "皇甫", "尉迟",
            "慕容", "司徒", "司空", "端木", "公羊", "澹台", "公冶", "宗政", "濮阳", "淳于",
            "单于", "太叔", "申屠", "公孙", "仲孙", "轩辕", "令狐", "钟离", "宇文", "长孙",
            "鲜于", "闾丘", "亓官", "司寇", "仉督", "子车", "颛孙", "端木", "巫马", "公西"
    };
    //名字（姓后）
    private static final String[] GIVEN_NAMES = {
            "叙白", "砚丞", "翊珩", "昭野", "淮序", "既明", "晏清", "昀朗", "叙深", "砚舟",
            "允墨", "景曜", "叙川", "淮之", "昭临", "砚知", "翊川", "既望", "晏桥", "昀野",
            "叙白", "淮安", "昭野", "砚书", "翊声", "景深", "允和", "既白", "晏声", "昀舟",
            "叙同", "淮临", "昭墨", "砚临", "翊同", "景珩", "允临", "既川", "晏同", "昀声",
            "叙舟", "淮声", "昭同", "砚同", "翊临", "景舟", "允舟", "既舟", "晏舟", "昀同",
            "叙临", "淮舟", "昭舟", "砚声", "翊舟", "景临", "允同", "既临", "晏临", "昀临",
            "叙声", "淮同", "昭声", "砚舟", "翊声", "景同", "允临", "既声", "晏声", "昀舟",
            "叙珩", "淮珩", "昭珩", "砚珩", "翊珩", "景珩", "允珩", "既珩", "晏珩", "昀珩",
            "叙曜", "淮曜", "昭曜", "砚曜", "翊曜", "景曜", "允曜", "既曜", "晏曜", "昀曜",
            "叙朗", "淮朗", "昭朗", "砚朗", "翊朗", "景朗", "允朗", "既朗", "晏朗", "昀朗",
            "昭棠", "叙柔", "砚卿", "淮月", "既夏", "晏宁", "昀舒", "允笙", "翊乔", "景芊",
            "叙蘅", "昭蘅", "砚蘅", "淮蘅", "既蘅", "晏蘅", "昀蘅", "允蘅", "翊蘅", "景蘅",
            "叙棠", "昭棠", "砚棠", "淮棠", "既棠", "晏棠", "昀棠", "允棠", "翊棠", "景棠",
            "叙柔", "昭柔", "砚柔", "淮柔", "既柔", "晏柔", "昀柔", "允柔", "翊柔", "景柔",
            "叙卿", "昭卿", "砚卿", "淮卿", "既卿", "晏卿", "昀卿", "允卿", "翊卿", "景卿",
            "叙月", "昭月", "砚月", "淮月", "既月", "晏月", "昀月", "允月", "翊月", "景月",
            "叙夏", "昭夏", "砚夏", "淮夏", "既夏", "晏夏", "昀夏", "允夏", "翊夏", "景夏",
            "叙宁", "昭宁", "砚宁", "淮宁", "既宁", "晏宁", "昀宁", "允宁", "翊宁", "景宁",
            "叙舒", "昭舒", "砚舒", "淮舒", "既舒", "晏舒", "昀舒", "允舒", "翊舒", "景舒",
            "叙笙", "昭笙", "砚笙", "淮笙", "既笙", "晏笙", "昀笙", "允笙", "翊笙", "景笙",
            // 新增名字（2026/1/1）
            "云深", "星河", "墨染", "清欢", "瑾瑜", "修远", "明轩", "浩然", "子衿", "若水",
            "逸尘", "瑾年", "清扬", "子墨", "云帆", "景行", "瑾瑜", "修竹", "明德", "致远",
            "清和", "子谦", "云逸", "景明", "瑾瑜", "修文", "明志", "志远", "清泉", "子安",
            "云开", "景逸", "瑾瑜", "修齐", "明远", "志诚", "清风", "子建", "云舒", "景和",
            "瑾瑜", "修身", "明理", "志强", "清韵", "子瑜", "云锦", "景平", "瑾瑜", "修心",
            "明义", "志明", "清音", "子骞", "云翔", "景泰", "瑾瑜", "修道", "明道", "志高",
            "清波", "子龙", "云涛", "景福", "瑾瑜", "修德", "明法", "志远", "清辉", "子期",
            "云海", "景云", "瑾瑜", "修业", "明礼", "志新", "清影", "子敬", "云峰", "景阳",
            "瑾瑜", "修睦", "明仁", "志勇", "清霜", "子昂", "云汉", "景行", "瑾瑜", "修平",
            "明智", "志华", "清露", "子真", "云梦", "景胜", "瑾瑜", "修静", "明慧", "志坚",
            "清秋", "子美", "云泽", "景龙", "瑾瑜", "修雅", "明达", "志远", "清寒", "子文",
            "云溪", "景瑞", "瑾瑜", "修远", "明哲", "志宏", "清晓", "子健", "云霓", "景星",
            "瑾瑜", "修明", "明远", "志伟", "清夜", "子厚", "云衢", "景辉", "瑾瑜", "修诚",
            "明诚", "志远", "清昼", "子方", "云翼", "景明", "瑾瑜", "修敬", "明敬", "志诚",
            "清晖", "子路", "云鹏", "景福", "瑾瑜", "修仁", "明仁", "志仁", "清光", "子贡",
            "云鹤", "景泰", "瑾瑜", "修义", "明义", "志义", "清辉", "子游", "云龙", "景云",
            "瑾瑜", "修礼", "明礼", "志礼", "清影", "子夏", "云虎", "景行", "瑾瑜", "修智",
            "明智", "志智", "清音", "子张", "云豹", "景明", "瑾瑜", "修信", "明信", "志信",
            "清韵", "子思", "云雀", "景逸", "瑾瑜", "修勇", "明勇", "志勇", "清波", "子舆",
            "云鹰", "景和", "瑾瑜", "修严", "明严", "志严", "清泉", "子产", "云鹏", "景平",
            "瑾瑜", "修宽", "明宽", "志宽", "清流", "子羽", "云凤", "景泰", "瑾瑜", "修敏",
            "明敏", "志敏", "清风", "子罕", "云凰", "景福", "瑾瑜", "修惠", "明惠", "志惠",
            "清月", "子游", "云鸾", "景云", "瑾瑜", "修慈", "明慈", "志慈", "清霜", "子路",
            "云鹄", "景行", "瑾瑜", "修俭", "明俭", "志俭", "清露", "子贡", "云鹏", "景明",
            "瑾瑜", "修让", "明让", "志让", "清秋", "子夏", "云鹤", "景逸", "瑾瑜", "修和",
            "明和", "志和", "清寒", "子张", "云龙", "景和", "瑾瑜", "修平", "明平", "志平"
    };

    public NameManager() {}

    public static NameManager get(ServerLevel level) {
        DimensionDataStorage storage = level.getDataStorage();
        return storage.computeIfAbsent(NameManager::load, NameManager::new, DATA_NAME);
    }

    public static int getNextNPCId(ServerLevel level) {
        NameManager manager = get(level);
        int id = manager.nextNPCId++;
        manager.setDirty();
        return id;
    }

    public String generateUniqueName(UUID entityId, Random random) {
        if (entityNames.containsKey(entityId)) {
            return entityNames.get(entityId);
        }

        String fullName;
        int attempts = 0;
        do {
            String surname = SURNAMES[random.nextInt(SURNAMES.length)];
            String givenName = GIVEN_NAMES[random.nextInt(GIVEN_NAMES.length)];
            fullName = surname + givenName;
            attempts++;
        } while (usedFullNames.contains(fullName) && attempts < 100);

        if (usedFullNames.contains(fullName)) {
            fullName += "_" + UUID.randomUUID().toString().substring(0, 4);
        }

        usedFullNames.add(fullName);
        entityNames.put(entityId, fullName);
        setDirty();
        return fullName;
    }

    @Override
    public CompoundTag save(@Nonnull CompoundTag tag) {
        ListTag namesList = new ListTag();
        for (String name : usedFullNames) {
            namesList.add(StringTag.valueOf(Objects.requireNonNull(name)));
        }
        tag.put("UsedNames", namesList);

        ListTag entityNameList = new ListTag();
        for (Map.Entry<UUID, String> entry : entityNames.entrySet()) {
            CompoundTag entryTag = new CompoundTag();
            entryTag.putUUID("EntityId", Objects.requireNonNull(entry.getKey()));
            entryTag.putString("FullName", Objects.requireNonNull(entry.getValue()));
            entityNameList.add(entryTag);
        }
        tag.put("EntityNames", entityNameList);

        tag.putInt("NextNPCId", nextNPCId);
        return tag;
    }

    public static NameManager load(CompoundTag tag) {
        NameManager data = new NameManager();

        ListTag namesList = tag.getList("UsedNames", Tag.TAG_STRING);
        for (Tag t : namesList) {
            data.usedFullNames.add(t.getAsString());
        }

        ListTag entityNameList = tag.getList("EntityNames", Tag.TAG_COMPOUND);
        for (Tag t : entityNameList) {
            CompoundTag entryTag = (CompoundTag) t;
            UUID id = entryTag.getUUID("EntityId");
            String name = entryTag.getString("FullName");
            data.entityNames.put(id, name);
        }

        if (tag.contains("NextNPCId")) {
            data.nextNPCId = tag.getInt("NextNPCId");
        }

        return data;
    }
    
    /**
     * 更新NPC名称
     */
    public void updateNPCName(UUID npcUuid, String newName) {
        // 从已使用名称中移除旧名称
        String oldName = entityNames.get(npcUuid);
        if (oldName != null) {
            usedFullNames.remove(oldName);
        }
        
        // 添加新名称到已使用名称集合
        usedFullNames.add(newName);
        
        // 更新实体名称映射
        entityNames.put(npcUuid, newName);
        
        setDirty(); // 标记数据已更改
    }
}
