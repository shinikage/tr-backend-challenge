package unit

import CandlestickManagerImpl
import Quote
import TimestampManager
import org.junit.jupiter.api.Test
import java.time.Duration
import java.time.Instant
import java.time.temporal.ChronoUnit
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CandlestickManagerImplTest {

    @Test
    fun getCandlesticks_shouldReturnEmptyListIfThereAreNoQuotes() {

        val timestampManager = object : TimestampManager {
            var time: Instant = Instant.now().minusNanos(Duration.ofSeconds(12).toNanos())
            override fun now(): Instant {
                time = time.plusNanos(Duration.ofSeconds(2).toNanos())
                return time
            }
        }

        val candlestickManager = CandlestickManagerImpl(10, 2, ChronoUnit.SECONDS, timestampManager)
        val candlesticks = candlestickManager.getCandlesticks("1")
        assertTrue { candlesticks.isEmpty() }
    }

    @Test
    fun getCandlesticks_candlesticksShouldBeSameForAllIntervals() {

        val timestampManager = object : TimestampManager {
            var i = 0
            override fun now(): Instant {
                if (i <= 2) {
                    i++
                    return Instant.now().minusNanos(Duration.ofSeconds(8).toNanos())
                } else {
                    return Instant.now()
                }
            }
        }

        val candlestickManager = CandlestickManagerImpl(9, 3, ChronoUnit.SECONDS, timestampManager)

        candlestickManager.addQuote(Quote("1", 1.0))
        candlestickManager.addQuote(Quote("1", 3.0))
        candlestickManager.addQuote(Quote("1", 2.0))

        val candlesticks = candlestickManager.getCandlesticks("1")
        assertEquals(3, candlesticks.size)

        val first = candlesticks[0]
        val second = candlesticks[1]
        val third = candlesticks[2]

        assertEquals(1.0, first.openPrice)
        assertEquals(2.0, first.closingPrice)
        assertEquals(1.0, first.lowPrice)
        assertEquals(3.0, first.highPrice)

        assertEquals(1.0, second.openPrice)
        assertEquals(2.0, second.closingPrice)
        assertEquals(1.0, second.lowPrice)
        assertEquals(3.0, second.highPrice)

        assertEquals(1.0, third.openPrice)
        assertEquals(2.0, third.closingPrice)
        assertEquals(1.0, third.lowPrice)
        assertEquals(3.0, third.highPrice)
    }

    @Test
    fun getCandlesticks_candlesticksShouldBeSameStartingFromSecondInterval() {

        val timestampManager = object : TimestampManager {
            var i = 0
            override fun now(): Instant {
                if (i <= 2) {
                    i++
                    return Instant.now().minusNanos(Duration.ofSeconds(5).toNanos())
                } else {
                    return Instant.now()
                }
            }
        }

        val candlestickManager = CandlestickManagerImpl(9, 3, ChronoUnit.SECONDS, timestampManager)

        candlestickManager.addQuote(Quote("1", 1.0))
        candlestickManager.addQuote(Quote("1", 3.0))
        candlestickManager.addQuote(Quote("1", 2.0))

        val candlesticks = candlestickManager.getCandlesticks("1")
        assertEquals(3, candlesticks.size)

        val first = candlesticks[0]
        val second = candlesticks[1]
        val third = candlesticks[2]

        assertEquals(-1.0, first.openPrice)
        assertEquals(-1.0, first.closingPrice)
        assertEquals(-1.0, first.lowPrice)
        assertEquals(-1.0, first.highPrice)

        assertEquals(1.0, second.openPrice)
        assertEquals(2.0, second.closingPrice)
        assertEquals(1.0, second.lowPrice)
        assertEquals(3.0, second.highPrice)

        assertEquals(1.0, third.openPrice)
        assertEquals(2.0, third.closingPrice)
        assertEquals(1.0, third.lowPrice)
        assertEquals(3.0, third.highPrice)
    }

    @Test
    fun getCandlesticks_candlesticksShouldBeSameStartingFromThirdInterval() {

        val timestampManager = object : TimestampManager {
            var i = 0
            override fun now(): Instant {
                if (i <= 2) {
                    i++
                    return Instant.now().minusNanos(Duration.ofSeconds(2).toNanos())
                } else {
                    return Instant.now()
                }
            }
        }

        val candlestickManager = CandlestickManagerImpl(9, 3, ChronoUnit.SECONDS, timestampManager)

        candlestickManager.addQuote(Quote("1", 1.0))
        candlestickManager.addQuote(Quote("1", 3.0))
        candlestickManager.addQuote(Quote("1", 2.0))

        val candlesticks = candlestickManager.getCandlesticks("1")
        assertEquals(3, candlesticks.size)

        val first = candlesticks[0]
        val second = candlesticks[1]
        val third = candlesticks[2]

        assertEquals(-1.0, first.openPrice)
        assertEquals(-1.0, first.closingPrice)
        assertEquals(-1.0, first.lowPrice)
        assertEquals(-1.0, first.highPrice)

        assertEquals(-1.0, second.openPrice)
        assertEquals(-1.0, second.closingPrice)
        assertEquals(-1.0, second.lowPrice)
        assertEquals(-1.0, second.highPrice)

        assertEquals(1.0, third.openPrice)
        assertEquals(2.0, third.closingPrice)
        assertEquals(1.0, third.lowPrice)
        assertEquals(3.0, third.highPrice)
    }

    @Test
    fun getCandlesticks_absentCandlesticksInIntervalUsePreviousCandle() {

        val timestampManager = object : TimestampManager {
            var i = 0
            override fun now(): Instant {
                if (i <= 2) {
                    i++
                    return Instant.now().minusNanos(Duration.ofSeconds(8).toNanos())
                } else if (i < 6) {
                    i++
                    return Instant.now().minusNanos(Duration.ofSeconds(2).toNanos())
                } else {
                    return Instant.now()
                }
            }
        }

        val candlestickManager = CandlestickManagerImpl(9, 3, ChronoUnit.SECONDS, timestampManager)

        candlestickManager.addQuote(Quote("1", 1.0))
        candlestickManager.addQuote(Quote("1", 3.0))
        candlestickManager.addQuote(Quote("1", 2.0))

        candlestickManager.addQuote(Quote("1", 4.0))
        candlestickManager.addQuote(Quote("1", 5.0))
        candlestickManager.addQuote(Quote("1", 2.0))

        val candlesticks = candlestickManager.getCandlesticks("1")
        assertEquals(3, candlesticks.size)

        val first = candlesticks[0]
        val second = candlesticks[1]
        val third = candlesticks[2]

        assertEquals(1.0, first.openPrice)
        assertEquals(2.0, first.closingPrice)
        assertEquals(1.0, first.lowPrice)
        assertEquals(3.0, first.highPrice)

        // previous candle is still here
        assertEquals(1.0, second.openPrice)
        assertEquals(2.0, second.closingPrice)
        assertEquals(1.0, second.lowPrice)
        assertEquals(3.0, second.highPrice)

        assertEquals(4.0, third.openPrice)
        assertEquals(2.0, third.closingPrice)
        assertEquals(2.0, third.lowPrice)
        assertEquals(5.0, third.highPrice)
    }

    @Test
    fun getCandlesticks_shouldSkipQuotesEarlierAndOutsideInterval() {

        val timestampManager = object : TimestampManager {
            var i = 0
            override fun now(): Instant {
                if (i <= 2) {
                    i++
                    return Instant.now().minusNanos(Duration.ofSeconds(12).toNanos())
                } else {
                    return Instant.now()
                }
            }
        }

        val candlestickManager = CandlestickManagerImpl(9, 3, ChronoUnit.SECONDS, timestampManager)

        candlestickManager.addQuote(Quote("1", 1.0))
        candlestickManager.addQuote(Quote("1", 3.0))
        candlestickManager.addQuote(Quote("1", 2.0))

        val candlesticks = candlestickManager.getCandlesticks("1")
        assertEquals(3, candlesticks.size)

        val first = candlesticks[0]
        val second = candlesticks[1]
        val third = candlesticks[2]

        assertEquals(-1.0, first.openPrice)
        assertEquals(-1.0, first.closingPrice)
        assertEquals(-1.0, first.lowPrice)
        assertEquals(-1.0, first.highPrice)

        assertEquals(-1.0, second.openPrice)
        assertEquals(-1.0, second.closingPrice)
        assertEquals(-1.0, second.lowPrice)
        assertEquals(-1.0, second.highPrice)

        assertEquals(-1.0, third.openPrice)
        assertEquals(-1.0, third.closingPrice)
        assertEquals(-1.0, third.lowPrice)
        assertEquals(-1.0, third.highPrice)
    }
}