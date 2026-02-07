package dev.user.shop.gacha;

import org.bukkit.util.BlockVector;

import java.util.UUID;

/**
 * 扭蛋机方块绑定数据类
 */
public class GachaBlockBinding {

    private final int id;
    private final UUID worldUuid;
    private final BlockVector position;
    private final String machineId;
    private final UUID createdBy;
    private final long createdAt;
    private UUID displayEntityUuid;
    private boolean outdated;

    public GachaBlockBinding(int id, UUID worldUuid, BlockVector position, String machineId, UUID createdBy, long createdAt) {
        this(id, worldUuid, position, machineId, createdBy, createdAt, null, false);
    }

    public GachaBlockBinding(int id, UUID worldUuid, BlockVector position, String machineId, UUID createdBy, long createdAt, UUID displayEntityUuid) {
        this(id, worldUuid, position, machineId, createdBy, createdAt, displayEntityUuid, false);
    }

    public GachaBlockBinding(int id, UUID worldUuid, BlockVector position, String machineId, UUID createdBy, long createdAt, UUID displayEntityUuid, boolean outdated) {
        this.id = id;
        this.worldUuid = worldUuid;
        this.position = position;
        this.machineId = machineId;
        this.createdBy = createdBy;
        this.createdAt = createdAt;
        this.displayEntityUuid = displayEntityUuid;
        this.outdated = outdated;
    }

    public int getId() {
        return id;
    }

    public UUID getWorldUuid() {
        return worldUuid;
    }

    public BlockVector getPosition() {
        return position;
    }

    public String getMachineId() {
        return machineId;
    }

    public UUID getCreatedBy() {
        return createdBy;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public UUID getDisplayEntityUuid() {
        return displayEntityUuid;
    }

    public void setDisplayEntityUuid(UUID displayEntityUuid) {
        this.displayEntityUuid = displayEntityUuid;
    }

    public boolean isOutdated() {
        return outdated;
    }

    public void setOutdated(boolean outdated) {
        this.outdated = outdated;
    }
}
