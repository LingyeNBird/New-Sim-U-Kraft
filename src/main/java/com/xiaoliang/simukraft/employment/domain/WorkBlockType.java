package com.xiaoliang.simukraft.employment.domain;

public enum WorkBlockType {
    BUILD_BOX,
    FARMLAND_BOX,
    INDUSTRIAL_CONTROL_BOX,
    COMMERCIAL_CONTROL_BOX,
    LOGISTICS_SERVER_BOX;

    public String toLegacyKey() {
        return switch (this) {
            case BUILD_BOX -> "build_box";
            case FARMLAND_BOX -> "farmland";
            case INDUSTRIAL_CONTROL_BOX -> "industrial";
            case COMMERCIAL_CONTROL_BOX -> "commercial";
            case LOGISTICS_SERVER_BOX -> "logistics";
        };
    }

    public static WorkBlockType fromLegacyKey(String key) {
        if (key == null) {
            return null;
        }
        return switch (key) {
            case "build_box", "buildbox" -> BUILD_BOX;
            case "farmland", "farmland_box" -> FARMLAND_BOX;
            case "industrial", "wool_farm", "beef_farm" -> INDUSTRIAL_CONTROL_BOX;
            case "commercial", "bakery", "meat_shop", "fruit_shop", "building_material_store" -> COMMERCIAL_CONTROL_BOX;
            case "logistics", "logistics_server_box" -> LOGISTICS_SERVER_BOX;
            default -> null;
        };
    }
}

