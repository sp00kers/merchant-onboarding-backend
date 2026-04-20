/**
 * Mock External Verification API
 *
 * Simulates external verification agencies by consuming verification request
 * events from Kafka, processing them with deterministic keyword-based logic,
 * and publishing verification response events back.
 *
 * Verification types handled:
 *   - BUSINESS_REGISTRATION  (Business Registration Certificate)
 *   - DIRECTOR_ID            (Director Government ID)
 *   - BENEFICIAL_OWNERSHIP   (Beneficial Ownership Declaration)
 *
 * Deterministic keyword triggers:
 *   BUSINESS_REGISTRATION — registrationNumber starting with "INVALID" or "EXPIRED" → FAILED
 *   DIRECTOR_ID           — directorIC starting with "BLOCK" or "FAKE" → FAILED
 *   BENEFICIAL_OWNERSHIP  — directorName containing "SANCTIONED", or ownershipPercentage > 100 or <= 0 → FAILED
 *   Any blank required field for a given type → FAILED
 */

const { Kafka } = require('kafkajs');

const KAFKA_BROKER = process.env.KAFKA_BROKER || 'kafka:29092';
const REQUEST_TOPIC = process.env.REQUEST_TOPIC || 'verification.requests.topic';
const RESPONSE_TOPIC = process.env.RESPONSE_TOPIC || 'verification.responses.topic';

const kafka = new Kafka({
  clientId: 'mock-external-api',
  brokers: [KAFKA_BROKER],
  retry: {
    initialRetryTime: 3000,
    retries: 10
  }
});

const consumer = kafka.consumer({ groupId: 'mock-external-verification-group' });
const producer = kafka.producer();

// ---------------------------------------------------------------------------
// Helper
// ---------------------------------------------------------------------------

function randomInt(min, max) {
  return Math.floor(Math.random() * (max - min + 1)) + min;
}

function isBlank(value) {
  return value === null || value === undefined || String(value).trim() === '';
}

// ---------------------------------------------------------------------------
// Deterministic verification handlers
// ---------------------------------------------------------------------------

function handleBusinessRegistration(request) {
  const regNum = (request.registrationNumber || '').trim();
  const bizName = (request.businessName || '').trim();

  const responseData = {
    registryMatch: true,
    businessName: bizName,
    registrationNumber: regNum,
    registrationStatus: 'ACTIVE',
    verifiedBy: 'SSM (Suruhanjaya Syarikat Malaysia)',
  };
  const riskIndicators = [];

  // Blank required fields
  if (isBlank(regNum)) {
    riskIndicators.push('Registration number is blank');
    return buildResult('FAILED', 0, responseData, riskIndicators, 'Business registration verification failed — missing registration number');
  }
  if (isBlank(bizName)) {
    riskIndicators.push('Business name is blank');
    return buildResult('FAILED', 0, responseData, riskIndicators, 'Business registration verification failed — missing business name');
  }

  // Keyword triggers
  const upper = regNum.toUpperCase();
  if (upper.startsWith('INVALID')) {
    responseData.registrationStatus = 'INVALID';
    responseData.registryMatch = false;
    riskIndicators.push('Registration number flagged as INVALID');
    return buildResult('FAILED', 0, responseData, riskIndicators, 'Business registration verification failed — invalid registration number');
  }
  if (upper.startsWith('EXPIRED')) {
    responseData.registrationStatus = 'EXPIRED';
    riskIndicators.push('Registration number flagged as EXPIRED');
    return buildResult('FAILED', 0, responseData, riskIndicators, 'Business registration verification failed — expired registration');
  }

  // All good
  return buildResult('PASSED', 100, responseData, riskIndicators, 'Business registration verification passed — registration is active and valid');
}

function handleDirectorId(request) {
  const ic = (request.directorIC || '').trim();
  const dirName = (request.directorName || '').trim();

  const responseData = {
    identityVerified: true,
    directorName: dirName,
    icNumber: ic,
    watchlistCheck: 'CLEAR',
    verifiedBy: 'National Registration Department (JPN)',
  };
  const riskIndicators = [];

  // Blank required fields
  if (isBlank(ic)) {
    riskIndicators.push('Director IC number is blank');
    return buildResult('FAILED', 0, responseData, riskIndicators, 'Director ID verification failed — missing IC number');
  }
  if (isBlank(dirName)) {
    riskIndicators.push('Director name is blank');
    return buildResult('FAILED', 0, responseData, riskIndicators, 'Director ID verification failed — missing director name');
  }

  // Keyword triggers
  const upper = ic.toUpperCase();
  if (upper.startsWith('BLOCK')) {
    responseData.identityVerified = false;
    responseData.watchlistCheck = 'BLOCKED';
    riskIndicators.push('Director IC is on the blocked list');
    return buildResult('FAILED', 0, responseData, riskIndicators, 'Director ID verification failed — IC number is blocked');
  }
  if (upper.startsWith('FAKE')) {
    responseData.identityVerified = false;
    riskIndicators.push('Director IC flagged as potentially fraudulent');
    return buildResult('FAILED', 0, responseData, riskIndicators, 'Director ID verification failed — IC number appears fraudulent');
  }

  // All good
  return buildResult('PASSED', 100, responseData, riskIndicators, 'Director ID verification passed — identity confirmed');
}

