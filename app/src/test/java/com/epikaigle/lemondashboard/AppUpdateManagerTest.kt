package com.epikaigle.lemondashboard

import org.junit.Assert.*
import org.junit.Test

class AppUpdateManagerTest {

    @Test
    fun testIsNewerVersion() {
        // Newer versions
        assertTrue(AppUpdateManager.isNewerVersion("1.5.2", "1.5.3"))
        assertTrue(AppUpdateManager.isNewerVersion("1.5.2", "1.6.0"))
        assertTrue(AppUpdateManager.isNewerVersion("1.5.2", "2.0.0"))
        assertTrue(AppUpdateManager.isNewerVersion("1.5.2", "v1.5.3"))
        assertTrue(AppUpdateManager.isNewerVersion("v1.5.2", "1.5.3"))
        assertTrue(AppUpdateManager.isNewerVersion("v1.5.2", "v1.5.3"))
        assertTrue(AppUpdateManager.isNewerVersion("1.5.2", "1.5.2.1"))
        assertTrue(AppUpdateManager.isNewerVersion("1.5", "1.5.1"))

        // Same or older versions
        assertFalse(AppUpdateManager.isNewerVersion("1.5.2", "1.5.2"))
        assertFalse(AppUpdateManager.isNewerVersion("v1.5.2", "1.5.2"))
        assertFalse(AppUpdateManager.isNewerVersion("1.5.2", "v1.5.2"))
        assertFalse(AppUpdateManager.isNewerVersion("1.5.2", "1.5.1"))
        assertFalse(AppUpdateManager.isNewerVersion("1.5.2", "1.4.9"))
        assertFalse(AppUpdateManager.isNewerVersion("2.0.0", "1.9.9"))
    }

    @Test
    fun testFormatFileSize() {
        assertEquals("0 o", AppUpdateManager.formatFileSize(0))
        assertEquals("500,0 o", AppUpdateManager.formatFileSize(500))
        assertEquals("1,0 Ko", AppUpdateManager.formatFileSize(1024))
        assertEquals("3,3 Mo", AppUpdateManager.formatFileSize(3475456))
    }
}
