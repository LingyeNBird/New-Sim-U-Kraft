package com.xiaoliang.simukraft;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class VersionCompareTest {
    private static final Pattern VERSION_PATTERN = Pattern.compile("v?(\\d+)\\.(\\d+)\\.(\\d+)([a-zA-Z]*\\d*)?(?:-([a-zA-Z]+\\d*))?");

    public static void main(String[] args) {
        testParseVersion("v1.0.5a1");
        testParseVersion("1.0.4b3");
        testParseVersion("1.0.5");
        testParseVersion("v1.0.4");
    }

    private static void testParseVersion(String version) {
        System.out.println("\n=== Testing: " + version + " ===");

        Matcher matcher = VERSION_PATTERN.matcher(version.trim());
        boolean matches = matcher.matches();
        System.out.println("matches: " + matches);

        if (matches) {
            System.out.println("Group 1 (major): " + matcher.group(1));
            System.out.println("Group 2 (minor): " + matcher.group(2));
            System.out.println("Group 3 (patch): " + matcher.group(3));
            System.out.println("Group 4 (subVersion): " + matcher.group(4));
            System.out.println("Group 5 (fixVersion): " + matcher.group(5));
        } else {
            System.out.println("FAILED TO MATCH!");
        }
    }
}
