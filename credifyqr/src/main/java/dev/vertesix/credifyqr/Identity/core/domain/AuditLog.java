package dev.vertesix.credifyqr.Identity.core.domain;

/**
 * Immutable audit trail event used for security and compliance logging.
 */
public class AuditLog {
    private final String id;
    private final String actorId;
    private final ActionType actionType;
    private final String targetId;
    private final String ipAddress;
    private final long timestamp;

    /**
     * Constructs a new audit log record.
     *
     * @param id unique audit event identifier
     * @param actorId id of the user or system component that performed the action
     * @param actionType the type of action performed
     * @param targetId the target entity of the action
     * @param ipAddress the IP address where the action originated
     * @param timestamp event creation timestamp in milliseconds
     */
    public AuditLog(String id, String actorId, ActionType actionType, String targetId, String ipAddress, long timestamp) {
        this.id = id;
        this.actorId = actorId;
        this.actionType = actionType;
        this.targetId = targetId;
        this.ipAddress = ipAddress;
        this.timestamp = timestamp;
    }

    /** @return audit record identifier */
    public String getId() { return id; }
    /** @return actor identifier */
    public String getActorId() { return actorId; }
    /** @return action type that was recorded */
    public ActionType getActionType() { return actionType; }
    /** @return target entity identifier */
    public String getTargetId() { return targetId; }
    /** @return IP address associated with the audit event */
    public String getIpAddress() { return ipAddress; }
    /** @return event timestamp in milliseconds */
    public long getTimestamp() { return timestamp; }
}