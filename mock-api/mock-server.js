/**
 * Third-Party Verification & Compliance Review Portal
 *
 * Replaces the automated mock API with a manual review interface.
 * When "Start Verification" or "Start Review" is triggered from the main app,
 * Kafka delivers requests here. A reviewer opens http://localhost:3001,
 * views documents, and clicks Pass or Fail for each.
 */

const { Kafka } = require('kafkajs');
const express = require('express');
const path = require('path');

const PORT = process.env.PORT || 3001;
const KAFKA_BROKER = process.env.KAFKA_BROKER || 'kafka:29092';

const VERIFICATION_REQUEST_TOPIC = process.env.REQUEST_TOPIC || 'verification.requests.topic';
const VERIFICATION_RESPONSE_TOPIC = process.env.RESPONSE_TOPIC || 'verification.responses.topic';
const COMPLIANCE_REQUEST_TOPIC = process.env.COMPLIANCE_REQUEST_TOPIC || 'compliance.requests.topic';
const COMPLIANCE_RESPONSE_TOPIC = process.env.COMPLIANCE_RESPONSE_TOPIC || 'compliance.responses.topic';

const kafka = new Kafka({
  clientId: 'mock-external-api',
  brokers: [KAFKA_BROKER],
  retry: { initialRetryTime: 3000, retries: 10 },
});

const verificationConsumer = kafka.consumer({ groupId: 'mock-external-verification-group' });
const complianceConsumer = kafka.consumer({ groupId: 'mock-compliance-review-group' });
const producer = kafka.producer();

// ---------------------------------------------------------------------------
// In-memory store for pending requests
// ---------------------------------------------------------------------------
const pendingRequests = new Map();

const VERIFICATION_TYPE_LABELS = {
  BUSINESS_REGISTRATION: 'Business Registration Certificate',
  DIRECTOR_ID: 'Director Government ID',
  BENEFICIAL_OWNERSHIP: 'Beneficial Ownership Declaration',
};

const COMPLIANCE_TYPE_LABELS = {
  BUSINESS_LICENSE: 'Business License',
  PCI_DSS_SAQ: 'PCI DSS SAQ',
  TERMS_OF_SERVICE: 'Terms of Service',
};

// ---------------------------------------------------------------------------
// Kafka message handlers — store as pending, do NOT auto-respond
// ---------------------------------------------------------------------------

async function handleVerificationRequest(message) {
  const request = JSON.parse(message.value.toString());
  console.log(`[VERIFICATION] Received — case=${request.caseId} type=${request.verificationType} file="${request.documentFileName || 'none'}"`);

  pendingRequests.set(request.externalReference, {
    id: request.externalReference,
    category: 'verification',
    caseId: request.caseId,
    type: request.verificationType,
    typeLabel: VERIFICATION_TYPE_LABELS[request.verificationType] || request.verificationType,
    businessName: request.businessName,
    registrationNumber: request.registrationNumber,
    directorName: request.directorName,
    documentFileName: request.documentFileName,
    documentFilePath: request.documentFilePath,
    requestedAt: request.requestedAt,
    externalReference: request.externalReference,
  });
}

async function handleComplianceRequest(message) {
  const request = JSON.parse(message.value.toString());
  console.log(`[COMPLIANCE] Received — case=${request.caseId} type=${request.documentType} file="${request.documentFileName || 'none'}"`);

  pendingRequests.set(request.externalReference, {
    id: request.externalReference,
    category: 'compliance',
    caseId: request.caseId,
    type: request.documentType,
    typeLabel: COMPLIANCE_TYPE_LABELS[request.documentType] || request.documentType,
    businessName: request.businessName,
    registrationNumber: request.registrationNumber,
    directorName: request.directorName,
    documentFileName: request.documentFileName,
    documentFilePath: request.documentFilePath,
    requestedAt: request.requestedAt,
    externalReference: request.externalReference,
  });
}

// ---------------------------------------------------------------------------
// Decision handler — produce Kafka response based on reviewer action
// ---------------------------------------------------------------------------

