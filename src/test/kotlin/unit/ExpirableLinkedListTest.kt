package unit

import ExpirableLinkedList
import ExpirableLinkedListItem
import Quote
import org.junit.jupiter.api.Test
import java.time.Duration
import java.time.Instant
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ExpirableLinkedListTest {

    @Test
    fun addTest_shouldAddHeadWhenListIsEmpty() {
        val testList: ExpirableLinkedList<Quote> = ExpirableLinkedList(Duration.ofMillis(100))
        val added = testList.add(ExpirableLinkedListItem<Quote>(Quote("1", 1.0), Instant.now()))

        assertTrue { added }
        assertEquals(1, testList.size)
    }

    @Test
    fun addTest_shouldRemoveElementsAddedEarlierThanTimeIntervalFromLastElement() {
        val testList: ExpirableLinkedList<Quote> = ExpirableLinkedList(Duration.ofMillis(100))

        val first = ExpirableLinkedListItem(Quote("1", 1.0), Instant.ofEpochMilli(3))
        val second = ExpirableLinkedListItem(Quote("1", 2.0), Instant.ofEpochMilli(40))
        val third = ExpirableLinkedListItem(Quote("2", 2.0), Instant.ofEpochMilli(90))
        val fourth = ExpirableLinkedListItem(Quote("2", 2.0), Instant.ofEpochMilli(110))

        testList.add(first)
        testList.add(second)
        testList.add(third)

        assertEquals(3, testList.size)

        testList.add(fourth)

        assertEquals(3, testList.size)

        assertTrue {
            testList.contains(second)
            testList.contains(third)
            testList.contains(fourth)
        }
    }

}