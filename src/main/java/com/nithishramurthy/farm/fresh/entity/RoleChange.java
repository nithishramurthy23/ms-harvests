package com.nithishramurthy.farm.fresh.entity;

import jakarta.persistence.*;

import java.time.Instant;

/** Audit log: who gave whom which role, and when. */
@Entity
@Table(name = "role_changes", indexes = @Index(name = "idx_role_changes_changed_at", columnList = "changed_at"))
public class RoleChange {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "from_role", length = 20, nullable = false)
    private Role fromRole;

    @Enumerated(EnumType.STRING)
    @Column(name = "to_role", length = 20, nullable = false)
    private Role toRole;

    @Column(name = "changed_by", length = 255, nullable = false)
    private String changedBy;

    @Column(name = "changed_at", nullable = false)
    private Instant changedAt = Instant.now();

    protected RoleChange() {}

    public RoleChange(User user, Role fromRole, Role toRole, String changedBy) {
        this.user = user;
        this.fromRole = fromRole;
        this.toRole = toRole;
        this.changedBy = changedBy;
    }

    public Long getId() { return id; }
    public User getUser() { return user; }
    public Role getFromRole() { return fromRole; }
    public Role getToRole() { return toRole; }
    public String getChangedBy() { return changedBy; }
    public Instant getChangedAt() { return changedAt; }
}
