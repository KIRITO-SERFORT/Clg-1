package com.example.util

/**
 * Pure helpers for UPI deep links (no Android types, so they are unit tested on the JVM).
 *
 * What this can and cannot do:
 *  - It builds a standard person-to-person link: upi://pay?pa=&pn=&am=&cu=INR&tn=
 *    (no merchant code, no signature, so apps treat it as a normal "pay a person" request).
 *  - It can read the status a UPI app hands back. That status is reported by the other app and is
 *    NOT proof the money arrived, so callers must word results as "the app reported ...".
 */
object UpiLink {
    const val MIN_RUPEES = 1
    const val MAX_RUPEES = 100_000

    // name@handle ; handle is letters/digits (okaxis, oksbi, ybl, upi, paytm ...)
    private val VPA = Regex("^[A-Za-z0-9._-]{2,64}@[A-Za-z][A-Za-z0-9]{1,30}$")

    fun isValidVpa(vpa: String): Boolean = VPA.matches(vpa.trim())

    /** RFC 3986 percent-encoding of UTF-8 bytes; only unreserved characters are left as they are. */
    fun encode(value: String): String {
        val sb = StringBuilder()
        for (b in value.toByteArray(Charsets.UTF_8)) {
            val c = b.toInt() and 0xFF
            val unreserved = (c in 'A'.code..'Z'.code) || (c in 'a'.code..'z'.code) || (c in '0'.code..'9'.code) ||
                c == '-'.code || c == '.'.code || c == '_'.code || c == '~'.code
            if (unreserved) sb.append(c.toChar()) else sb.append('%').append("%02X".format(c))
        }
        return sb.toString()
    }

    /** Transaction notes are kept to plain, short ASCII: some UPI apps reject or garble anything else. */
    fun cleanNote(note: String): String =
        note.filter { it.isLetterOrDigit() && it.code < 128 || it == ' ' || it == '.' || it == ',' || it == '-' }
            .trim().replace(Regex(" +"), " ").take(40)

    /** "50.00" for a whole-rupee amount, or null when no (valid) amount should be sent. */
    fun amount(rupees: Int?): String? =
        if (rupees == null || rupees < MIN_RUPEES || rupees > MAX_RUPEES) null else "$rupees.00"

    /** Returns the link, or throws IllegalArgumentException if the payee id is not a valid UPI id. */
    fun build(vpa: String, payeeName: String, rupees: Int?, note: String): String {
        val id = vpa.trim()
        require(isValidVpa(id)) { "Invalid UPI id" }
        val sb = StringBuilder("upi://pay?pa=").append(id)          // '@' stays literal, as in the UPI spec examples
        sb.append("&pn=").append(encode(payeeName.trim().take(40)))
        amount(rupees)?.let { sb.append("&am=").append(it) }
        sb.append("&cu=INR")
        cleanNote(note).takeIf { it.isNotEmpty() }?.let { sb.append("&tn=").append(encode(it)) }
        return sb.toString()
    }

    enum class Outcome { SUCCESS, FAILURE, PENDING, UNKNOWN }

    data class Result(val outcome: Outcome, val txnId: String?)

    /**
     * UPI apps return either a "response" string like "txnId=A1&responseCode=00&Status=SUCCESS&txnRef=..."
     * or a plain "Status" extra. Anything unreadable is UNKNOWN (never assumed to be success).
     */
    fun parseResult(response: String?, statusExtra: String?): Result {
        val map = HashMap<String, String>()
        response?.split('&')?.forEach { part ->
            val i = part.indexOf('=')
            if (i > 0) map[part.substring(0, i).trim().lowercase()] = part.substring(i + 1).trim()
        }
        val raw = (map["status"] ?: statusExtra)?.trim()?.lowercase()
        val outcome = when (raw) {
            "success" -> Outcome.SUCCESS
            "failure", "failed" -> Outcome.FAILURE
            "submitted", "pending" -> Outcome.PENDING
            else -> Outcome.UNKNOWN
        }
        return Result(outcome, map["txnid"]?.takeIf { it.isNotEmpty() })
    }

    fun message(r: Result): String = when (r.outcome) {
        Outcome.SUCCESS -> "Your UPI app reported success. Thank you for the tea!"
        Outcome.FAILURE -> "Your UPI app reported that the payment failed. Please check your bank or UPI app before trying again."
        Outcome.PENDING -> "Your UPI app says the payment is still pending. Check your bank or UPI app to confirm."
        Outcome.UNKNOWN -> "Your UPI app did not report a result. Please check your bank or UPI app to see whether it went through."
    }
}
