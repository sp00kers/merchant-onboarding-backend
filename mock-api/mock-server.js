/**
 * Mock External Verification API
 *
 * Simulates external verification agencies (Bank Negara, SSM, CTOS, etc.)
 * by consuming verification request events from Kafka, processing them with
 * realistic mock logic, and publishing verification response events back.
 *
 * Verification types handled:
 *   - BUSINESS_REGISTRY   (SSM / Companies Commission of Malaysia)
 *   - IDENTITY_VERIFICATION (National Registration Department / watchlist)
 *   - ADDRESS_VERIFICATION  (Geocoding / postal verification)
 *   - FINANCIAL_CHECK       (CTOS / credit bureau)
 *   - SANCTIONS_SCREENING   (Bank Negara / UN / OFAC / EU sanctions lists)
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
// Verification handlers — one per type, replicating the original Java logic
// ---------------------------------------------------------------------------

function randomInt(min, max) {
  return Math.floor(Math.random() * (max - min + 1)) + min;
}

function handleBusinessRegistry(request) {
  const responseData = {
    registryMatch: true,
    businessName: request.businessName,
    registrationNumber: request.registrationNumber,
    registrationStatus: 'ACTIVE',
    incorporationDate: '2018-03-15',
    verifiedBy: 'SSM (Suruhanjaya Syarikat Malaysia)',
  };
  const riskIndicators = [];
  let baseScore = randomInt(70, 94);

  if (request.businessType === 'Bhd') {
    baseScore += 5;
  } else if (request.businessType === 'Sole Proprietorship') {
    baseScore -= 5;
    riskIndicators.push('Higher risk business structure');
  }

  return finalize(baseScore, responseData, riskIndicators, 'Business registry verification completed successfully via SSM');
}

function handleIdentityVerification(request) {
  const pepStatus = Math.random() < 0.5 ? 'NOT_PEP' : 'POTENTIAL_PEP';
  const responseData = {
    identityVerified: true,
    directorName: request.directorName,
    icNumber: request.directorIC,
    watchlistCheck: 'CLEAR',
    pepStatus,
    verifiedBy: 'National Registration Department (JPN)',
  };
  const riskIndicators = [];
  let baseScore = randomInt(70, 94);

  if (pepStatus === 'POTENTIAL_PEP') {
    baseScore -= 10;
    riskIndicators.push('Potential Politically Exposed Person (PEP)');
  }

  return finalize(baseScore, responseData, riskIndicators, 'Identity verification completed via JPN');
}

function handleAddressVerification(request) {
  const responseData = {
    addressVerified: true,
    address: request.businessAddress,
    addressType: 'COMMERCIAL',
    geocodeConfidence: 'HIGH',
    verifiedBy: 'Pos Malaysia Address Verification',
  };
  const riskIndicators = [];
  let baseScore = randomInt(70, 94);

  if (!request.businessAddress || request.businessAddress.length < 20) {
    baseScore -= 15;
    riskIndicators.push('Incomplete address information');
  }

  return finalize(baseScore, responseData, riskIndicators, 'Address verification completed');
}

function handleFinancialCheck(request) {
  const outstandingLitigation = Math.random() < 0.2;
  const responseData = {
    creditScore: randomInt(650, 800),
    bankruptcyCheck: 'CLEAR',
    outstandingLitigation,
    estimatedRevenue: 'RM ' + randomInt(100000, 1000000),
    verifiedBy: 'CTOS Data Systems',
  };
  const riskIndicators = [];
  let baseScore = randomInt(70, 94);

  if (outstandingLitigation) {
    baseScore -= 20;
    riskIndicators.push('Outstanding litigation detected');
  }

  return finalize(baseScore, responseData, riskIndicators, 'Financial check completed via CTOS');
}

function handleSanctionsScreening(request) {
  const potentialMatch = Math.random() < 0.1; // 10% chance
  const responseData = {
    sanctionsMatch: potentialMatch,
    screenedLists: ['UN Sanctions', 'OFAC SDN', 'EU Sanctions', 'BNM Sanctions'],
    businessName: request.businessName,
    directorName: request.directorName,
    matchScore: potentialMatch ? randomInt(60, 90) : 0,
    verifiedBy: 'Bank Negara Malaysia (BNM) Sanctions Screening',
  };
  const riskIndicators = [];
  let baseScore = randomInt(70, 94);

  if (potentialMatch) {
    baseScore -= 30;
    riskIndicators.push('Potential sanctions list match detected');
    riskIndicators.push('Manual review required for sanctions clearance');
  }

  return finalize(baseScore, responseData, riskIndicators, 'Sanctions screening completed via BNM');
}

function finalize(baseScore, responseData, riskIndicators, notes) {
  // Apply random variation ±5, clamp to [30, 100]
  baseScore = Math.max(30, Math.min(100, baseScore + randomInt(-5, 5)));
  const success = baseScore >= 50;
  return {
    status: success ? 'COMPLETED' : 'FAILED',
    confidenceScore: baseScore,
    responseData: JSON.stringify(responseData),
    riskIndicators: JSON.stringify(riskIndicators),
    notes,
  };
}

const handlers = {
  BUSINESS_REGISTRY: handleBusinessRegistry,
  IDENTITY_VERIFICATION: handleIdentityVerification,
  ADDRESS_VERIFICATION: handleAddressVerification,
  FINANCIAL_CHECK: handleFinancialCheck,
  SANCTIONS_SCREENING: handleSanctionsScreening,
};

// ---------------------------------------------------------------------------
// Main processing loop
// ---------------------------------------------------------------------------

async function processMessage(message) {
  const request = JSON.parse(message.value.toString());
  console.log(`[RECEIVED] Verification request — case=${request.caseId} type=${request.verificationType}`);

  // Simulate external agency processing delay (3–5 seconds)
  const delay = randomInt(3000, 5000);
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
    completedAt: new Date().toISOString(),
  };

  await producer.send({
    topic: RESPONSE_TOPIC,
    messages: [{
      key: request.caseId,
      value: JSON.stringify(responseEvent),
    }],
  });

  console.log(`[PUBLISHED] Response for case=${request.caseId} type=${request.verificationType} → status=${result.status} score=${result.confidenceScore}`);
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
