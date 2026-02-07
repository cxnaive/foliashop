package dev.user.shop.gacha;

import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.entity.Display;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.util.Vector;
import org.joml.Vector3f;

import java.util.Random;

/**
 * 展示实体粒子效果配置
 */
public class ParticleEffectConfig {

    // 预设效果类型
    public enum EffectType {
        NONE,           // 无效果
        STAR_RING,      // 环绕星光
        MAGIC_RUNE,     // 魔法符文
        RAINBOW_HALO,   // 彩虹光环
        FLAME_AURA,     // 火焰气息
        FROST_CRYSTAL,  // 冰霜结晶
        LOVE_BUBBLE     // 爱心气泡
    }

    // 效果类型
    private final EffectType type;
    // 粒子密度 (每周期生成的粒子数)
    private final int density;
    // 效果半径
    private final double radius;
    // 效果速度
    private final double speed;
    // 自定义颜色 (可选)
    private final Color customColor;

    // 内部状态
    private double angle = 0;
    private int tick = 0;
    private final Random random = new Random();

    public ParticleEffectConfig(EffectType type, int density, double radius, double speed, Color customColor) {
        this.type = type != null ? type : EffectType.NONE;
        this.density = Math.max(1, density);
        this.radius = radius > 0 ? radius : 1.0;
        this.speed = speed > 0 ? speed : 1.0;
        this.customColor = customColor;
    }

    public static ParticleEffectConfig fromConfig(org.bukkit.configuration.ConfigurationSection section) {
        if (section == null) {
            return null;  // 返回 null 以便继承父配置
        }

        EffectType type = EffectType.NONE;
        String typeStr = section.getString("type", "NONE").toUpperCase();
        try {
            type = EffectType.valueOf(typeStr);
        } catch (IllegalArgumentException ignored) {}

        int density = section.getInt("density", 3);
        double radius = section.getDouble("radius", 1.2);
        double speed = section.getDouble("speed", 1.0);

        // 解析自定义颜色
        Color customColor = null;
        String colorStr = section.getString("color", null);
        if (colorStr != null && !colorStr.isEmpty()) {
            try {
                customColor = Color.fromRGB(Integer.parseInt(colorStr.replace("#", ""), 16));
            } catch (NumberFormatException ignored) {}
        }

        return new ParticleEffectConfig(type, density, radius, speed, customColor);
    }

    /**
     * 每tick更新粒子效果
     * @param display 展示实体
     * @return 是否继续播放
     */
    public boolean tick(ItemDisplay display) {
        if (type == EffectType.NONE || !display.isValid() || display.isDead()) {
            return false;
        }

        Location loc = display.getLocation();
        World world = loc.getWorld();
        if (world == null) return false;

        // 获取展示实体的实际位置（考虑变换）
        Vector3f scale = display.getTransformation().getScale();
        float maxScale = Math.max(scale.x, Math.max(scale.y, scale.z));
        double effectRadius = radius * maxScale;

        tick++;
        angle += 0.1 * speed;

        switch (type) {
            case STAR_RING -> playStarRing(world, loc, effectRadius);
            case MAGIC_RUNE -> playMagicRune(world, loc, effectRadius);
            case RAINBOW_HALO -> playRainbowHalo(world, loc, effectRadius);
            case FLAME_AURA -> playFlameAura(world, loc, effectRadius);
            case FROST_CRYSTAL -> playFrostCrystal(world, loc, effectRadius);
            case LOVE_BUBBLE -> playLoveBubble(world, loc, effectRadius);
        }

        return true;
    }

    /**
     * 环绕星光 - 金色星星围绕实体旋转
     */
    private void playStarRing(World world, Location center, double radius) {
        Color color = customColor != null ? customColor : Color.YELLOW;

        for (int i = 0; i < density; i++) {
            double offsetAngle = angle + (2 * Math.PI * i / density);
            double x = center.getX() + radius * Math.cos(offsetAngle);
            double z = center.getZ() + radius * Math.sin(offsetAngle);
            double y = center.getY() + 0.3 * Math.sin(offsetAngle * 2);

            Location particleLoc = new Location(world, x, y, z);

            // 使用红色stone粒子模拟星星
            world.spawnParticle(Particle.DUST, particleLoc, 1,
                0, 0, 0, 0,
                new Particle.DustOptions(color, 0.8f));

            // 偶尔添加闪烁效果
            if (random.nextInt(10) == 0) {
                world.spawnParticle(Particle.END_ROD, particleLoc, 1, 0.1, 0.1, 0.1, 0);
            }
        }
    }

