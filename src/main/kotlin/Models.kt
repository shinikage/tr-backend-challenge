import java.time.Duration
import java.time.Instant
import java.time.temporal.TemporalUnit
import java.util.LinkedHashMap
import java.util.LinkedList
import kotlin.collections.HashMap

data class InstrumentEvent(val type: Type, val data: Instrument) {
    enum class Type {
        ADD,
        DELETE
    }
}

data class QuoteEvent(val data: Quote)

data class Instrument(val isin: ISIN, val description: String)
typealias ISIN = String

data class Quote(val isin: ISIN, val price: Price)
typealias Price = Double

interface TimestampManager {
    /**
     * Returns current time in the for of an instant.
     */
    fun now(): Instant
}

class TimestampManagerImpl : TimestampManager {
    override fun now(): Instant {
        return Instant.now()
    }

}

interface CandlestickManager {
    fun getCandlesticks(isin: String): List<Candlestick>
    fun addQuote(quote: Quote)
    fun addInstrument(instrument: Instrument)
    fun deleteInstrument(instrument: Instrument)
}

/**
 * A CandlestickManager. It accepts quotes and instruments, manages them (add and delete), stores them
 * and gather statistics for multiple Quotes in form of Candlesticks.
 *
 * @windowInterval denotes a time window per which multiple Candlesticks should be gathered and returned to the user
 * @candlestickInterval is a time period per which single Candlestick is created.
 *
 * Example: windowInterval - 30 minutes, candlestickInterval - 1 minute, therefore the user will get 30 candlesticks
 * in general if the "getCandlesticks" api of this service is called.
 *
 * Incoming quotes and instruments are stored temporarily stored in in-memory HashMap storage, where isin
 * is the key and the list of quotes is the value.
 *
 * The quotes per each isin are stored in a customized data structure called ExpirableLinkedList.
 * ExpirableLinkedList accepts a time interval (in this case of this service - a @windowInterval) as a parameter.
 * When an item is added to the ExpirableLinkedList, it's checked, whether the item's timestamp is included into the interval,
 * and the items which are not in the interval are removed.
 *
 * @temporalUnit parameter denotes a temporal unit (minute, second and so on).
 * @timestampManager a service, where the current system time (e.g. "now") could be obtained from.
 */
class CandlestickManagerImpl(
    private val windowInterval: Long,
    private val candlestickInterval: Long,
    private val temporalUnit: TemporalUnit,
    private val timestampManager: TimestampManager
) :
    CandlestickManager {

    private val quotesByIsin: HashMap<String, ExpirableLinkedList<Quote>> = HashMap()

    private fun generateQuotesByTimestamp(): LinkedHashMap<Instant, LinkedList<ExpirableLinkedListItem<Quote>>> {
        val timeranges = LinkedHashMap<Instant, LinkedList<ExpirableLinkedListItem<Quote>>>()
        val now = timestampManager.now()
        var currentTimestamp = now.minus(windowInterval, temporalUnit)

        while (currentTimestamp <= now) {
            timeranges.put(currentTimestamp, LinkedList())
            currentTimestamp = currentTimestamp.plus(candlestickInterval, temporalUnit)
        }
        timeranges.put(now, LinkedList())
        return timeranges
    }

    private fun fillQuotesByTimestamp(quotes: ExpirableLinkedList<Quote>): LinkedHashMap<Instant, LinkedList<ExpirableLinkedListItem<Quote>>> {
        val quotesByTimestamp = generateQuotesByTimestamp()
        val timestamps = quotesByTimestamp.keys.toList()

        for (quote in quotes) {
            var ind = timestamps.binarySearch { i -> if (quote.timestamp == i) 0 else (if (quote.timestamp < i) 1 else -1) }
            if (ind < -1) {
                ind = -1 * ind - 2
            }
            if (ind >= 0) {
                quotesByTimestamp.get(timestamps[ind])?.add(quote)
            }
        }
        return quotesByTimestamp
    }

    @Synchronized
    override fun getCandlesticks(isin: String): List<Candlestick> {
        val quotes = quotesByIsin.getOrDefault(isin, ExpirableLinkedList(Duration.of(windowInterval, temporalUnit)))

        if (quotes.isEmpty()) {
            return emptyList()
        }

        val quotesByTimestamp = fillQuotesByTimestamp(quotes)
        val timestamps = quotesByTimestamp.keys.toList()
        val result = LinkedList<Candlestick>()

        var lastCandlestick =
            Candlestick(timestamps[0].minus(candlestickInterval, temporalUnit), timestamps[0], -1.0, -1.0, -1.0, -1.0)

        for (i in 0 until timestamps.size - 1) {

            val quoteRange = quotesByTimestamp[timestamps[i]]
            if (quoteRange == null || quoteRange.isEmpty()) {
                // no previous timestamp
                if (i <= 0) {
                    lastCandlestick = Candlestick(
                        timestamps[0],
                        timestamps[1],
                        -1.0,
                        -1.0,
                        -1.0,
                        -1.0
                    )
                } else {
                    lastCandlestick = Candlestick(
                        timestamps[i],
                        timestamps[i + 1],
                        lastCandlestick.openPrice,
                        lastCandlestick.highPrice,
                        lastCandlestick.lowPrice,
                        lastCandlestick.closingPrice
                    )
                }
                result.add(lastCandlestick)
                continue
            }

            val max = quoteRange.map { it.value.price }.maxOrNull()
            val min = quoteRange.map { it.value.price }.minOrNull()

            lastCandlestick = Candlestick(
                timestamps[i],
                timestamps[i + 1],
                quoteRange.first.value.price,
                max ?: -1.0,
                min ?: -1.0,
                quoteRange.last.value.price
            )
            result.add(lastCandlestick)
        }

        return result
    }

    @Synchronized
    override fun addQuote(quote: Quote) {
        var quotes = quotesByIsin.getOrDefault(quote.isin, null)
        if (quotes == null) {
            quotes = ExpirableLinkedList(Duration.of(windowInterval, temporalUnit))
            quotesByIsin.put(quote.isin, quotes)
        }
        quotes.add(ExpirableLinkedListItem(quote, timestampManager.now()))
    }

    @Synchronized
    override fun addInstrument(instrument: Instrument) {
        quotesByIsin[instrument.isin] = ExpirableLinkedList(Duration.of(windowInterval, temporalUnit))
    }

    @Synchronized
    override fun deleteInstrument(instrument: Instrument) {
        quotesByIsin.remove(instrument.isin)
    }
}

data class ExpirableLinkedListItem<T>(val value: T, val timestamp: Instant)

class ExpirableLinkedList<T>(private val flushInterval: Duration) : LinkedList<ExpirableLinkedListItem<T>>() {

    private fun withinTimeInterval(element: ExpirableLinkedListItem<T>): Boolean {
        return kotlin.math.abs(
            Duration.between(element.timestamp, super.getFirst().timestamp).toNanos()
        ) <= flushInterval.toNanos()
    }

    override fun add(element: ExpirableLinkedListItem<T>): Boolean {
        if (super.isEmpty() || withinTimeInterval(element)) {
            return super.add(element)
        }
        // flush
        val i = super.iterator()
        while (i.hasNext()) {
            if (!withinTimeInterval(element)) {
                super.remove(i.next())
            } else {
                break
            }
        }
        return super.add(element)
    }
}

data class Candlestick(
    val openTimestamp: Instant,
    var closeTimestamp: Instant,
    val openPrice: Price,
    var highPrice: Price,
    var lowPrice: Price,
    var closingPrice: Price
)