function handleBeneficialOwnership(request) {
  const dirName = (request.directorName || '').trim();
  const ownership = request.ownershipPercentage;

  const responseData = {
    directorName: dirName,
    ownershipPercentage: ownership,
    sanctionsCheck: 'CLEAR',
    verifiedBy: 'Bank Negara Malaysia (BNM)',
  };
  const riskIndicators = [];

  // Blank required fields
  if (isBlank(dirName)) {
    riskIndicators.push('Director name is blank');
    return buildResult('FAILED', 0, responseData, riskIndicators, 'Beneficial ownership verification failed — missing director name');
  }

  // Keyword triggers
  if (dirName.toUpperCase().includes('SANCTIONED')) {
    responseData.sanctionsCheck = 'MATCH';
    riskIndicators.push('Director name matched sanctions list');
    return buildResult('FAILED', 0, responseData, riskIndicators, 'Beneficial ownership verification failed — director is on sanctions list');
  }

  // Ownership percentage checks
  if (ownership !== null && ownership !== undefined) {
    if (ownership > 100) {
      riskIndicators.push('Ownership percentage exceeds 100%');
      return buildResult('FAILED', 0, responseData, riskIndicators, 'Beneficial ownership verification failed — ownership percentage exceeds 100%');
    }
    if (ownership <= 0) {
      riskIndicators.push('Ownership percentage is zero or negative');
      return buildResult('FAILED', 0, responseData, riskIndicators, 'Beneficial ownership verification failed — ownership percentage must be positive');
    }
  }

  // All good
  return buildResult('PASSED', 100, responseData, riskIndicators, 'Beneficial ownership verification passed — no issues found');
}

function buildResult(status, confidenceScore, responseData, riskIndicators, notes) {
  return {
    status,
    confidenceScore,
    responseData: JSON.stringify(responseData),
    riskIndicators: JSON.stringify(riskIndicators),
    notes,
  };
}

const handlers = {
  BUSINESS_REGISTRATION: handleBusinessRegistration,
  DIRECTOR_ID: handleDirectorId,
  BENEFICIAL_OWNERSHIP: handleBeneficialOwnership,
};

// ---------------------------------------------------------------------------
// Main processing loop
// ---------------------------------------------------------------------------

async function processMessage(message) {
  const request = JSON.parse(message.value.toString());
  console.log(`[RECEIVED] Verification request — case=${request.caseId} type=${request.verificationType}`);

  // Simulate external agency processing delay (500ms – 2s)
  const delay = randomInt(500, 2000);
  console.log(`[PROCESSING] Simulating ${request.verificationType} agency call (${delay}ms)...`);
  await new Promise(resolve => setTimeout(resolve, delay));

  const handler = handlers[request.verificationType];
  if (!handler) {
    console.error(`[ERROR] Unknown verification type: ${request.verificationType}`);
    return;
  }

  const result = handler(request);

  const responseEvent = {
    caseId: request.caseId,
    verificationType: request.verificationType,
    externalReference: request.externalReference,
    status: result.status,
    confidenceScore: result.confidenceScore,
    responseData: result.responseData,
    riskIndicators: result.riskIndicators,
    notes: result.notes,
    completedAt: new Date().toISOString().replace('Z', ''),
  };

  await producer.send({
    topic: RESPONSE_TOPIC,
    messages: [{
      key: request.caseId,
      value: JSON.stringify(responseEvent),
    }],
  });

  console.log(`[PUBLISHED] Response for case=${request.caseId} type=${request.verificationType} → status=${result.status}`);
}

async function run() {
  console.log('============================================');
  console.log(' Mock External Verification API');
  console.log('============================================');
  console.log(`Kafka broker : ${KAFKA_BROKER}`);
  console.log(`Request topic: ${REQUEST_TOPIC}`);
  console.log(`Response topic: ${RESPONSE_TOPIC}`);
  console.log('');

  await producer.connect();
  console.log('[KAFKA] Producer connected');

  await consumer.connect();
  console.log('[KAFKA] Consumer connected');

  await consumer.subscribe({ topic: REQUEST_TOPIC, fromBeginning: false });
  console.log(`[KAFKA] Subscribed to ${REQUEST_TOPIC}`);

  await consumer.run({
    eachMessage: async ({ topic, partition, message }) => {
      try {
        await processMessage(message);
      } catch (err) {
        console.error('[ERROR] Failed to process message:', err);
      }
    },
  });

  console.log('[READY] Waiting for verification requests...');
}

run().catch(err => {
  console.error('[FATAL]', err);
  process.exit(1);
});
