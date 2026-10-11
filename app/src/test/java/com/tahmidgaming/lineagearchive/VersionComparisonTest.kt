package com.tahmidgaming.lineagearchive

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class VersionComparisonTest {
    @Test
    fun comparesNumericComponentsRatherThanStrings() {
        assertTrue(compareVersions("2.0.0", "1.0.37") > 0)
        assertTrue(compareVersions("1.0.10", "1.0.9") > 0)
        assertTrue(compareVersions("1.0.37", "1.0.37") == 0)
        assertTrue(compareVersions("1.0.36", "1.0.37") < 0)
    }

    @Test
    fun ignoresVersionPrefixesAndMissingTrailingComponents() {
        assertEquals(0, compareVersions("v2.0", "2.0.0"))
    }

    @Test
    fun updateFlagRequiresKnownInstalledVersionAndNewerRemoteVersion() {
        assertTrue(UpdateItem("App", "1.0.37", "2.0.0").availableUpdate)
        assertFalse(UpdateItem("App", "not installed", "2.0.0").availableUpdate)
        assertFalse(UpdateItem("App", "2.0.0", "1.0.37").availableUpdate)
        assertFalse(UpdateItem("App", "1.0.37", null).availableUpdate)
    }
}