    /**
     * 魔法符文 - 紫色神秘符文环绕
     */
    private void playMagicRune(World world, Location center, double radius) {
        Color primaryColor = customColor != null ? customColor : Color.PURPLE;
        Color secondaryColor = Color.fromRGB(150, 0, 200);

        // 绘制符文圆环
        int points = density * 3;
        for (int i = 0; i < points; i++) {
            double theta = angle + (2 * Math.PI * i / points);
            double x = center.getX() + radius * Math.cos(theta);
            double z = center.getZ() + radius * Math.sin(theta);
            double y = center.getY() + 0.1;

            Location particleLoc = new Location(world, x, y, z);

            // 交替使用两种颜色
            Particle.DustOptions dust = new Particle.DustOptions(
                i % 2 == 0 ? primaryColor : secondaryColor, 0.6f);
            world.spawnParticle(Particle.DUST, particleLoc, 1, 0, 0, 0, 0, dust);
        }

        // 上升的神秘粒子
        if (tick % 5 == 0) {
            for (int i = 0; i < density; i++) {
                double r = random.nextDouble() * radius * 0.8;
                double theta = random.nextDouble() * 2 * Math.PI;
                double x = center.getX() + r * Math.cos(theta);
                double z = center.getZ() + r * Math.sin(theta);
                double y = center.getY() + random.nextDouble() * 0.5;

                Location particleLoc = new Location(world, x, y, z);
                world.spawnParticle(Particle.WITCH, particleLoc, 1, 0, 0.1, 0, 0.02);
            }
        }
    }

    /**
     * 彩虹光环 - 彩色循环变化
     */
    private void playRainbowHalo(World world, Location center, double radius) {
        int colors = density * 2;
        for (int i = 0; i < colors; i++) {
            float hue = ((float) i / colors + (float) tick / 100) % 1.0f;
            Color color = Color.fromRGB(
                java.awt.Color.HSBtoRGB(hue, 0.8f, 1.0f) & 0xFFFFFF);

            double theta = angle + (2 * Math.PI * i / colors);
            double x = center.getX() + radius * Math.cos(theta);
            double z = center.getZ() + radius * Math.sin(theta);
            double y = center.getY() + 0.2 * Math.sin(theta * 3 + angle);

            Location particleLoc = new Location(world, x, y, z);
            world.spawnParticle(Particle.DUST, particleLoc, 1, 0, 0, 0, 0,
                new Particle.DustOptions(color, 0.7f));
        }
    }

    /**
     * 火焰气息 - 火焰粒子向上飘散
     */
    private void playFlameAura(World world, Location center, double radius) {
        // 底部火焰环
        for (int i = 0; i < density; i++) {
            double theta = angle * 2 + (2 * Math.PI * i / density);
            double r = radius * (0.8 + 0.2 * Math.sin(theta * 3));
            double x = center.getX() + r * Math.cos(theta);
            double z = center.getZ() + r * Math.sin(theta);
            double y = center.getY() - 0.3;

            Location particleLoc = new Location(world, x, y, z);
            world.spawnParticle(Particle.FLAME, particleLoc, 1, 0.05, 0, 0.05, 0.02);
        }

        // 上升的火星
        if (tick % 3 == 0) {
            for (int i = 0; i < density / 2; i++) {
                double r = random.nextDouble() * radius * 0.7;
                double theta = random.nextDouble() * 2 * Math.PI;
                double x = center.getX() + r * Math.cos(theta);
                double z = center.getZ() + r * Math.sin(theta);
                double y = center.getY() - 0.2;

                Location particleLoc = new Location(world, x, y, z);
                world.spawnParticle(Particle.SMALL_FLAME, particleLoc, 1,
                    0.02, 0.05, 0.02, 0.03);
            }
        }

        // 烟雾效果
        if (tick % 5 == 0) {
            double x = center.getX() + (random.nextDouble() - 0.5) * radius;
            double z = center.getZ() + (random.nextDouble() - 0.5) * radius;
            double y = center.getY() + 0.5;
            world.spawnParticle(Particle.SMOKE, x, y, z, 1, 0.1, 0.1, 0.1, 0.01);
        }
    }

