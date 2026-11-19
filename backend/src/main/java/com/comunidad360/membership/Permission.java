package com.comunidad360.membership;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "permission")
public class Permission {

    @Id
    @Column(length = 60)
    private String code;

    @Column(name = "description_key", nullable = false, length = 120)
    private String descriptionKey;

    @Column(nullable = false, length = 40)
    private String module;

    protected Permission() {
    }

    public String getCode() {
        return code;
    }

    public String getDescriptionKey() {
        return descriptionKey;
    }

    public String getModule() {
        return module;
    }
}