async function handleDecision(id, decision) {
  const item = pendingRequests.get(id);
  if (!item) throw new Error(`No pending request with id: ${id}`);

  if (item.category === 'verification') {
    const passed = decision === 'PASSED';
    const responseEvent = {
      caseId: item.caseId,
      verificationType: item.type,
      externalReference: item.externalReference,
      status: decision,
      confidenceScore: passed ? 95 : 30,
      responseData: JSON.stringify(
        passed
          ? { verified: true, businessName: item.businessName, verifiedBy: 'Third-Party Reviewer', result: 'Document reviewed and approved' }
          : { verified: false, businessName: item.businessName, verifiedBy: 'Third-Party Reviewer', result: 'Document reviewed and rejected' }
      ),
      riskIndicators: JSON.stringify(passed ? [] : ['Document failed manual review']),
      notes: passed
        ? `${item.typeLabel} reviewed and approved by third-party reviewer`
        : `${item.typeLabel} reviewed and rejected by third-party reviewer`,
      completedAt: new Date().toISOString(),
    };

    await producer.send({
      topic: VERIFICATION_RESPONSE_TOPIC,
      messages: [{ key: item.caseId, value: JSON.stringify(responseEvent) }],
    });

    console.log(`[VERIFICATION] Decision: ${decision} for case=${item.caseId} type=${item.type}`);
  } else {
    const responseEvent = {
      caseId: item.caseId,
      documentType: item.type,
      externalReference: item.externalReference,
      status: decision,
      reason: decision === 'PASSED'
        ? `${item.typeLabel} reviewed and approved by third-party reviewer`
        : `${item.typeLabel} reviewed and rejected by third-party reviewer`,
      completedAt: new Date().toISOString(),
    };

    await producer.send({
      topic: COMPLIANCE_RESPONSE_TOPIC,
      messages: [{ key: item.caseId, value: JSON.stringify(responseEvent) }],
    });

    console.log(`[COMPLIANCE] Decision: ${decision} for case=${item.caseId} type=${item.type}`);
  }

  pendingRequests.delete(id);
}

// ---------------------------------------------------------------------------
// Express server
// ---------------------------------------------------------------------------

const app = express();
app.use(express.json());

// Serve uploaded documents
app.use('/uploads', express.static(path.join(__dirname, 'uploads')));

// API: Get all pending requests
app.get('/api/pending', (req, res) => {
  const items = Array.from(pendingRequests.values());
  res.json(items);
});

// API: Submit decision for a pending request
app.post('/api/decide', async (req, res) => {
  const { id, decision } = req.body;
  if (!id || !decision || !['PASSED', 'FAILED'].includes(decision)) {
    return res.status(400).json({ error: 'Invalid request. Provide id and decision (PASSED or FAILED).' });
  }
  try {
    await handleDecision(id, decision);
    res.json({ success: true, id, decision });
  } catch (err) {
    res.status(404).json({ error: err.message });
  }
});

// Serve the review portal HTML
app.get('/', (req, res) => {
  res.send(getPortalHtml());
});

// ---------------------------------------------------------------------------
// Portal HTML
// ---------------------------------------------------------------------------

