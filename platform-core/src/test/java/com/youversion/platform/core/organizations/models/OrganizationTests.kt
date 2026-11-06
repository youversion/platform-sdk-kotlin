package com.youversion.platform.core.organizations.models

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

class OrganizationTests {
    private fun organization(
        id: String = "id",
        parentOrgId: String? = "parent",
        name: String = "name",
        description: String? = "description",
        email: String? = "email",
        phone: String? = "phone",
        primaryLanguage: String = "en",
        websiteUrl: String? = "url",
        address: String? = "address",
    ) = Organization(
        id = id,
        parentOrgId = parentOrgId,
        name = name,
        description = description,
        email = email,
        phone = phone,
        primaryLanguage = primaryLanguage,
        websiteUrl = websiteUrl,
        address = address,
    )

    @Test
    fun `test values with the same fields are equal and hash alike`() {
        assertEquals(organization(), organization())
        assertEquals(organization().hashCode(), organization().hashCode())
    }

    @Test
    fun `test a difference in any field makes values unequal`() {
        listOf(
            organization(id = "other"),
            organization(parentOrgId = null),
            organization(name = "other"),
            organization(description = null),
            organization(email = null),
            organization(phone = null),
            organization(primaryLanguage = "es"),
            organization(websiteUrl = null),
            organization(address = null),
        ).forEach {
            assertNotEquals(organization(), it)
            assertNotEquals(organization().hashCode(), it.hashCode())
        }
    }

    @Test
    fun `test toString matches the existing format`() {
        assertEquals(
            "Organization(id=id, parentOrgId=parent, name=name, description=description, email=email, " +
                "phone=phone, primaryLanguage=en, websiteUrl=url, address=address)",
            organization().toString(),
        )
    }
}
