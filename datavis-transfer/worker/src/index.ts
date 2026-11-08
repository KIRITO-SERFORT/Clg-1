export interface Env {
  TRANSFERS: R2Bucket;
  DB: D1Database;
  RATE_LIMIT: KVNamespace;
}

const MAX_BYTES = 2 * 1024 * 1024;
const TTL_SECONDS = 10 * 60;

const EXPECTED_COLUMNS = [
  "schema_version", "record_type", "academic_year", "term_id", "term_name", "subject_code", "subject_name",
  "credits", "component_type", "component_name", "attempt_number", "marks_obtained", "marks_max",
  "weight_percent", "normalized_score", "result_status", "letter_grade", "grade_point", "attendance_percent",
  "attendance_present", "attendance_total", "data_source", "record_date"
];

function validateCsvShape(csv: string): { rows: number; columns: number } {
  const parsed: string[][] = [];
  let row: string[] = [];
  let field = "";
  let quoted = false;
  for (let i = 0; i < csv.length; i++) {
    const ch = csv[i];
    if (quoted) {
      if (ch === '"' && csv[i + 1] === '"') { field += '"'; i++; }
      else if (ch === '"') {
        quoted = false;
        const next = csv[i + 1];
        if (next !== undefined && next !== ',' && next !== '\n' && next !== '\r') throw new Error("unexpected_csv_character_after_quote");
      }
      else field += ch;
    } else if (ch === '"' && field.length === 0) quoted = true;
    else if (ch === '"') throw new Error("unexpected_csv_quote");
    else if (ch === ',') { row.push(field); field = ""; }
    else if (ch === '\n') { row.push(field.replace(/\r$/, "")); parsed.push(row); row = []; field = ""; }
    else field += ch;
    if (parsed.length > 10001) throw new Error("too_many_rows");
  }
  if (quoted) throw new Error("unterminated_csv_quote");
  if (field.length > 0 || row.length > 0) { row.push(field.replace(/\r$/, "")); parsed.push(row); }
  if (parsed.length < 2) throw new Error("no_data_rows");
  const header = parsed[0];
  if (header.length > 64 || header.length !== EXPECTED_COLUMNS.length || header.some((value, i) => value !== EXPECTED_COLUMNS[i])) throw new Error("unsupported_csv_columns");
  const dataRows = parsed.slice(1).filter(values => !(values.length === 1 && values[0] === ""));
  if (dataRows.length > 10000) throw new Error("too_many_rows");
  if (dataRows.some(values => values.length !== header.length)) throw new Error("inconsistent_csv_row");
  if (dataRows.some(values => values[0] !== "1" || values[1] !== "assessment")) throw new Error("unsupported_record_type_or_version");
  const col = Object.fromEntries(header.map((name, index) => [name, index]));
  for (const values of dataRows) {
    const numeric = (name: string): number | null => {
      const value = values[col[name]]?.trim() ?? "";
      if (!value) return null;
      const parsed = Number(value);
      return Number.isFinite(parsed) ? parsed : Number.NaN;
    };
    const credits = numeric("credits");
    const maximum = numeric("marks_max");
    const weight = numeric("weight_percent");
    const obtained = numeric("marks_obtained");
    const normalized = numeric("normalized_score");
    const status = values[col["result_status"]];
    if (!values[col["subject_name"]]?.trim() || credits === null || credits < 0 || maximum === null || maximum <= 0 || weight === null || weight < 0 || weight > 100) throw new Error("invalid_subject_or_component_values");
    if (obtained !== null && (!Number.isFinite(obtained) || obtained < 0 || obtained > maximum)) throw new Error("invalid_marks_obtained");
    if (normalized !== null && (!Number.isFinite(normalized) || normalized < 0 || normalized > 100)) throw new Error("invalid_normalized_score");
    if (!["RECORDED", "ABSENT", "EXEMPTED", "NOT_CONDUCTED"].includes(status)) throw new Error("invalid_result_status");
    if ((status === "RECORDED") !== (obtained !== null)) throw new Error("result_status_marks_mismatch");
    const attempt = numeric("attempt_number");
    if (attempt === null || !Number.isInteger(attempt) || attempt < 1) throw new Error("invalid_attempt_number");
  }
  return { rows: dataRows.length, columns: header.length };
}
const json = (body: unknown, status = 200) => new Response(JSON.stringify(body), {
  status, headers: { "content-type": "application/json; charset=utf-8", "cache-control": "no-store" }
});

