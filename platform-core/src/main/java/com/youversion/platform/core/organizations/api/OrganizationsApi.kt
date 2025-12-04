package com.youversion.platform.core.organizations.api

import com.youversion.platform.core.organizations.models.Organization

interface OrganizationsApi {
    suspend fun organizations(bibleVersionId: Int): List<Organization>

    suspend fun organization(organizationId: String): Organization
}
