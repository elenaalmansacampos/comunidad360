package com.comunidad360.providers;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "provider")
public class Provider {
    @Id private UUID id;
    @Column(name = "community_id", nullable = false) private UUID communityId;
    @Column(nullable = false, length = 200) private String name;
    @Column(name = "tax_id", length = 20) private String taxId;
    @Column(length = 100) private String category;
    @Column(length = 320) private String email;
    @Column(length = 50) private String phone;
    @Column(length = 300) private String address;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20) private ProviderStatus status;
    @Column(length = 2000) private String notes;
    @Column(name = "created_by_user_id") private UUID createdByUserId;
    @Column(name = "created_at", nullable = false) private Instant createdAt;
    @Column(name = "updated_at", nullable = false) private Instant updatedAt;
    @Version @Column(nullable = false) private long version;

    protected Provider() {}
    public Provider(UUID id, UUID communityId, String name, String taxId, String category, String email,
                    String phone, String address, String notes, UUID createdByUserId,
                    Instant createdAt, Instant updatedAt) {
        this.id = id; this.communityId = communityId; this.name = name; this.taxId = taxId;
        this.category = category; this.email = email; this.phone = phone; this.address = address;
        this.status = ProviderStatus.ACTIVE; this.notes = notes; this.createdByUserId = createdByUserId;
        this.createdAt = createdAt; this.updatedAt = updatedAt;
    }
    public UUID getId() { return id; }
    public UUID getCommunityId() { return communityId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getTaxId() { return taxId; }
    public void setTaxId(String taxId) { this.taxId = taxId; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }
    public ProviderStatus getStatus() { return status; }
    public void setStatus(ProviderStatus status) { this.status = status; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
    public long getVersion() { return version; }
}
