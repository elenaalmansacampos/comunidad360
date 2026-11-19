package com.comunidad360.membership;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "role")
public class Role {

    @Id
    @Column(length = 40)
    private String code;

    @Column(name = "name_key", nullable = false, length = 120)
    private String nameKey;

    @Column(nullable = false, length = 20)
    private String scope;

    @Column(name = "is_system", nullable = false)
    private boolean system;

    protected Role() {
    }

    public String getCode() {
        return code;
    }

    public String getNameKey() {
        return nameKey;
    }

    public String getScope() {
        return scope;
    }

    public boolean isSystem() {
        return system;
    }
}
