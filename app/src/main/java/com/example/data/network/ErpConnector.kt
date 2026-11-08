package com.example.data.network

import com.example.data.model.Timetable

/**
 * Integration boundary for college portals. Each supported college gets ONE implementation of this
 * interface; the rest of the app (repository, worker, UI) only talks to this interface.
 *
 * Today there is exactly one connector: ACGCET (see [ErpClient]). Do not claim support for another
 * portal until its connector is implemented AND tested against that portal.
 */
interface ErpConnector {
    /** Stable machine id, e.g. "acgcet". Never change once released. */
    val id: String

    /** Name shown to the student, e.g. "ACGCET". */
    val shortName: String

    @Throws(java.io.IOException::class)
    fun fetchFullAttendance(username: String, password: String): FullAttendanceResult

    @Throws(java.io.IOException::class)
    fun fetchTimetable(username: String, password: String): Timetable
}

object ErpConnectors {
    const val DEFAULT_ID = "acgcet"
    const val DEFAULT_SHORT_NAME = "ACGCET"

    /** A fresh connector per call (connectors keep per-login cookie state). */
    fun create(): ErpConnector = ErpClient()
}
