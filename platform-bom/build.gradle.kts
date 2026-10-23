plugins {
    `java-platform`
    id("youversion.publishing")
    id("youversion.spotless")
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
    coordinates(artifactId = "platform-bom")

    pom {
        name = "YouVersion Platform SDK - BOM"
        description =
            """
            Keeps the YouVersion Platform SDK modules on the same version.
            """.trimIndent()
    }
}
