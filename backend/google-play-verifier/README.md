# Google Play purchase verifier

Deploy this service behind HTTPS (Cloud Run is suitable). It verifies subscription purchase tokens directly with the Google Play Developer API. The Android app must not grant Plus based on a local boolean or a successful Play UI callback alone.

## Required configuration

- `PLAY_PACKAGE_NAME`: existing Android application ID `com.aistudio.acgcetwidtz.vkzpst`.
- `PLAY_PLUS_PRODUCT_ID`: exact subscription product ID created in Play Console.
- `PLAY_SERVICE_ACCOUNT_JSON`: service-account JSON with Play Developer API access. Store as a deployment secret; never commit it.
- `PORT`: optional, defaults to 8080.

Grant the service account the minimum Play Console permissions needed to view subscriptions/orders. Enable the Google Play Android Developer API in the service project. Configure the same `PLAY_PLUS_PRODUCT_ID` as `CLG1_PLUS_PRODUCT_ID` in the Android build and set `CLG1_BILLING_VERIFY_URL` to `https://<service>/verify`.

The endpoint validates package, product and purchase token against `purchases.subscriptionsv2.get`. It returns `active=true` only for an active or grace-period subscription whose matching line item has not expired. Do not use this endpoint as a substitute for Real-time Developer Notifications in a production subscription backend; add RTDN reconciliation, monitoring, authenticated/App Check client identity and abuse controls before public release. Test pending, cancellation, expiry, refund, account switching and restore-purchase flows in Play internal testing.
