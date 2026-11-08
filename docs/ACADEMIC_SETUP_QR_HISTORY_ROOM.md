# Academic setup, QR, history and local database

## Academic setup wizard

The Exam Command Center includes a four-step wizard for institution/program metadata, multiple academic periods, calendar type, assessment components/maxima/weights/optional component pass thresholds, aggregation/missing-mark policies, and editable grade bands. It validates the profile before activation, supports a locally saved draft, and includes a fictional 80%-marks sample calculation. It does not assume an institution's grading policy: users must enter values from their official handbook. The current screen uses the first configured period as the active period; all configured periods are stored in the Room profile configuration.

## QR templates

- Generate a QR from the current structure-only `.clg1template`.
- Scan using the embedded camera scanner and review the decoded template before applying it.
- QR payloads are compressed and URL-safe encoded; they have a size cap. Use the `.clg1template` file-sharing route if the QR is too dense.
- Templates do not contain marks, student identity, attendance or ERP credentials.
- Imported community templates are unverified. Import merges new subjects and preserves existing marks; duplicate existing subjects are not overwritten.

## Historical analytics and PDF

Archiving stores one snapshot per academic-year/term pair. The report summarizes period average/SGPA trends, grade distribution, latest subject performance, assessment-category averages, subject deltas against the previous archived period and risk flags. PDF generation is available for the first 10 reports without Plus; further report generation requires a fresh online verification of a Google Play subscription. Archiving remains available and does not erase earlier snapshots.

## Room migration

`Clg1Database` is Room schema version 3. Migrations 1→2 add term snapshots and 2→3 add the audit table without destructive fallback. The existing SharedPreferences workspace is migrated into normalized Room tables and retained as a recovery copy. Changes are dual-written to SharedPreferences and Room with a debounced Room mirror; a SHA-256 mirror marker causes the app to retry a missed mirror after restart. Academic periods, subjects, assessment marks, exam events, snapshots and audit rows are local-only. Academic backup/restore includes current workspace, profile metadata and historical snapshots.

The first shipped Room schema is version 1; a future schema change must increment the database version and add a non-destructive migration. Never add `fallbackToDestructiveMigration()`.
