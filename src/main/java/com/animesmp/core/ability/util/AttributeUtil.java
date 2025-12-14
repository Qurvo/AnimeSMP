package com.animesmp.core.util;

import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.attribute.Attribute;

public final class AttributeUtil {
    private AttributeUtil() {}

    public static Attribute maxHealth() {
        Attribute a = Registry.ATTRIBUTE.get(NamespacedKey.minecraft("generic.max_health"));
        if (a == null) a = Registry.ATTRIBUTE.get(NamespacedKey.minecraft("max_health"));
        return a;
    }
}
