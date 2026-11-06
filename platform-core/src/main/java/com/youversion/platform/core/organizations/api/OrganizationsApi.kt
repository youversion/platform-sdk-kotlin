package com.youversion.platform.core.organizations.api

import com.youversion.platform.core.organizations.models.Organization

public interface OrganizationsApi {
    public suspend fun organizations(bibleVersionId: Int): List<Organization>

    public suspend fun organization(organizationId: String): Organization
}
