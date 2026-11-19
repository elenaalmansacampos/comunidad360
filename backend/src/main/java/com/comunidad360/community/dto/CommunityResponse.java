package com.comunidad360.community.dto;

import com.comunidad360.community.Community;

import java.util.UUID;

public record CommunityResponse(
        UUID id,
        String name,
        String taxId,
        String address,
        String city,
        String province,
        String postalCode,
        String timezone,
        String defaultLanguage,
        String currency,
        String status,
        long version
) {
    public static CommunityResponse from(Community community) {
        return new CommunityResponse(
                community.getId(),
                community.getName(),
                community.getTaxId(),
                community.getAddress(),
                community.getCity(),
                community.getProvince(),
                community.getPostalCode(),
                community.getTimezone(),
                community.getDefaultLanguage(),
                community.getCurrency(),
                community.getStatus().name(),
                community.getVersion()
        );
    }
}
