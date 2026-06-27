package htt.pseudorandomometricsequencesapi.domain.distribution

import org.apache.commons.math3.random.JDKRandomGenerator
import org.apache.commons.math3.random.RandomGenerator
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class FakeCommonsRandomForRademacher : RandomGenerator {
    var value = 0.3
    override fun nextDouble() = value
    override fun nextGaussian() = 0.0
    override fun nextInt() = 0
    override fun nextInt(n: Int) = 0
    override fun nextLong() = 0L
    override fun setSeed(seed: Int) {}
    override fun setSeed(seed: Long) {}
    override fun setSeed(seed: IntArray?) {}
    override fun nextBoolean() = false
    override fun nextFloat() = 0.5f
    override fun nextBytes(bytes: ByteArray) {}
}

class RademacherGeneratorTest {

    private val fakeRandom = FakeCommonsRandomForRademacher()

    @Test
    fun `create should return a valid generator`() {
        val gen = RademacherGenerator.create(fakeRandom)
        assertNotNull(gen)
    }

    @Test
    fun `sample returns -1 when U is less than 0 5`() {
        fakeRandom.value = 0.3
        val gen = RademacherGenerator.create(fakeRandom)
        assertEquals(-1.0, gen.sample(), 1e-9)
    }

    @Test
    fun `sample returns +1 when U is greater than or equal to 0 5`() {
        fakeRandom.value = 0.7
        val gen = RademacherGenerator.create(fakeRandom)
        assertEquals(1.0, gen.sample(), 1e-9)
    }

    @Test
    fun `all samples should be exactly -1 or +1`() {
        val rng = JDKRandomGenerator().also { it.setSeed(42L) }
        val gen = RademacherGenerator.create(rng)
        repeat(10_000) {
            val x = gen.sample()
            assertTrue(x == -1.0 || x == 1.0)
        }
    }

    @Test
    fun `statistical mean should be close to 0`() {
        val rng = JDKRandomGenerator().also { it.setSeed(42L) }
        val gen = RademacherGenerator.create(rng)
        val mean = (1..50_000).map { gen.sample() }.average()
        assertEquals(0.0, mean, 0.02)
    }

    @Test
    fun `statistical variance should be close to 1`() {
        val rng = JDKRandomGenerator().also { it.setSeed(42L) }
        val gen = RademacherGenerator.create(rng)
        val samples = (1..50_000).map { gen.sample() }
        val mean = samples.average()
        val variance = samples.map { (it - mean) * (it - mean) }.average()
        assertEquals(1.0, variance, 0.02)
    }
}
