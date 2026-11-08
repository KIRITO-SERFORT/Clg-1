# Clg 1% academic data schema

## Local storage

The current implementation stores the academic workspace in app-private SharedPreferences under file `clg1_academic_local`, key `academic_records_v1`. The JSON root carries `schema_version: 1`, institution name, term, passing threshold, grade bands, combination/missing-mark rules, subjects, assessment components and exams. It is local-only; no account or cloud service is involved. The academic backup export wraps this JSON and profile metadata in an AES-256-GCM envelope using a passphrase-derived PBKDF2-HMAC-SHA256 key. A future Room migration must preserve this JSON before switching storage engines.

## Calculation rules

For each recorded assessment:

`normalized percentage = obtained marks / maximum marks × 100`

For weighted combination:

`weighted points = normalized percentage × weight percentage / 100`

`subject percentage = sum(weighted points) / sum(included weights) × 100`

Missing/not-conducted assessments are excluded and the remaining included weights are renormalized by default. The result remains provisional while required marks are missing. The engine also models arithmetic average, best-N, highest and sum modes; the current screen's main workflow is weighted average. Absent/exempted policies exist in the domain model but do not yet have a complete screen editor.

Grade bands are supplied by the example defaults or a validated imported template. The defaults are not asserted to be an institution's official scheme.

`SGPA = sum(subject credits × mapped grade point) / sum(subject credits)`

SGPA is withheld if any listed subject lacks a valid result/credit. `CGPA` is available as a pure engine function when term credit totals and SGPA values are supplied; the app does not infer an institution's cumulative conversion rule.

## Structure-only template

File extension: `.clg1template` (JSON internally).

Top-level keys include `format`, `schema_version`, college/program/department/regulation/academic year/term metadata, passing threshold, decimal precision, combination and missing-mark policies, best-N, grade bands, optional subject structure, verification status, creation time, notes and SHA-256 checksum.

Template subject structure includes code, name, credits and assessment definitions (name, maximum marks, weight, category and optional component pass threshold). It does not include obtained marks, attendance, timetable, student identity, ERP credentials, cookies or tokens. Import validates schema, size, keys, ranges, weights and checksum; the preview labels the template as community/unverified. Merge adds new subjects and preserves existing matching subjects/marks.

## CSV export for DataVis (schema version 1)

One row represents one assessment component. Header:

`schema_version,record_type,academic_year,term_id,term_name,subject_code,subject_name,credits,component_type,component_name,attempt_number,marks_obtained,marks_max,weight_percent,normalized_score,data_source`

`record_type` is `assessment`; `data_source` is `manual`; blank `marks_obtained` and `normalized_score` mean no recorded score. Export is local and user-initiated. The app previews included fields and asks for confirmation before sharing a `.csv` attachment through Android's share sheet. DataVis must still be opened and the file uploaded manually; no automatic browser attachment or seamless transfer is claimed.
