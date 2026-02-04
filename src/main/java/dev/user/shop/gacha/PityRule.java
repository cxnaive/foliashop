package dev.user.shop.gacha;

/**
 * 保底规则
 */
public class PityRule {
    private final int count;          // 抽奖次数阈值
    private final double maxProbability;  // 最大概率（抽中概率小于等于此值的奖品）

    public PityRule(int count, double maxProbability) {
        this.count = count;
        this.maxProbability = maxProbability;
    }

    public int getCount() { return count; }
    public double getMaxProbability() { return maxProbability; }

    /**
     * 获取规则哈希，用于数据库唯一标识此规则
     * 格式: count_maxProbability (如 "10_0.05")
     * 使用 Double.toString() 确保精度不丢失，避免 0.1 和 0.10 产生相同哈希
     */
    public String getRuleHash() {
        return count + "_" + Double.toString(maxProbability);
    }

    @Override
    public String toString() {
        return "PityRule{count=" + count + ", maxProb=" + maxProbability + "}";
    }
}
