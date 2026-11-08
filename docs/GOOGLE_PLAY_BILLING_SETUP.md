# Google Play Billing setup

The Android client uses Google Play Billing Library 9.1.0, queries the subscription product, handles pending purchases, restores owned subscriptions and does not grant Plus until the HTTPS verifier confirms the purchase token against the Google Play Developer API. The purchase token is stored encrypted with Android Keystore and is rechecked with the server when a premium PDF is requested. A local preference flag is not used as proof of payment.

## Deploy verifier

1. Create a subscription in Play Console with a base plan and offer. Choose the product ID; do not assume the example ID is correct.
2. Create a Google Cloud service account, enable Android Publisher API and grant the minimum Play Console permissions needed to view subscriptions and acknowledge purchases.
3. Deploy `backend/google-play-verifier` to Cloud Run or another HTTPS Node 22 host.
4. Configure server environment variables:
   - `PLAY_PACKAGE_NAME=com.aistudio.acgcetwidtz.vkzpst`
   - `PLAY_PLUS_PRODUCT_ID=<exact Play Console subscription product ID>`
   - `PLAY_SERVICE_ACCOUNT_JSON=<service account JSON stored as a deployment secret>`
   - `PORT=8080` (optional)
5. Set GitHub repository variable `CLG1_PLUS_PRODUCT_ID` to the same product ID and secret `CLG1_BILLING_VERIFY_URL` to the deployed `/verify` endpoint.
6. Run Play internal testing with license testers. Verify purchase, restore, cancellation-before-expiry, expiry, refund, pending payment and account-switch flows before public release.

The verifier queries `purchases.subscriptionsv2.get`, checks the matching line item and expiry, and acknowledges an active pending acknowledgement through the Android Publisher API. Do not commit the service-account key or upload-key keystore. The service has IP rate limiting but still needs production monitoring, secret rotation, deployment hardening and Play Console review before public rollout.
