import express from 'express';
import rateLimit from 'express-rate-limit';
import { google } from 'googleapis';

const app = express();
app.disable('x-powered-by');
app.use((_req, res, next) => { res.set('Cache-Control', 'no-store'); next(); });
app.use(express.json({ limit: '12kb', strict: true }));
app.use(rateLimit({ windowMs: 60_000, limit: 30, standardHeaders: true, legacyHeaders: false }));

const packageName = process.env.PLAY_PACKAGE_NAME;
const productId = process.env.PLAY_PLUS_PRODUCT_ID;
const credentials = process.env.PLAY_SERVICE_ACCOUNT_JSON;
if (!packageName || !productId || !credentials) {
  throw new Error('Set PLAY_PACKAGE_NAME, PLAY_PLUS_PRODUCT_ID and PLAY_SERVICE_ACCOUNT_JSON');
}
const auth = new google.auth.GoogleAuth({
  credentials: JSON.parse(credentials),
  scopes: ['https://www.googleapis.com/auth/androidpublisher']
});
const publisher = google.androidpublisher({ version: 'v3', auth });

app.post('/verify', async (req, res) => {
  try {
    const { packageName: requestedPackage, productId: requestedProduct, purchaseToken } = req.body ?? {};
    if (requestedPackage !== packageName || requestedProduct !== productId || typeof purchaseToken !== 'string' || purchaseToken.length < 20 || purchaseToken.length > 4096) {
      return res.status(400).json({ active: false, error: 'invalid_request' });
    }
    // Subscription product. The app does not receive an entitlement until Play's API confirms it.
    const result = await publisher.purchases.subscriptionsv2.get({ packageName, token: purchaseToken });
    const data = result.data;
    const activeStates = new Set(['SUBSCRIPTION_STATE_ACTIVE', 'SUBSCRIPTION_STATE_IN_GRACE_PERIOD', 'SUBSCRIPTION_STATE_CANCELED']);
    const matchingLine = (data.lineItems ?? []).find(item => item.productId === productId);
    const expiry = matchingLine?.expiryTime ? Date.parse(matchingLine.expiryTime) : 0;
    const active = activeStates.has(data.subscriptionState ?? '') && expiry > Date.now();
    if (active && data.acknowledgementState === 'ACKNOWLEDGEMENT_STATE_PENDING') {
      await publisher.purchases.subscriptions.acknowledge({ packageName, subscriptionId: productId, token: purchaseToken });
    }
    return res.status(200).json({ active, productId, expiresAt: active ? matchingLine.expiryTime : null });
  } catch (error) {
    // Never log the purchase token or the full Google API error payload.
    const code = Number(error?.code ?? 0);
    return res.status(code === 404 ? 200 : 502).json({ active: false, error: code === 404 ? 'purchase_not_found' : 'verification_unavailable' });
  }
});

app.get('/health', (_req, res) => res.status(200).json({ ok: true }));
const port = Number(process.env.PORT ?? 8080);
app.listen(port, '0.0.0.0', () => process.stdout.write(`Clg 1% billing verifier listening on ${port}\n`));
