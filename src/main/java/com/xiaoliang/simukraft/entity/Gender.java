package com.xiaoliang.simukraft.entity;

public enum Gender {
    MALE("male"),
    FEMALE("female");

    private final String name;

    Gender(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public static Gender getRandom() {
        return getRandom(0.5D);
    }

    public static Gender getRandom(double maleChance) {
        double normalizedMaleChance = Math.max(0.0D, Math.min(1.0D, maleChance));
        return Math.random() < normalizedMaleChance ? MALE : FEMALE;
    }

    public static Gender fromString(String gender) {
        if (gender == null) return MALE;
        return gender.equalsIgnoreCase("female") ? FEMALE : MALE;
    }
}
