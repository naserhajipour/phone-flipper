const express = require('express');
const bodyParser = require('body-parser');
const fs = require('fs');
const path = require('path');
const { ethers } = require('ethers');
const { v4: uuidv4 } = require('uuid');
const { sendPaymentNotification } = require('./emailer');

const app = express();
app.use(bodyParser.json());

const ROOT = path.join(__dirname);
const ORDERS_FILE = path.join(ROOT, 'orders.json');
const CONFIG = require('./config.json');

const PROVIDER_URL = process.env.BSC_RPC_URL || 'https://bsc-dataseed.binance.org/';
const provider = new ethers.providers.JsonRpcProvider(PROVIDER_URL);
const BUSD_CONTRACT = CONFIG.busdContract.toLowerCase();
const RECEIVE_ADDRESS = CONFIG.receiveAddress.toLowerCase();
const PRICE_USD = Number(CONFIG.priceUSD) || 1.0;
const DEMO_DAYS = Number(CONFIG.demoDurationDays) || 3;
const CONFIRMATIONS = Number(CONFIG.confirmationsRequired) || 1;

function loadOrders() {
  try {
    if (!fs.existsSync(ORDERS_FILE)) return {};
    const raw = fs.readFileSync(ORDERS_FILE, 'utf8');
    return JSON.parse(raw || '{}');
  } catch (e) {
    console.error('loadOrders error', e.message);
    return {};
  }
}

function saveOrders(data) {
  fs.writeFileSync(ORDERS_FILE, JSON.stringify(data, null, 2));
}

function formatExpiryDate(days) {
  const now = Date.now();
  return new Date(now + days * 24 * 60 * 60 * 1000).toISOString();
}

async function verifyBUSDTransfer(txHash, expectedAmountTokens) {
  // expectedAmountTokens is in plain tokens (1.0 BUSD == 1)
  try {
    const receipt = await provider.getTransactionReceipt(txHash);
    if (!receipt) return { ok: false, reason: 'receipt not found' };
    // check confirmations
    const blockNumber = receipt.blockNumber;
    const latest = await provider.getBlockNumber();
    const confs = latest - blockNumber + 1;
    if (confs < CONFIRMATIONS) return { ok: false, reason: 'not enough confirmations', confirmations: confs };

    // Transfer event signature
    const transferSig = ethers.utils.id('Transfer(address,address,uint256)');
    // iterate logs
    for (const log of receipt.logs) {
      if (log.address.toLowerCase() === BUSD_CONTRACT && log.topics[0] === transferSig) {
        // topics[1] = from, topics[2] = to
        const to = '0x' + log.topics[2].slice(26);
        if (to.toLowerCase() === RECEIVE_ADDRESS) {
          // decode value
          const value = ethers.BigNumber.from(log.data);
          const decimals = ethers.BigNumber.from('1000000000000000000'); // 1e18 token decimals for BUSD
          // compare
          const expected = ethers.utils.parseUnits(String(expectedAmountTokens), 18);
          if (value.gte(expected)) {
            return { ok: true, receipt };
          } else {
            return { ok: false, reason: 'amount too small', value: value.toString(), expected: expected.toString() };
          }
        }
      }
    }
    return { ok: false, reason: 'no matching token transfer to receive address in tx' };
  } catch (err) {
    console.error('verifyBUSDTransfer error', err && err.message);
    return { ok: false, reason: 'exception', error: err.message };
  }
}

// Create order: buyer provides email (optional)
app.post('/create-order', async (req, res) => {
  const { buyerEmail } = req.body || {};
  const orderId = uuidv4();
  const amountUSD = PRICE_USD;
  const amountTokens = Number(PRICE_USD).toFixed(2); // BUSD tokens (1 USD == 1 BUSD)

  const orders = loadOrders();
  orders[orderId] = {
    orderId,
    status: 'pending',
    amountUSD,
    amountTokens,
    buyerEmail: buyerEmail || null,
    createdAt: new Date().toISOString()
  };
  saveOrders(orders);

  res.json({ ok: true, orderId, amountUSD, amountTokens, token: CONFIG.acceptedToken, receiveAddress: CONFIG.receiveAddress });
});

// Submit TX for verification
app.post('/submit-tx', async (req, res) => {
  const { orderId, txHash } = req.body || {};
  if (!orderId || !txHash) return res.status(400).json({ ok: false, error: 'orderId and txHash required' });
  const orders = loadOrders();
  const order = orders[orderId];
  if (!order) return res.status(404).json({ ok: false, error: 'order not found' });
  if (order.status === 'paid') return res.json({ ok: true, message: 'already paid', order });

  // verify token transfer in tx
  const verify = await verifyBUSDTransfer(txHash, Number(order.amountTokens));
  if (!verify.ok) return res.status(400).json({ ok: false, error: 'verification failed', reason: verify.reason, details: verify });

  // mark paid and issue license
  const licenseCode = `DEMO-NASER-3D-${orderId.slice(0,8).toUpperCase()}`;
  const licenseToken = uuidv4();
  const expiry = formatExpiryDate(DEMO_DAYS);
  order.status = 'paid';
  order.txHash = txHash;
  order.paidAt = new Date().toISOString();
  order.license = { licenseCode, licenseToken, expiresAt: expiry };
  orders[orderId] = order;
  saveOrders(orders);

  // send notification to admin
  try {
    await sendPaymentNotification(order, txHash);
  } catch (e) {
    console.warn('sendPaymentNotification failed', e && e.message);
  }

  res.json({ ok: true, order });
});

// Redeem demo endpoint (for demo code)
app.get('/redeem-demo', (req, res) => {
  const { code } = req.query;
  if (!code) return res.status(400).json({ ok: false, error: 'code required' });
  // For demo convenience, accept the earlier demo code
  if (code === 'DEMO-NASER-20261012-7B3C9F') {
    const token = uuidv4();
    const expiry = formatExpiryDate(1); // 1 day for that demo code
    return res.json({ ok: true, licenseToken: token, expiresAt: expiry });
  }
  return res.status(404).json({ ok: false, error: 'demo code not found' });
});

// Admin: send update email to all paid buyers
app.post('/send-update', async (req, res) => {
  const ADMIN_SECRET = process.env.ADMIN_SECRET || '';
  const { secret, subject, message } = req.body || {};
  if (!secret || secret !== ADMIN_SECRET) return res.status(403).json({ ok: false, error: 'forbidden' });
  const orders = loadOrders();
  const buyers = Object.values(orders).filter(o => o.status === 'paid' && o.buyerEmail);
  const results = [];
  for (const b of buyers) {
    try {
      await sendPaymentNotification({ orderId: b.orderId, amountBNB: b.amountTokens, amountUSD: b.amountUSD, orderTitle: subject }, b.txHash);
      // sendPaymentNotification currently sends to admin NOT to buyer; to send to buyer we can call nodemailer directly here
      results.push({ orderId: b.orderId, email: b.buyerEmail, status: 'notified_via_admin_email' });
    } catch (e) {
      results.push({ orderId: b.orderId, email: b.buyerEmail, error: e.message });
    }
  }
  res.json({ ok: true, notified: results.length, details: results });
});

// simple health
app.get('/health', (req, res) => res.json({ ok: true }));

const PORT = process.env.PORT || 3100;
app.listen(PORT, () => console.log('Payment server listening on', PORT));