function getPortalHtml() {
  return `<!DOCTYPE html>
<html lang="en">
<head>
<meta charset="UTF-8">
<meta name="viewport" content="width=device-width, initial-scale=1.0">
<title>Third Party Verification Portal</title>
<style>
  * { box-sizing: border-box; margin: 0; padding: 0; }
  body { font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif; background: #f0f2f5; color: #333; }
  .header { background: linear-gradient(135deg, #1a237e, #283593); color: white; padding: 20px 32px; }
  .header h1 { font-size: 22px; font-weight: 600; }
  .header p { font-size: 13px; opacity: 0.85; margin-top: 4px; }
  .container { max-width: 1100px; margin: 24px auto; padding: 0 16px; }
  .empty { text-align: center; padding: 60px 20px; color: #888; }
  .empty h2 { font-size: 18px; margin-bottom: 8px; color: #666; }
  .empty p { font-size: 14px; }
  .case-group { margin-bottom: 28px; }
  .case-header { font-size: 15px; font-weight: 600; color: #1a237e; margin-bottom: 12px; padding-bottom: 8px; border-bottom: 2px solid #c5cae9; }
  .case-header span { font-weight: 400; color: #666; margin-left: 12px; font-size: 13px; }
  .section-title { font-size: 13px; font-weight: 600; color: #555; text-transform: uppercase; letter-spacing: 0.5px; margin: 12px 0 8px 0; }
  .card { background: white; border-radius: 8px; padding: 16px 20px; margin-bottom: 10px; box-shadow: 0 1px 3px rgba(0,0,0,0.08); display: flex; align-items: center; gap: 16px; }
  .card-info { flex: 1; }
  .card-info .doc-type { font-weight: 600; font-size: 14px; color: #333; }
  .card-info .doc-file { font-size: 12px; color: #888; margin-top: 2px; }
  .card-info .doc-meta { font-size: 12px; color: #aaa; margin-top: 2px; }
  .card-actions { display: flex; gap: 8px; align-items: center; }
  .btn { padding: 8px 18px; border: none; border-radius: 6px; font-size: 13px; font-weight: 600; cursor: pointer; transition: all 0.15s; }
  .btn:hover { transform: translateY(-1px); box-shadow: 0 2px 6px rgba(0,0,0,0.15); }
  .btn:active { transform: translateY(0); }
  .btn-view { background: #e3f2fd; color: #1565c0; text-decoration: none; display: inline-block; }
  .btn-view:hover { background: #bbdefb; }
  .btn-pass { background: #43a047; color: white; }
  .btn-pass:hover { background: #388e3c; }
  .btn-fail { background: #e53935; color: white; }
  .btn-fail:hover { background: #c62828; }
  .btn:disabled { opacity: 0.5; cursor: not-allowed; transform: none; box-shadow: none; }
  .status-badge { padding: 4px 12px; border-radius: 12px; font-size: 12px; font-weight: 600; }
  .status-passed { background: #e8f5e9; color: #2e7d32; }
  .status-failed { background: #ffebee; color: #c62828; }
  .pulse { animation: pulse 2s infinite; }
  @keyframes pulse { 0%, 100% { opacity: 1; } 50% { opacity: 0.5; } }
  .counter { display: inline-block; background: #ff9800; color: white; border-radius: 12px; padding: 2px 10px; font-size: 12px; font-weight: 600; margin-left: 8px; }
</style>
</head>
<body>
<div class="header">
  <h1>Third Party Verification Portal</h1>
  <p>Review documents submitted for Background Verification and Compliance Review</p>
</div>
<div class="container" id="app">
  <div class="empty pulse">
    <h2>Loading...</h2>
    <p>Connecting to verification service</p>
  </div>
</div>

<script>
const decisions = {};

async function fetchPending() {
  try {
    const res = await fetch('/api/pending');
    const items = await res.json();
    render(items);
  } catch (err) {
    console.error('Failed to fetch pending:', err);
  }
}

function render(items) {
  const container = document.getElementById('app');

  if (items.length === 0 && Object.keys(decisions).length === 0) {
    container.innerHTML = '<div class="empty"><h2>No pending requests</h2><p>Waiting for verification or compliance review requests from the main application.<br>Click "Start Verification" or "Start Review" on a case to send documents here.</p></div>';
    return;
  }

  const groups = {};
  items.forEach(item => {
    if (!groups[item.caseId]) groups[item.caseId] = { businessName: item.businessName, verification: [], compliance: [] };
    groups[item.caseId][item.category].push(item);
  });

  let html = '';
  for (const [caseId, group] of Object.entries(groups)) {
    const total = group.verification.length + group.compliance.length;
    html += '<div class="case-group">';
    html += '<div class="case-header">' + esc(caseId) + '<span>' + esc(group.businessName || '') + '</span><span class="counter">' + total + ' pending</span></div>';

    if (group.verification.length > 0) {
      html += '<div class="section-title">Background Verification</div>';
      group.verification.forEach(item => { html += renderCard(item); });
    }
    if (group.compliance.length > 0) {
      html += '<div class="section-title">Compliance Review</div>';
      group.compliance.forEach(item => { html += renderCard(item); });
    }
    html += '</div>';
  }

  if (html === '') {
    container.innerHTML = '<div class="empty"><h2>No pending requests</h2><p>All documents have been reviewed. Waiting for new requests.</p></div>';
    return;
  }

  container.innerHTML = html;
}

function renderCard(item) {
  const decided = decisions[item.id];
  const filePath = item.documentFilePath ? '/' + item.documentFilePath : null;

  let actions = '';
  if (decided) {
    const cls = decided === 'PASSED' ? 'status-passed' : 'status-failed';
    actions = '<span class="status-badge ' + cls + '">' + decided + '</span>';
  } else {
    if (filePath) {
      actions += '<a href="' + esc(filePath) + '" target="_blank" class="btn btn-view">View Document</a>';
    }
    actions += '<button class="btn btn-pass" onclick="decide(\\'' + item.id + '\\', \\'PASSED\\')">Pass</button>';
    actions += '<button class="btn btn-fail" onclick="decide(\\'' + item.id + '\\', \\'FAILED\\')">Fail</button>';
  }

  return '<div class="card">' +
    '<div class="card-info">' +
      '<div class="doc-type">' + esc(item.typeLabel) + '</div>' +
      '<div class="doc-file">File: ' + esc(item.documentFileName || 'No document') + '</div>' +
      '<div class="doc-meta">Ref: ' + esc(item.externalReference || '') + '</div>' +
    '</div>' +
    '<div class="card-actions">' + actions + '</div>' +
  '</div>';
}

async function decide(id, decision) {
  const buttons = document.querySelectorAll('.btn-pass, .btn-fail');
  buttons.forEach(b => b.disabled = true);

  try {
    const res = await fetch('/api/decide', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ id, decision }),
    });
    if (res.ok) {
      decisions[id] = decision;
      await fetchPending();
    } else {
      const err = await res.json();
      alert('Error: ' + (err.error || 'Unknown error'));
    }
  } catch (err) {
    alert('Network error: ' + err.message);
  }

  buttons.forEach(b => b.disabled = false);
}

function esc(str) {
  if (!str) return '';
  return str.replace(/&/g,'&amp;').replace(/</g,'&lt;').replace(/>/g,'&gt;').replace(/"/g,'&quot;').replace(/'/g,'&#39;');
}

fetchPending();
setInterval(fetchPending, 3000);
</script>
</body>
</html>`;
}

