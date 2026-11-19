package com.comunidad360.membership;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface RolePermissionRepository extends JpaRepository<RolePermission, RolePermissionId> {

    @Query("""
            select rp.permissionCode
            from RolePermission rp
            where rp.roleCode in :roleCodes and rp.granted = true
            """)
    List<String> findGrantedPermissions(@Param("roleCodes") Collection<String> roleCodes);
}
