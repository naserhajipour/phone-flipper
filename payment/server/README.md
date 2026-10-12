# Payment server - updates

I've added a payment server implementation (payment/server/app.js) to handle:
- create-order (POST /create-order) -> creates an order and returns orderId, amount (USD/BUSD) and receive address
- submit-tx (POST /submit-tx) -> verify a BUSD transfer txHash and issue a 3-day license token
- redeem-demo (GET /redeem-demo?code=...) -> demo code support
- send-update (POST /send-update) -> admin endpoint to notify buyers (requires ADMIN_SECRET env var)

Default configuration is in payment/server/config.json:
- priceUSD: 1.00
- demoDurationDays: 3
- acceptedToken: BUSD
- busdContract: 0xe9e7cea3dedca5984780bafc599bd69add087d56
- confirmationsRequired: 1
- receiveAddress: <your receive address>

Environment variables you should set as GitHub Secrets or in your host environment:
- BSC_RPC_URL (optional, defaults to https://bsc-dataseed.binance.org/)
- ADMIN_SECRET (string used for /send-update access)

Email notifications use the existing emailer.js and SMTP env vars described earlier.

How to test locally:
1) cd payment/server
2) npm install
3) node app.js
4) POST /create-order with {"buyerEmail":"you@example.com"}
5) Send 1 BUSD to the configured receive address and call POST /submit-tx {"orderId":"...","txHash":"0x..."}

