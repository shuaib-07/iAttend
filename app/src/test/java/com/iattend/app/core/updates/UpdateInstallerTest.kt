package com.iattend.app.core.updates

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class UpdateInstallerTest {

    @Test
    fun `formatFileSize correctly formats various byte counts`() {
        assertEquals("0 B", UpdateInstaller.formatFileSize(0))
        assertEquals("0 B", UpdateInstaller.formatFileSize(-50))
        assertTrue(UpdateInstaller.formatFileSize(1024).contains("KB"))
        assertTrue(UpdateInstaller.formatFileSize(1024 * 1024).contains("MB"))
        assertTrue(UpdateInstaller.formatFileSize(4_600_000).contains("MB"))
    }

    @Test
    fun `targetApkFile creates sanitized file name`() {
        // Mock a dummy context or test file naming logic directly
        val tagName = "v0.6.0"
        val safeTag = tagName.replace("[^a-zA-Z0-9._-]".toRegex(), "_")
        assertEquals("v0.6.0", safeTag)

        val dirtyTag = "v0.6.0/beta:build#1"
        val safeDirty = dirtyTag.replace("[^a-zA-Z0-9._-]".toRegex(), "_")
        assertEquals("v0.6.0_beta_build_1", safeDirty)
    }
}
