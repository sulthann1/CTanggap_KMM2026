package id.ctanggap.app.detection

import org.junit.Assert.assertEquals
import org.junit.Test
import kotlin.random.Random

class CrashDetectorTest {
    private val g = 9.80665f
    private val rnd = Random(1)

    /** Simulasi: 3 detik melaju, satu sampel benturan, lalu 14 detik pascabenturan. */
    private fun run(
        speedBefore: Float = 40f, speedAfter: Float = 0f, impactG: Float = 6f,
        noiseAfterG: Float = 0.02f
    ): Int {
        var events = 0
        val d = CrashDetector(CrashDetector.Config()) { events++ }
        var t = 0L
        while (t < 3_000) {
            d.onSpeed(t, speedBefore)
            d.onAccel(t, 0f, 0f, g + noise(0.05f))
            t += 20
        }
        d.onAccel(t, 0f, 0f, impactG * g); t += 20
        while (t < 3_000 + 14_000) {
            d.onSpeed(t, speedAfter)
            d.onAccel(t, 0f, 0f, g + noise(noiseAfterG))
            t += 20
        }
        return events
    }

    private fun noise(a: Float) = (rnd.nextFloat() - 0.5f) * 2 * a * g

    @Test fun detectsImpactThenStill() = assertEquals(1, run())
    @Test fun ignoresImpactIfStillMoving() = assertEquals(0, run(speedAfter = 35f))
    @Test fun ignoresImpactIfPhoneKeepsShaking() = assertEquals(0, run(noiseAfterG = 0.6f))
    @Test fun ignoresImpactWhenNotDriving() = assertEquals(0, run(speedBefore = 3f))
    @Test fun ignoresSmallBump() = assertEquals(0, run(impactG = 2f))
}
