package com.comunidad360.providers;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "provider_contact")
public class ProviderContact {
    @Id private UUID id;
    @Column(name = "community_id", nullable = false) private UUID communityId;
    @Column(name = "provider_id", nullable = false) private UUID providerId;
    @Column(nullable = false, length = 200) private String name;
    @Column(length = 100) private String role;
    @Column(length = 320) private String email;
    @Column(length = 50) private String phone;
    @Column(name = "is_primary", nullable = false) private boolean primaryContact;
    @Column(name = "created_at", nullable = false) private Instant createdAt;
    @Column(name = "updated_at", nullable = false) private Instant updatedAt;

    protected ProviderContact() {}
    public ProviderContact(UUID id, UUID communityId, UUID providerId, String name, String role,
                           String email, String phone, boolean primaryContact,
                           Instant createdAt, Instant updatedAt) {
        this.id = id; this.communityId = communityId; this.providerId = providerId; this.name = name;
        this.role = role; this.email = email; this.phone = phone; this.primaryContact = primaryContact;
        this.createdAt = createdAt; this.updatedAt = updatedAt;
    }
    public UUID getId() { return id; }
    public UUID getProviderId() { return providerId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public boolean isPrimaryContact() { return primaryContact; }
    public void setPrimaryContact(boolean primaryContact) { this.primaryContact = primaryContact; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
