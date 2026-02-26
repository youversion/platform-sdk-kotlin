package com.youversion.platform.core.di

@RequiresOptIn(level = RequiresOptIn.Level.ERROR, message = "Internal library API, do not use.")
@Retention(AnnotationRetention.BINARY)
@Target(
    AnnotationTarget.CLASS,
    AnnotationTarget.TYPEALIAS,
    AnnotationTarget.FUNCTION,
    AnnotationTarget.PROPERTY,
    AnnotationTarget.FIELD,
    AnnotationTarget.CONSTRUCTOR,
)
annotation class PlatformInternalApi
