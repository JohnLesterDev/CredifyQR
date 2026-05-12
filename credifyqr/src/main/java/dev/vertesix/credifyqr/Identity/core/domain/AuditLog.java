package dev.vertesix.credifyqr.Identity.core.domain;

// Created core entity to track immutable audit trail events
public class AuditLog {
    private final String id;
    private final String actorId;
    private final ActionType actionType;
    private final String targetId;
    private final String ipAddress;
    private final long timestamp;

    public AuditLog(String id, String actorId, ActionType actionType, String targetId, String ipAddress, long timestamp) {
        this.id = id;
        this.actorId = actorId;
        this.actionType = actionType;
        this.targetId = targetId;
        this.ipAddress = ipAddress;
        this.timestamp = timestamp;
    }

    public String getId() { return id; }
    public String getActorId() { return actorId; }
    public ActionType getActionType() { return actionType; }
    public String getTargetId() { return targetId; }
    public String getIpAddress() { return ipAddress; }
    public long getTimestamp() { return timestamp; }
}