// ---------------------------------------------------------------------------
// Start Express immediately, then connect Kafka in background
// ---------------------------------------------------------------------------

// Start HTTP server first so the portal is always accessible
app.listen(PORT, () => {
  console.log('============================================');
  console.log(' Third Party Verification & Compliance Portal');
  console.log('============================================');
  console.log(`HTTP portal             : http://localhost:${PORT}`);
  console.log(`Kafka broker            : ${KAFKA_BROKER}`);
  console.log('');
  console.log(`[HTTP] Portal running at http://localhost:${PORT}`);
});

// Connect Kafka in the background
async function connectKafka() {
  await producer.connect();
  console.log('[KAFKA] Producer connected');

  await verificationConsumer.connect();
  console.log('[KAFKA] Verification consumer connected');
  await verificationConsumer.subscribe({ topic: VERIFICATION_REQUEST_TOPIC, fromBeginning: false });
  console.log(`[KAFKA] Subscribed to ${VERIFICATION_REQUEST_TOPIC}`);

  await complianceConsumer.connect();
  console.log('[KAFKA] Compliance consumer connected');
  await complianceConsumer.subscribe({ topic: COMPLIANCE_REQUEST_TOPIC, fromBeginning: false });
  console.log(`[KAFKA] Subscribed to ${COMPLIANCE_REQUEST_TOPIC}`);

  await verificationConsumer.run({
    eachMessage: async ({ message }) => {
      try { await handleVerificationRequest(message); }
      catch (err) { console.error('[ERROR] Failed to process verification message:', err); }
    },
  });

  await complianceConsumer.run({
    eachMessage: async ({ message }) => {
      try { await handleComplianceRequest(message); }
      catch (err) { console.error('[ERROR] Failed to process compliance message:', err); }
    },
  });

  console.log('[READY] Waiting for verification & compliance requests...');
}

connectKafka().catch(err => {
  console.error('[KAFKA] Failed to connect — portal still accessible but cannot receive requests:', err.message);
});
