/**
 * Optional WebSocket relay for Yara real-time channel.
 * Run: node server/relay.js  (needs `npm i ws`)
 */
const { WebSocketServer } = require('ws');
const wss = new WebSocketServer({ port: 8080 });
console.log('Yara relay listening on ws://0.0.0.0:8080');
wss.on('connection', (ws) => {
  ws.on('message', (msg) => {
    wss.clients.forEach(c => { if (c !== ws && c.readyState === 1) c.send(msg.toString()); });
  });
});
