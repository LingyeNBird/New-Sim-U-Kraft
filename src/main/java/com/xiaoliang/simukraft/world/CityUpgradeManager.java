package com.xiaoliang.simukraft.world;

import net.minecraft.server.packs.resources.ResourceManager;

import java.util.*;

public class CityUpgradeManager  {
    private final Map<Integer, CityUpgrade> upgrades = new HashMap<>();
    private static CityUpgradeManager instance;

    public record CityUpgrade(int level, String name, String description, Requirements requirements, String upgradeType,
                              String unlocks) {
    }

    public record Requirements(int population, int wood, int cobblestone, int ironIngot, int goldIngot, int diamond,
                               int lapisLazuli, double funds) {
    }

    private CityUpgradeManager() {
        // 初始化升级数据
        this.upgrades.put(0, new CityUpgrade(0, "拓荒者", "第一行脚印陷进无名的土地，后来这里叫做'开始'。", 
            new Requirements(0, 0, 0, 0, 0, 0, 0, 0.0), "auto", ""));
        this.upgrades.put(1, new CityUpgrade(1, "部落", "语言在篝火中诞生，我们给万物起名，用血缘捆成绳结。", 
            new Requirements(3, 0, 0, 0, 0, 0, 0, 0.0), "manual", ""));
        this.upgrades.put(2, new CityUpgrade(2, "村落", "春种秋藏的秩序里，生出比血缘更深的牵绊——邻居。", 
            new Requirements(4, 8, 8, 0, 0, 0, 0, 5.0), "manual", "解锁区块购买，并增加创建城市给予的区块周围一圈范围内未被占有的区块购买权"));
        this.upgrades.put(3, new CityUpgrade(3, "城镇", "道路开始计较宽度，陌生人与货物在码头上交换远方。", 
            new Requirements(5, 16, 16, 0, 0, 0, 0, 10.0), "manual", "解锁区块购买，在已解锁的地块继续解锁一圈未被占有的区块"));
        this.upgrades.put(4, new CityUpgrade(4, "城市", "时间被钟声切成等份，有人造屋，有人造梦，有人造律法。", 
            new Requirements(8, 32, 32, 0, 0, 0, 0, 15.0), "manual", "解锁区块购买，在已解锁的地块继续解锁一圈未被占有的区块"));
        this.upgrades.put(5, new CityUpgrade(5, "城池", "城墙内外的世界开始对峙，而文书跑得比马蹄更远。", 
            new Requirements(10, 64, 64, 3, 0, 0, 0, 20.0), "manual", "解锁区块购买，在已解锁的地块继续解锁一圈未被占有的区块"));
        this.upgrades.put(6, new CityUpgrade(6, "都会", "蒸汽与电改写昼夜，旧神像在新大厦的阴影里剥落。", 
            new Requirements(15, 128, 128, 6, 0, 0, 0, 30.0), "manual", "解锁区块购买，在已解锁的地块继续解锁一圈未被占有的区块"));
        this.upgrades.put(7, new CityUpgrade(7, "重镇", "血管长成骨骼，每一次脉动都让远方随之震颤。", 
            new Requirements(20, 256, 256, 12, 10, 0, 0, 60.0), "manual", "解锁区块购买，在已解锁的地块继续解锁一圈未被占有的区块"));
        this.upgrades.put(8, new CityUpgrade(8, "中心城", "图纸决定山的去留，灯火彻夜不眠，浇筑新的星空。", 
            new Requirements(25, 512, 128, 24, 20, 3, 0, 80.0), "manual", "解锁区块购买，在已解锁的地块继续解锁一圈未被占有的区块"));
        this.upgrades.put(9, new CityUpgrade(9, "枢纽城", "信息在海底奔跑，货物在空中飞行，我们搬运世界本身。", 
            new Requirements(30, 512, 256, 48, 40, 6, 28, 90.0), "manual", "解锁区块购买，在已解锁的地块继续解锁一圈未被占有的区块"));
        this.upgrades.put(10, new CityUpgrade(10, "大都市", "这里产生的无形刻度，丈量着半个地球的明天。", 
            new Requirements(35, 512, 256, 64, 64, 12, 32, 100.0), "manual", "解锁区块购买，在已解锁的地块继续解锁一圈未被占有的区块"));
        this.upgrades.put(11, new CityUpgrade(11, "不夜都", "当人造星群淹没银河，它醒着，成为所有时区的首都。", 
            new Requirements(40, 512, 256, 128, 64, 12, 64, 120.0), "manual", "解锁所有未被占用区块的购买权"));
    }

    public static CityUpgradeManager getInstance() {
        if (instance == null) {
            instance = new CityUpgradeManager();
        }
        return instance;
    }

    /**
     * 加载升级配置文件（可选）
     * 如果配置文件存在，则从文件加载升级数据，否则使用硬编码的数据
     */
    public void loadUpgrades(ResourceManager resourceManager) {
        // 保留硬编码的数据，不覆盖
    }

    public CityUpgrade getUpgrade(int level) {
        return upgrades.get(level);
    }

    public List<CityUpgrade> getAllUpgrades() {
        List<CityUpgrade> sortedUpgrades = new ArrayList<>(upgrades.values());
        sortedUpgrades.sort(Comparator.comparingInt(CityUpgrade::level));
        return sortedUpgrades;
    }

    public boolean canUpgrade(CityData.CityInfo cityInfo, int targetLevel) {
        CityUpgrade upgrade = upgrades.get(targetLevel);
        if (upgrade == null || targetLevel <= cityInfo.getCityLevel()) {
            return false;
        }
        
        Requirements requirements = upgrade.requirements();
        int population = cityInfo.getCitizenIds().size();
        double funds = cityInfo.getFunds();
        
        return population >= requirements.population() &&
               funds >= requirements.funds();
    }

    public CityUpgrade getNextUpgrade(CityData.CityInfo cityInfo) {
        int currentLevel = cityInfo.getCityLevel();
        return upgrades.get(currentLevel + 1);
    }

    public boolean hasNextUpgrade(CityData.CityInfo cityInfo) {
        return upgrades.containsKey(cityInfo.getCityLevel() + 1);
    }

    public String getUpgradeRequirementsText(CityUpgrade upgrade) {
        Requirements requirements = upgrade.requirements();
        StringBuilder sb = new StringBuilder();
        
        if (requirements.population() > 0) {
            sb.append("人口: ").append(requirements.population()).append("\n");
        }
        if (requirements.wood() > 0) {
            sb.append("木头: ").append(requirements.wood()).append("\n");
        }
        if (requirements.cobblestone() > 0) {
            sb.append("圆石: ").append(requirements.cobblestone()).append("\n");
        }
        if (requirements.ironIngot() > 0) {
            sb.append("铁锭: ").append(requirements.ironIngot()).append("\n");
        }
        if (requirements.goldIngot() > 0) {
            sb.append("金锭: ").append(requirements.goldIngot()).append("\n");
        }
        if (requirements.diamond() > 0) {
            sb.append("钻石: ").append(requirements.diamond()).append("\n");
        }
        if (requirements.lapisLazuli() > 0) {
            sb.append("青金石: ").append(requirements.lapisLazuli()).append("\n");
        }
        if (requirements.funds() > 0.0) {
            sb.append("资金: $").append(requirements.funds()).append("\n");
        }
        
        return sb.toString().trim();
    }
}
