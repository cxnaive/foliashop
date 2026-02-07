package dev.user.shop.gacha;

import dev.user.shop.config.ShopConfig;
import org.bukkit.configuration.ConfigurationSection;

/**
 * 展示实体配置类
 * 支持默认配置和扭蛋机特定配置的继承关系
 */
public class DisplayEntityConfig {

    // 默认值
    public static final boolean DEFAULT_ENABLED = true;
    public static final float DEFAULT_SCALE = 0.8f;
    public static final float DEFAULT_ROTATION_Y = 45.0f;
    public static final boolean DEFAULT_FACE_PLAYER = false;
    public static final boolean DEFAULT_FLOATING_ANIMATION = true;
    public static final float DEFAULT_FLOAT_AMPLITUDE = 0.1f;
    public static final float DEFAULT_FLOAT_SPEED = 1.0f;
    public static final float DEFAULT_HEIGHT_OFFSET = 1.5f;
    public static final int DEFAULT_ANIMATION_PERIOD = 3;
    public static final float DEFAULT_VIEW_RANGE = 32.0f;
    public static final float DEFAULT_SHADOW_RADIUS = 0.3f;
    public static final float DEFAULT_SHADOW_STRENGTH = 0.3f;
    public static final boolean DEFAULT_GLOWING = false;
    public static final String DEFAULT_GLOW_COLOR = null; // null 表示使用白色发光

    // 配置值（使用包装类型，null 表示未设置，继承默认值）
    private final Boolean enabled;
    private final Float scale;
    private final Float rotationY;
    private final Boolean facePlayer;
    private final Boolean floatingAnimation;
    private final Float floatAmplitude;
    private final Float floatSpeed;
    private final Float heightOffset;
    private final Integer animationPeriod;
    private final Float viewRange;
    private final Float shadowRadius;
    private final Float shadowStrength;
    private final Boolean glowing;
    private final String glowColor;
    private final ParticleEffectConfig particleEffect;

    /**
     * 创建默认配置
     */
    public static DisplayEntityConfig defaultConfig() {
        return new DisplayEntityConfig(
            DEFAULT_ENABLED, DEFAULT_SCALE, DEFAULT_ROTATION_Y, DEFAULT_FACE_PLAYER,
            DEFAULT_FLOATING_ANIMATION, DEFAULT_FLOAT_AMPLITUDE, DEFAULT_FLOAT_SPEED,
            DEFAULT_HEIGHT_OFFSET, DEFAULT_ANIMATION_PERIOD, DEFAULT_VIEW_RANGE,
            DEFAULT_SHADOW_RADIUS, DEFAULT_SHADOW_STRENGTH, DEFAULT_GLOWING, DEFAULT_GLOW_COLOR,
            new ParticleEffectConfig(ParticleEffectConfig.EffectType.NONE, 1, 1.0, 1.0, null)
        );
    }

    /**
     * 从 ShopConfig 创建默认配置
     */
    public static DisplayEntityConfig fromShopConfig(ShopConfig config) {
        return new DisplayEntityConfig(
            config.isDisplayEntityEnabled(),
            config.getDisplayEntityScale(),
            config.getDisplayEntityRotationY(),
            config.isDisplayEntityFacePlayer(),
            config.isDisplayEntityFloatingAnimation(),
            config.getDisplayEntityFloatAmplitude(),
            config.getDisplayEntityFloatSpeed(),
            config.getDisplayEntityHeightOffset(),
            config.getDisplayEntityAnimationPeriod(),
            config.getDisplayEntityViewRange(),
            config.getDisplayEntityShadowRadius(),
            config.getDisplayEntityShadowStrength(),
            config.isDisplayEntityGlowing(),
            config.getDisplayEntityGlowColor(),
            config.getDisplayEntityParticleEffect()
        );
    }

    /**
     * 从配置节解析展示实体配置（部分配置）
     */
    public static DisplayEntityConfig fromConfig(ConfigurationSection section) {
        if (section == null) return null;

        return new DisplayEntityConfig(
            getBooleanOrNull(section, "enabled"),
            getFloatOrNull(section, "scale"),
            getFloatOrNull(section, "rotation-y"),
            getBooleanOrNull(section, "face-player"),
            getBooleanOrNull(section, "floating-animation"),
            getFloatOrNull(section, "float-amplitude"),
            getFloatOrNull(section, "float-speed"),
            getFloatOrNull(section, "height-offset"),
            getIntOrNull(section, "animation-period"),
            getFloatOrNull(section, "view-range"),
            getFloatOrNull(section, "shadow-radius"),
            getFloatOrNull(section, "shadow-strength"),
            getBooleanOrNull(section, "glowing"),
            getStringOrNull(section, "glow-color"),
            ParticleEffectConfig.fromConfig(section.getConfigurationSection("particle-effect"))
        );
    }

