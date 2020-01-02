package onion.w4v3xrmknycexlsd.app.hypercampus

import android.util.Log
import org.junit.Test
import org.junit.Assert.*

class DateTest {
    @Test
    fun date_isCorrect() {
        // needs to be updated when tested
        Log.e("date",currentDate().toString())
        assertEquals(currentDate(), 20191222)
    }
}
