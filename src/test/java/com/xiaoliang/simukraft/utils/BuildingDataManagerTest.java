package com.xiaoliang.simukraft.utils;

public class BuildingDataManagerTest {
    public static void main(String[] args) {
        System.out.println("Testing BuildingDataManager static initialization...");
        // 触发BuildingDataManager的静态初始化
        new BuildingDataManager();
        System.out.println("Test completed, check if simukraftbuilding folder is generated");
    }
}
