package eu.darken.sdmse.appcleaner.core.automation.specs

import android.content.Context
import android.content.pm.PackageManager
import android.view.accessibility.AccessibilityNodeInfo
import eu.darken.sdmse.common.pkgs.features.InstallId
import eu.darken.sdmse.common.pkgs.features.Installed
import eu.darken.sdmse.common.pkgs.toPkgId
import eu.darken.sdmse.common.storage.StorageStatsManager2
import eu.darken.sdmse.common.user.UserHandle2
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.spyk
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import testhelpers.BaseTest

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [29])
class OnTheFlyLablerTest : BaseTest() {

    private val statsManager = mockk<StorageStatsManager2>()
    private val packageManager = mockk<PackageManager>().apply {

    }
    private val context = mockk<Context>().apply {
        every { packageManager } returns this@OnTheFlyLablerTest.packageManager
    }
    private val testPkg = mockk<Installed>().apply {
        every { installId } returns InstallId("test.pkg".toPkgId(), UserHandle2(0))
    }

    suspend fun create(
        size: Long = 0,
        sizeTexts: Set<String> = emptySet(),
    ): StorageEntryFinder {
        val spy = spyk(StorageEntryFinder(context, statsManager)).apply {
            coEvery { determineTargetSize(testPkg) } returns size
            coEvery { generateTargetTexts(size) } returns sizeTexts
        }
        return spy
    }

    fun node(
        text: String,
        type: String = "android.widget.TextView",
        id: String? = null,
    ) = AccessibilityNodeInfo().apply {
        setText(text)
        viewIdResourceName = id
        className = type
    }

    @Test
    fun `match via summary`() = runTest {
        val finder = create(
            size = 79627776,
            sizeTexts = setOf("79.63 MB", "80 MB", "79,63 MB", "79.63 MB", "80 MB", "79,63 MB"),
        )
        finder.createSizeMatcher(testPkg)!!.invoke(
            node(
                text = "80 MB interner Speicher belegt",
                id = "android:id/summary"
            )
        ) shouldBe true
        finder.createSizeMatcher(testPkg)!!.invoke(
            node(
                text = "Im internen Speicher, 80 MB belegt",
                id = "android:id/summary"
            )
        ) shouldBe true
    }

    @Test
    fun `do not match partial numbers via summary`() = runTest {
        val finder = create(
            size = 79627776,
            sizeTexts = setOf("79.63 MB", "80 MB", "79,63 MB", "79.63 MB", "80 MB", "79,63 MB"),
        )
        finder.createSizeMatcher(testPkg)!!.invoke(
            node(
                text = "1.80 MB used since Jan 21",
                id = "android:id/summary"
            )
        ) shouldBe false
        finder.createSizeMatcher(testPkg)!!.invoke(
            node(
                text = "Used 1.80 MB since Jan 21",
                id = "android:id/summary"
            )
        ) shouldBe false
    }
}