async function sha256(value: string): Promise<string> {
  const digest = await crypto.subtle.digest("SHA-256", new TextEncoder().encode(value));
  return [...new Uint8Array(digest)].map(b => b.toString(16).padStart(2, "0")).join("");
}
function randomToken(): string {
  const bytes = crypto.getRandomValues(new Uint8Array(32));
  return btoa(String.fromCharCode(...bytes)).replaceAll("+", "-").replaceAll("/", "_").replaceAll("=", "");
}
async function rateLimit(request: Request, env: Env): Promise<boolean> {
  const ip = request.headers.get("CF-Connecting-IP") ?? "unknown";
  const key = `rl:${ip}:${Math.floor(Date.now() / 60000)}`;
  const count = Number(await env.RATE_LIMIT.get(key) ?? "0");
  if (count >= 8) return false;
  await env.RATE_LIMIT.put(key, String(count + 1), { expirationTtl: 120 });
  return true;
}

export default {
  async fetch(request: Request, env: Env): Promise<Response> {
    const url = new URL(request.url);
    if (request.method === "POST" && url.pathname === "/v1/transfers") {
      if (!await rateLimit(request, env)) return json({ error: "rate_limited" }, 429);
      if (request.headers.get("X-CSV-Schema") !== "1") return json({ error: "unsupported_schema" }, 400);
      const type = request.headers.get("content-type") ?? "";
      if (!type.toLowerCase().startsWith("text/csv")) return json({ error: "csv_required" }, 415);
      const declaredSize = Number(request.headers.get("content-length") ?? "0");
      if (declaredSize > MAX_BYTES) return json({ error: "invalid_size" }, 413);
      const csv = await request.text();
      const size = new TextEncoder().encode(csv).byteLength;
      if (size < 10 || size > MAX_BYTES) return json({ error: "invalid_size" }, 413);
      let shape: { rows: number; columns: number };
      try { shape = validateCsvShape(csv); } catch (error) { return json({ error: String(error) }, 400); }
      const token = randomToken();
      const tokenHash = await sha256(token);
      const objectKey = `temporary/${crypto.randomUUID()}.csv`;
      const now = Math.floor(Date.now() / 1000);
      await env.TRANSFERS.put(objectKey, csv, { httpMetadata: { contentType: "text/csv; charset=utf-8" } });
      try {
        await env.DB.prepare("INSERT INTO transfers(token_hash, object_key, created_at, expires_at, consumed_at, byte_size) VALUES(?, ?, ?, ?, NULL, ?)")
          .bind(tokenHash, objectKey, now, now + TTL_SECONDS, size).run();
      } catch (error) {
        await env.TRANSFERS.delete(objectKey);
        throw error;
      }
      return json({ import_token: token, expires_in_seconds: TTL_SECONDS, schema_version: 1, rows: shape.rows, columns: shape.columns }, 201);
    }

    if (request.method === "POST" && url.pathname === "/v1/transfers/redeem") {
      if (!await rateLimit(request, env)) return json({ error: "rate_limited" }, 429);
      const requestBody = await request.json().catch(() => ({})) as { token?: string };
      const token = requestBody.token ?? "";
      if (!/^[A-Za-z0-9_-]{40,60}$/.test(token)) return json({ error: "invalid_token" }, 400);
      const tokenHash = await sha256(token);
      const now = Math.floor(Date.now() / 1000);
      // Atomic consume prevents replay and concurrent double downloads.
      const consumed = await env.DB.prepare("UPDATE transfers SET consumed_at = ? WHERE token_hash = ? AND consumed_at IS NULL AND expires_at > ? RETURNING object_key, byte_size")
        .bind(now, tokenHash, now).first<{ object_key: string; byte_size: number }>();
      if (!consumed) return json({ error: "expired_or_already_used" }, 410);
      const object = await env.TRANSFERS.get(consumed.object_key);
      if (!object) return json({ error: "transfer_missing" }, 404);
      const csv = await object.text();
      await env.TRANSFERS.delete(consumed.object_key);
      return new Response(csv, { headers: { "content-type": "text/csv; charset=utf-8", "cache-control": "no-store", "x-content-type-options": "nosniff" } });
    }
    return json({ error: "not_found" }, 404);
  },

  async scheduled(_controller: ScheduledController, env: Env, ctx: ExecutionContext): Promise<void> {
    ctx.waitUntil((async () => {
      const now = Math.floor(Date.now() / 1000);
      const expired = await env.DB.prepare("SELECT object_key FROM transfers WHERE expires_at <= ? OR consumed_at IS NOT NULL LIMIT 500")
        .bind(now).all<{ object_key: string }>();
      for (const row of expired.results ?? []) await env.TRANSFERS.delete(row.object_key);
      await env.DB.prepare("DELETE FROM transfers WHERE expires_at <= ? OR consumed_at IS NOT NULL").bind(now).run();
    })());
  }
};
