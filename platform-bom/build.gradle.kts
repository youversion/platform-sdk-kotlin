plugins {
    `java-platform`
    alias(libs.plugins.maven.publish)
}

// Pins every SDK module to this BOM's version. Each module also depends on the BOM, so a
// consumer that names the modules directly still resolves them all to one version.
dependencies {
    constraints {
        api(projects.platformCore)
        api(projects.platformUi)
        api(projects.platformReader)
    }
}

mavenPublishing {
    coordinates(
        groupId = "com.youversion.platform",
        artifactId = "platform-bom",
        // The release workflow passes -PsdkVersion so the published coordinate and the
        // version stamped into the release commit cannot drift. Local builds fall back to
        // the catalog. See RELEASING.md.
        version =
            (project.findProperty("sdkVersion") as? String)
                ?: libs.versions.youversionPlatform.get(),
    )

    pom {
        name = "YouVersion Platform SDK - BOM"
        description =
            """
            Keeps the YouVersion Platform SDK modules on the same version.
            """.trimIndent()
    }
}
