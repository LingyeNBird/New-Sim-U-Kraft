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
        return Math.random() < 0.5 ? MALE : FEMALE;
    }

    public static Gender fromString(String gender) {
        if (gender == null) return MALE;
        return gender.equalsIgnoreCase("female") ? FEMALE : MALE;
    }
}