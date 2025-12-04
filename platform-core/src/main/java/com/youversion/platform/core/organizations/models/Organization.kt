package com.youversion.platform.core.organizations.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Organization(
    @SerialName(CodingKey.ID) val id: String,
    @SerialName(CodingKey.PARENT_ORG_ID) val parentOrgId: String?,
    @SerialName(CodingKey.NAME) val name: String,
    @SerialName(CodingKey.DESCRIPTION) val description: String?,
    @SerialName(CodingKey.EMAIL) val email: String?,
    @SerialName(CodingKey.PHONE) val phone: String?,
    @SerialName(CodingKey.PRIMARY_LANGUAGE) val primaryLanguage: String,
    @SerialName(CodingKey.WEBSITE_URL) val websiteUrl: String?,
    @SerialName(CodingKey.ADDRESS) val address: String?,
) {
    private object CodingKey {
        const val ID = "id"
        const val PARENT_ORG_ID = "parent_organization_id"
        const val NAME = "name"
        const val DESCRIPTION = "description"
        const val EMAIL = "email"
        const val PHONE = "phone"
        const val PRIMARY_LANGUAGE = "primary_language"
        const val WEBSITE_URL = "website_url"
        const val ADDRESS = "address"
    }

    // ----- Companion
    companion object {
        val preview: Organization
            get() =
                Organization(
                    id = "abc123",
                    parentOrgId = null,
                    name = "Biblica",
                    description = "Everyone deserves a chance to be transformed by Jesus",
                    email = null,
                    phone = null,
                    primaryLanguage = "en",
                    websiteUrl = "https://www.example.com",
                    address = null,
                )
    }
}
