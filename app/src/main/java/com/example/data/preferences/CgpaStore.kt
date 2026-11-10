package com.example.data.preferences

import android.content.Context
import com.example.domain.CgpaRounding
import com.example.domain.CgpaUiState
import com.example.domain.GradeRow
import com.example.domain.QuickRow
import com.example.domain.SemesterBlock
import com.example.domain.SubjectRow
import com.example.domain.TargetInputs
import org.json.JSONArray
import org.json.JSONObject

/**
 * Keeps the calculator's inputs on this phone (SharedPreferences, never uploaded). Reading is defensive:
 * a damaged or older value falls back to the defaults instead of crashing the screen.
 */
object CgpaStore {
    private const val PREFS = "clg1_cgpa_calculator"
    private const val KEY = "state_v1"

    fun load(context: Context): CgpaUiState {
        val raw = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY, null) ?: return CgpaUiState()
        return runCatching { decode(JSONObject(raw)) }.getOrDefault(CgpaUiState())
    }

    fun save(context: Context, state: CgpaUiState) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(KEY, encode(state).toString()).apply()
    }

    fun clear(context: Context) { context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().remove(KEY).apply() }

    private fun encode(s: CgpaUiState) = JSONObject().apply {
        put("mode", s.mode); put("scaleName", s.scaleName)
        put("grades", JSONArray().apply { s.grades.forEach { put(JSONObject().put("id", it.id).put("label", it.label).put("point", it.point)) } })
        put("semesters", JSONArray().apply {
            s.semesters.forEach { sem ->
                put(JSONObject().put("id", sem.id).put("name", sem.name).put("subjects", JSONArray().apply {
                    sem.subjects.forEach { r -> put(JSONObject().put("id", r.id).put("name", r.name).put("credits", r.credits).put("grade", r.grade).put("counts", r.counts)) }
                }))
            }
        })
        put("quick", JSONArray().apply { s.quick.forEach { put(JSONObject().put("id", it.id).put("name", it.name).put("sgpa", it.sgpa).put("credits", it.credits)) } })
        put("equalWeights", s.equalWeights); put("roundSgpaFirst", s.roundSgpaFirst)
        put("rounding", s.rounding.name); put("places", s.places); put("factor", s.factor)
        put("target", JSONObject().put("current", s.target.current).put("done", s.target.done).put("remaining", s.target.remaining).put("target", s.target.target))
    }

    private fun decode(o: JSONObject): CgpaUiState {
        val d = CgpaUiState()
        fun arr(a: JSONArray?) = (0 until (a?.length() ?: 0)).map { a!!.getJSONObject(it) }
        val t = o.optJSONObject("target")
        return CgpaUiState(
            mode = o.optInt("mode", 0).coerceIn(0, 2),
            scaleName = o.optString("scaleName", d.scaleName),
            grades = arr(o.optJSONArray("grades")).map { GradeRow(it.optString("id"), it.optString("label"), it.optString("point")) }.ifEmpty { d.grades },
            semesters = arr(o.optJSONArray("semesters")).map { sem ->
                SemesterBlock(sem.optString("id"), sem.optString("name"), arr(sem.optJSONArray("subjects")).map {
                    SubjectRow(it.optString("id"), it.optString("name"), it.optString("credits"), it.optString("grade"), it.optBoolean("counts", true))
                })
            }.ifEmpty { d.semesters },
            quick = arr(o.optJSONArray("quick")).map { QuickRow(it.optString("id"), it.optString("name"), it.optString("sgpa"), it.optString("credits")) }.ifEmpty { d.quick },
            equalWeights = o.optBoolean("equalWeights", false), roundSgpaFirst = o.optBoolean("roundSgpaFirst", false),
            rounding = runCatching { CgpaRounding.valueOf(o.optString("rounding")) }.getOrDefault(CgpaRounding.HALF_UP),
            places = o.optInt("places", 2).coerceIn(0, 4), factor = o.optString("factor", ""),
            target = TargetInputs(t?.optString("current") ?: "", t?.optString("done") ?: "", t?.optString("remaining") ?: "", t?.optString("target") ?: "")
        )
    }
}
