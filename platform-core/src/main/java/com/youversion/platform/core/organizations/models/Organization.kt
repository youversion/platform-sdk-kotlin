package com.youversion.platform.core.organizations.models

import com.youversion.platform.core.di.PlatformInternalApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
public class Organization
    @PlatformInternalApi
    constructor(
        @SerialName(CodingKey.ID) public val id: String,
        @SerialName(CodingKey.PARENT_ORG_ID) public val parentOrgId: String?,
        @SerialName(CodingKey.NAME) public val name: String,
        @SerialName(CodingKey.DESCRIPTION) public val description: String?,
        @SerialName(CodingKey.EMAIL) public val email: String?,
        @SerialName(CodingKey.PHONE) public val phone: String?,
        @SerialName(CodingKey.PRIMARY_LANGUAGE) public val primaryLanguage: String,
        @SerialName(CodingKey.WEBSITE_URL) public val websiteUrl: String?,
        @SerialName(CodingKey.ADDRESS) public val address: String?,
    ) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is Organization) return false
            return id == other.id &&
                parentOrgId == other.parentOrgId &&
                name == other.name &&
                description == other.description &&
                email == other.email &&
                phone == other.phone &&
                primaryLanguage == other.primaryLanguage &&
                websiteUrl == other.websiteUrl &&
                address == other.address
        }

        override fun hashCode(): Int {
            var result = id.hashCode()
            result = 31 * result + parentOrgId.hashCode()
            result = 31 * result + name.hashCode()
            result = 31 * result + description.hashCode()
            result = 31 * result + email.hashCode()
            result = 31 * result + phone.hashCode()
            result = 31 * result + primaryLanguage.hashCode()
            result = 31 * result + websiteUrl.hashCode()
            result = 31 * result + address.hashCode()
            return result
        }

        override fun toString(): String =
            "Organization(id=$id, parentOrgId=$parentOrgId, name=$name, description=$description, email=$email, " +
                "phone=$phone, primaryLanguage=$primaryLanguage, websiteUrl=$websiteUrl, address=$address)"

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
        public companion object {
            public val preview: Organization
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
