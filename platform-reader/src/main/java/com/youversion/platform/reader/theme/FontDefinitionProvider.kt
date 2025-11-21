package com.youversion.platform.reader.theme

import com.youversion.platform.reader.FontDefinition

interface FontDefinitionProvider {
    fun fonts(): List<FontDefinition>
}
