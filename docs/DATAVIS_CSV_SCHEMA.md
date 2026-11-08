# Clg 1% → DataVis CSV schema v1

The export is one row per subject assessment component. It uses UTF-8, a header row, standard CSV quoting and `schema_version=1`. The transfer Worker accepts exactly these 23 columns in this order:

1. `schema_version` — always `1`.
2. `record_type` — `assessment` in this release.
3. `academic_year` — selected academic year, if configured.
4. `term_id` — active period identifier (or deterministic fallback).
5. `term_name` — active period name.
6. `subject_code` — student-entered course code.
7. `subject_name` — student-entered subject name.
8. `credits` — subject credits.
9. `component_type` — configured component category (e.g. internal/practical/external).
10. `component_name` — assessment component name.
11. `attempt_number` — `1` in this release.
12. `marks_obtained` — blank when no mark has been entered; absent/exempt/pending state is represented in `result_status`.
13. `marks_max` — configured maximum marks.
14. `weight_percent` — configured component weight.
15. `normalized_score` — blank if no mark exists; otherwise obtained / maximum × 100.
16. `result_status` — `RECORDED`, `ABSENT`, `EXEMPTED` or `NOT_CONDUCTED`.
17. `letter_grade` — subject grade where calculated; blank otherwise.
18. `grade_point` — subject grade point where calculated; blank otherwise.
19. `attendance_percent` — blank in this release; no attendance is exported by default.
20. `attendance_present` — blank in this release.
21. `attendance_total` — blank in this release.
22. `data_source` — `manual` in this release.
23. `record_date` — ISO-8601 timestamp if the component has a record date; blank otherwise.

Null/blank fields are intentional and must not be silently converted to zero. `normalized_score`, `letter_grade` and `grade_point` are not official results; they are calculated only from the user's configured rules and may be blank for incomplete records. The CSV never includes student name, email, phone, roll/register number, ERP credentials, session cookies, access tokens or device identifiers.

The Worker currently enforces the exact header, schema version, `record_type=assessment`, at most 10,000 data rows, at most 64 columns, a 2 MB file cap and HTTPS. DataVis must continue to validate the CSV through the same pipeline as its manual uploader.