    /**
     * 冰霜结晶 - 蓝色雪花飘落
     */
    private void playFrostCrystal(World world, Location center, double radius) {
        Color iceColor = customColor != null ? customColor : Color.fromRGB(200, 230, 255);

        // 雪花飘落
        for (int i = 0; i < density; i++) {
            double theta = angle * 0.5 + (2 * Math.PI * i / density);
            double r = radius * (0.5 + 0.5 * Math.sin(theta * 2));
            double x = center.getX() + r * Math.cos(theta);
            double z = center.getZ() + r * Math.sin(theta);
            double y = center.getY() + 0.5 + 0.3 * Math.sin(theta + tick * 0.1);

            Location particleLoc = new Location(world, x, y, z);
            world.spawnParticle(Particle.DUST, particleLoc, 1, 0.05, 0.05, 0.05, 0,
                new Particle.DustOptions(iceColor, 0.6f));
        }

        // 冰霜效果
        if (tick % 4 == 0) {
            for (int i = 0; i < density / 2; i++) {
                double r = random.nextDouble() * radius;
                double theta = random.nextDouble() * 2 * Math.PI;
                double x = center.getX() + r * Math.cos(theta);
                double z = center.getZ() + r * Math.sin(theta);
                double y = center.getY() + random.nextDouble() * 0.8;

                Location particleLoc = new Location(world, x, y, z);
                world.spawnParticle(Particle.SNOWFLAKE, particleLoc, 1,
                    0.02, -0.02, 0.02, 0.01);
            }
        }

        // 冰晶闪烁
        if (tick % 10 == 0) {
            double x = center.getX() + (random.nextDouble() - 0.5) * radius * 1.5;
            double z = center.getZ() + (random.nextDouble() - 0.5) * radius * 1.5;
            double y = center.getY() + random.nextDouble() * 0.5;
            world.spawnParticle(Particle.END_ROD, x, y, z, 1, 0, 0, 0, 0);
        }
    }

    /**
     * 爱心气泡 - 粉色爱心向上飘
     */
    private void playLoveBubble(World world, Location center, double radius) {
        Color pink = customColor != null ? customColor : Color.fromRGB(255, 182, 193);

        // 上升爱心
        if (tick % 4 == 0) {
            for (int i = 0; i < density; i++) {
                double r = random.nextDouble() * radius * 0.6;
                double theta = random.nextDouble() * 2 * Math.PI;
                double x = center.getX() + r * Math.cos(theta);
                double z = center.getZ() + r * Math.sin(theta);
                double y = center.getY() + random.nextDouble() * 0.3;

                Location particleLoc = new Location(world, x, y, z);

                // 使用红色stone粒子模拟爱心
                world.spawnParticle(Particle.DUST, particleLoc, 1,
                    0.02, 0.03, 0.02, 0,
                    new Particle.DustOptions(pink, 0.5f));

                // 偶尔添加闪烁
                if (random.nextInt(5) == 0) {
                    world.spawnParticle(Particle.HEART, particleLoc, 1, 0.1, 0.1, 0.1, 0);
                }
            }
        }

        // 环绕的粉色粒子
        for (int i = 0; i < density; i++) {
            double theta = angle + (2 * Math.PI * i / density);
            double x = center.getX() + radius * 0.8 * Math.cos(theta);
            double z = center.getZ() + radius * 0.8 * Math.sin(theta);
            double y = center.getY() + 0.2 * Math.sin(tick * 0.2 + i);

            Location particleLoc = new Location(world, x, y, z);
            world.spawnParticle(Particle.DUST, particleLoc, 1, 0, 0, 0, 0,
                new Particle.DustOptions(Color.fromRGB(255, 105, 180), 0.4f));
        }
    }

    // Getters
    public EffectType getType() { return type; }
    public int getDensity() { return density; }
    public double getRadius() { return radius; }
    public double getSpeed() { return speed; }
    public Color getCustomColor() { return customColor; }
}
