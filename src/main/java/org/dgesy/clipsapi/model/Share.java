package org.dgesy.clipsapi.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "shares")
public class Share {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "clip_id", nullable = false)
    private Clip clip;

    @ManyToOne
    @JoinColumn(name = "shared_by_user_id", nullable = false)
    private User sharedBy;

    @ManyToOne
    @JoinColumn(name = "shared_with_user_id", nullable = false)
    private User sharedWith;

    private LocalDateTime createdAt = LocalDateTime.now();

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Clip getClip() { return clip; }
    public void setClip(Clip clip) { this.clip = clip; }
    public User getSharedBy() { return sharedBy; }
    public void setSharedBy(User sharedBy) { this.sharedBy = sharedBy; }
    public User getSharedWith() { return sharedWith; }
    public void setSharedWith(User sharedWith) { this.sharedWith = sharedWith; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}