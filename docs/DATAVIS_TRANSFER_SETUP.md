# One-time Clg 1% → DataVis transfer

The Android app can upload a user-confirmed CSV to a Cloudflare Worker and open DataVis with a random one-time token. The worker stores the CSV in a private R2 bucket, stores only the SHA-256 token hash in D1, expires transfers after 10 minutes, limits requests, consumes each token atomically once, deletes retrieved objects and runs scheduled cleanup. Maximum CSV size is 2 MB. The existing manual CSV share remains available as a fallback.

## Deploy the Worker

1. Install Node 22 and Wrangler in `datavis-transfer/worker`.
2. Create a private R2 bucket named `clg1-datavis-transfers`.
3. Create a D1 database and apply `schema.sql`.
4. Create a KV namespace for rate limits.
5. Replace the D1 and KV placeholder IDs in `wrangler.toml`.
6. Run `npm install`, `npx wrangler d1 execute clg1-datavis-transfers --file=schema.sql`, then `npm run deploy`.
7. Set GitHub repository variable `CLG1_DATAVIS_TRANSFER_API` to the deployed HTTPS Worker base URL.

## Integrate DataVis

The DataVis source repository was not included with the Clg 1% Android project, so its existing uploader has not been overwritten. The drop-in Streamlit helper is `datavis-transfer/datavis-integration/streamlit_import.py`. Copy it into the DataVis project, set `CLG1_TRANSFER_API_BASE` in the DataVis hosting environment to the Worker URL, and call `maybe_import_clg1_csv()` before rendering the existing manual uploader. If it returns a DataFrame, pass it through DataVis's existing validation/import pipeline; do not bypass existing schema validation. The helper clears the query parameter before retrieval and never logs the token.

The Worker does not require browser CORS because the DataVis server redeems the token in a POST body and retrieves the CSV server-to-server. Keep the R2 bucket private. Monitor abuse/rate limits and verify the deployment before enabling the Android transfer button.