    private static String getStringOrNull(ConfigurationSection section, String path) {
        if (!section.contains(path)) return null;
        return section.getString(path);
    }

    private static Float getFloatOrNull(ConfigurationSection section, String path) {
        if (!section.contains(path)) return null;
        double value = section.getDouble(path);
        return (float) value;
    }

    private static Boolean getBooleanOrNull(ConfigurationSection section, String path) {
        if (!section.contains(path)) return null;
        return section.getBoolean(path);
    }

    private static Integer getIntOrNull(ConfigurationSection section, String path) {
        if (!section.contains(path)) return null;
        return section.getInt(path);
    }

    public DisplayEntityConfig(Boolean enabled, Float scale, Float rotationY, Boolean facePlayer,
                               Boolean floatingAnimation, Float floatAmplitude, Float floatSpeed,
                               Float heightOffset, Integer animationPeriod, Float viewRange,
                               Float shadowRadius, Float shadowStrength, Boolean glowing, String glowColor,
                               ParticleEffectConfig particleEffect) {
        this.enabled = enabled;
        this.scale = scale;
        this.rotationY = rotationY;
        this.facePlayer = facePlayer;
        this.floatingAnimation = floatingAnimation;
        this.floatAmplitude = floatAmplitude;
        this.floatSpeed = floatSpeed;
        this.heightOffset = heightOffset;
        this.animationPeriod = animationPeriod;
        this.viewRange = viewRange;
        this.shadowRadius = shadowRadius;
        this.shadowStrength = shadowStrength;
        this.glowing = glowing;
        this.glowColor = glowColor;
        this.particleEffect = particleEffect;
    }

    /**
     * 合并配置：当前配置未设置的值从父配置继承
     */
    public DisplayEntityConfig mergeWithParent(DisplayEntityConfig parent) {
        if (parent == null) return this;
        return new DisplayEntityConfig(
            this.enabled != null ? this.enabled : parent.enabled,
            this.scale != null ? this.scale : parent.scale,
            this.rotationY != null ? this.rotationY : parent.rotationY,
            this.facePlayer != null ? this.facePlayer : parent.facePlayer,
            this.floatingAnimation != null ? this.floatingAnimation : parent.floatingAnimation,
            this.floatAmplitude != null ? this.floatAmplitude : parent.floatAmplitude,
            this.floatSpeed != null ? this.floatSpeed : parent.floatSpeed,
            this.heightOffset != null ? this.heightOffset : parent.heightOffset,
            this.animationPeriod != null ? this.animationPeriod : parent.animationPeriod,
            this.viewRange != null ? this.viewRange : parent.viewRange,
            this.shadowRadius != null ? this.shadowRadius : parent.shadowRadius,
            this.shadowStrength != null ? this.shadowStrength : parent.shadowStrength,
            this.glowing != null ? this.glowing : parent.glowing,
            this.glowColor != null ? this.glowColor : parent.glowColor,
            this.particleEffect != null ? this.particleEffect : parent.particleEffect
        );
    }

    // Getters
    public boolean isEnabled() { return enabled != null ? enabled : DEFAULT_ENABLED; }
    public float getScale() { return scale != null ? scale : DEFAULT_SCALE; }
    public float getRotationY() { return rotationY != null ? rotationY : DEFAULT_ROTATION_Y; }
    public boolean isFacePlayer() { return facePlayer != null ? facePlayer : DEFAULT_FACE_PLAYER; }
    public boolean isFloatingAnimation() { return floatingAnimation != null ? floatingAnimation : DEFAULT_FLOATING_ANIMATION; }
    public float getFloatAmplitude() { return floatAmplitude != null ? floatAmplitude : DEFAULT_FLOAT_AMPLITUDE; }
    public float getFloatSpeed() { return floatSpeed != null ? floatSpeed : DEFAULT_FLOAT_SPEED; }
    public float getHeightOffset() { return heightOffset != null ? heightOffset : DEFAULT_HEIGHT_OFFSET; }
    public int getAnimationPeriod() { return animationPeriod != null ? animationPeriod : DEFAULT_ANIMATION_PERIOD; }
    public float getViewRange() { return viewRange != null ? viewRange : DEFAULT_VIEW_RANGE; }
    public float getShadowRadius() { return shadowRadius != null ? shadowRadius : DEFAULT_SHADOW_RADIUS; }
    public float getShadowStrength() { return shadowStrength != null ? shadowStrength : DEFAULT_SHADOW_STRENGTH; }
    public boolean isGlowing() { return glowing != null ? glowing : DEFAULT_GLOWING; }
    public String getGlowColor() { return glowColor; }
    public ParticleEffectConfig getParticleEffect() { return particleEffect; }
}
