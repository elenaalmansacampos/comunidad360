package com.comunidad360.board;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "board_note_comment")
public class BoardNoteComment {

    @Id
    private UUID id;

    @Column(name = "community_id", nullable = false)
    private UUID communityId;

    @Column(name = "note_id", nullable = false)
    private UUID noteId;

    @Column(name = "author_user_id")
    private UUID authorUserId;

    @Column(nullable = false, length = 1000)
    private String body;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected BoardNoteComment() {
    }

    public BoardNoteComment(UUID id, UUID communityId, UUID noteId, UUID authorUserId,
                            String body, Instant createdAt) {
        this.id = id;
        this.communityId = communityId;
        this.noteId = noteId;
        this.authorUserId = authorUserId;
        this.body = body;
        this.createdAt = createdAt;
    }

    public UUID getId() {
        return id;
    }

    public UUID getCommunityId() {
        return communityId;
    }

    public UUID getNoteId() {
        return noteId;
    }

    public UUID getAuthorUserId() {
        return authorUserId;
    }

    public String getBody() {
        return body;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
