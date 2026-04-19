package com.xiaoliang.simukraft.test;

import com.xiaoliang.simukraft.utils.BuildingDataManager;

public class BuildingDataTest {
    public static void main(String[] args) {
        System.out.println("=== 建筑数据读取测试 ===");
        
        // 测试住宅类建筑
        System.out.println("\n--- 住宅类建筑 ---");
        var residentialBuildings = BuildingDataManager.getBuildingsByCategory("residential");
        for (var building : residentialBuildings) {
            System.out.println("名称: " + building.getName());
            System.out.println("尺寸: " + building.getSize());
            System.out.println("价格: " + building.getAmount());
            System.out.println("作者: " + building.getAuthor());
            System.out.println("描述: " + building.getDescription());
            System.out.println("文件: " + building.getFileName());
            System.out.println("---");
        }
        
        // 测试搜索功能
        System.out.println("\n--- 搜索测试 ---");
        var searchResults = BuildingDataManager.searchBuildings("单元");
        System.out.println("搜索'单元'找到 " + searchResults.size() + " 个结果");
        for (var building : searchResults) {
            System.out.println("- " + building.getName() + " (" + building.getCategory() + ")");
        }
    }
}