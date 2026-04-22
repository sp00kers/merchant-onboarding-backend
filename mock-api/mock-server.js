/**
 * Mock External Verification & Compliance Review API
 *
 * Simulates external agencies for:
 *   1. Background Verification (document-based)
 *      - BUSINESS_REGISTRATION, DIRECTOR_ID, BENEFICIAL_OWNERSHIP
 *   2. Compliance Review (document-based)
 *      - BUSINESS_LICENSE, PCI_DSS_SAQ, TERMS_OF_SERVICE
 */

const { Kafka } = require('kafkajs');

const KAFKA_BROKER = process.env.KAFKA_BROKER || 'kafka:29092';

// Background Verification topics
const VERIFICATION_REQUEST_TOPIC = process.env.REQUEST_TOPIC || 'verification.requests.topic';
const VERIFICATION_RESPONSE_TOPIC = process.env.RESPONSE_TOPIC || 'verification.responses.topic';

// Compliance Review topics
const COMPLIANCE_REQUEST_TOPIC = process.env.COMPLIANCE_REQUEST_TOPIC || 'compliance.requests.topic';
const COMPLIANCE_RESPONSE_TOPIC = process.env.COMPLIANCE_RESPONSE_TOPIC || 'compliance.responses.topic';

const kafka = new Kafka({
  clientId: 'mock-external-api',
  brokers: [KAFKA_BROKER],
  retry: {
    initialRetryTime: 3000,
    retries: 10
  }
});

const verificationConsumer = kafka.consumer({ groupId: 'mock-external-verification-group' });
const complianceConsumer = kafka.consumer({ groupId: 'mock-compliance-review-group' });
const producer = kafka.producer();

// ---------------------------------------------------------------------------
// Document filename-based outcome detection
//
// The uploaded document filename determines pass/fail:
//   1. No document uploaded = FAILED
//   2. Filename contains failure keyword = FAILED
//      (expired, revoked, invalid, refused, suspended, fake)
//   3. Filename does NOT contain accepted keyword = FAILED (wrong document)
//   4. Otherwise = PASSED
// ---------------------------------------------------------------------------

const FAILURE_KEYWORDS = ['expired', 'revoked', 'invalid', 'refused', 'suspended', 'fake'];

// Each verification/compliance type requires the filename to contain at least one accepted keyword
const ACCEPTED_KEYWORDS = {
  // Background Verification
  BUSINESS_REGISTRATION: ['business_certificate'],
  DIRECTOR_ID: ['ic'],
  BENEFICIAL_OWNERSHIP: ['beneficial_ownership'],
  // Compliance Review
  BUSINESS_LICENSE: ['business_license'],
  PCI_DSS_SAQ: ['pci_dss'],
  TERMS_OF_SERVICE: ['terms_of_service'],
};

function isDocumentFailed(filename) {
  if (!filename) return true; // no document = fail
  const lower = filename.toLowerCase();
  return FAILURE_KEYWORDS.some(kw => lower.includes(kw));
}

function isWrongDocument(type, filename) {
  if (!filename) return true;
  const keywords = ACCEPTED_KEYWORDS[type];
  if (!keywords) return false; // unknown type — don't block
  const lower = filename.toLowerCase();
  return !keywords.some(kw => lower.includes(kw));
}

// ---------------------------------------------------------------------------
// Background Verification handlers
// ---------------------------------------------------------------------------

function handleBusinessRegistration(request) {
  if (isDocumentFailed(request.documentFileName)) {
    return finalize(35, {
      registryMatch: false, businessName: request.businessName,
      registrationNumber: request.registrationNumber, registrationStatus: 'NOT_FOUND',
      verifiedBy: 'SSM (Suruhanjaya Syarikat Malaysia)',
    }, ['Business registration document verification failed'], 'Business Registration Certificate verification failed');
  }
  if (isWrongDocument('BUSINESS_REGISTRATION', request.documentFileName)) {
    return finalize(20, {
      registryMatch: false, businessName: request.businessName,
      registrationNumber: request.registrationNumber, registrationStatus: 'WRONG_DOCUMENT',
      verifiedBy: 'SSM (Suruhanjaya Syarikat Malaysia)',
    }, ['Wrong document type submitted for Business Registration verification'], 'Incorrect document uploaded — expected a Business Registration Certificate');
  }
  return finalize(88, {
    registryMatch: true, businessName: request.businessName,
    registrationNumber: request.registrationNumber, registrationStatus: 'ACTIVE',
    incorporationDate: '2018-03-15', verifiedBy: 'SSM (Suruhanjaya Syarikat Malaysia)',
  }, [], 'Business Registration Certificate verified successfully via SSM');
}

function handleDirectorId(request) {
  if (isDocumentFailed(request.documentFileName)) {
    return finalize(30, {
      identityVerified: false, directorName: request.directorName,
      icNumber: request.directorIC, documentAuthenticity: 'UNVERIFIED',
      pepStatus: 'UNKNOWN', verifiedBy: 'National Registration Department (JPN)',
    }, ['Director identity document could not be verified'], 'Director Government ID verification failed');
  }
  if (isWrongDocument('DIRECTOR_ID', request.documentFileName)) {
    return finalize(18, {
      identityVerified: false, directorName: request.directorName,
      icNumber: request.directorIC, documentAuthenticity: 'WRONG_DOCUMENT',
      pepStatus: 'UNKNOWN', verifiedBy: 'National Registration Department (JPN)',
    }, ['Wrong document type submitted for Director ID verification'], 'Incorrect document uploaded — expected a Director Government ID');
  }
  return finalize(90, {
    identityVerified: true, directorName: request.directorName,
    icNumber: request.directorIC, documentAuthenticity: 'VERIFIED',
    pepStatus: 'NOT_PEP', verifiedBy: 'National Registration Department (JPN)',
  }, [], 'Director Government ID verified via JPN');
}

function handleBeneficialOwnership(request) {
  if (isDocumentFailed(request.documentFileName)) {
    return finalize(32, {
      declarationReceived: false, businessName: request.businessName,
      directorName: request.directorName, ownershipStructureVerified: false,
      verifiedBy: 'SSM Beneficial Ownership Registry',
    }, ['Beneficial ownership document verification failed'], 'Beneficial Ownership Declaration verification failed');
  }
  if (isWrongDocument('BENEFICIAL_OWNERSHIP', request.documentFileName)) {
    return finalize(15, {
      declarationReceived: false, businessName: request.businessName,
      directorName: request.directorName, ownershipStructureVerified: false,
      verifiedBy: 'SSM Beneficial Ownership Registry',
    }, ['Wrong document type submitted for Beneficial Ownership verification'], 'Incorrect document uploaded — expected a Beneficial Ownership Declaration');
  }
  return finalize(87, {
    declarationReceived: true, businessName: request.businessName,
    directorName: request.directorName, ownershipStructureVerified: true,
    verifiedBy: 'SSM Beneficial Ownership Registry',
  }, [], 'Beneficial Ownership Declaration verified via SSM registry');
}

function finalize(score, responseData, riskIndicators, notes) {
  return {
    status: score >= 50 ? 'PASSED' : 'FAILED',
    confidenceScore: score,
    responseData: JSON.stringify(responseData),
    riskIndicators: JSON.stringify(riskIndicators),
    notes,
  };
}

const verificationHandlers = {
  BUSINESS_REGISTRATION: handleBusinessRegistration,
  DIRECTOR_ID: handleDirectorId,
  BENEFICIAL_OWNERSHIP: handleBeneficialOwnership,
};

// ---------------------------------------------------------------------------
// Compliance Review handlers
// ---------------------------------------------------------------------------

function handleBusinessLicense(request) {
  if (isDocumentFailed(request.documentFileName)) {
    return { status: 'FAILED', reason: 'Business license document verification failed — document may be expired, revoked, or invalid' };
  }
  if (isWrongDocument('BUSINESS_LICENSE', request.documentFileName)) {
    return { status: 'FAILED', reason: 'Incorrect document uploaded — expected a Business License' };
  }
  return { status: 'PASSED', reason: 'Business license verified successfully' };
}

function handlePciDssSaq(request) {
  if (isDocumentFailed(request.documentFileName)) {
    return { status: 'FAILED', reason: 'PCI DSS self-assessment document verification failed — document may be invalid or incomplete' };
  }
  if (isWrongDocument('PCI_DSS_SAQ', request.documentFileName)) {
    return { status: 'FAILED', reason: 'Incorrect document uploaded — expected a PCI DSS Self-Assessment Questionnaire' };
  }
  return { status: 'PASSED', reason: 'PCI DSS compliance requirements met' };
}

function handleTermsOfService(request) {
  if (isDocumentFailed(request.documentFileName)) {
    return { status: 'FAILED', reason: 'Terms of Service document verification failed — document may be unsigned or refused' };
  }
  if (isWrongDocument('TERMS_OF_SERVICE', request.documentFileName)) {
    return { status: 'FAILED', reason: 'Incorrect document uploaded — expected a Terms of Service document' };
  }
  return { status: 'PASSED', reason: 'Terms of Service accepted and verified' };
}

const complianceHandlers = {
  BUSINESS_LICENSE: handleBusinessLicense,
  PCI_DSS_SAQ: handlePciDssSaq,
  TERMS_OF_SERVICE: handleTermsOfService,
};

// ---------------------------------------------------------------------------
// Message processing
// ---------------------------------------------------------------------------

function randomInt(min, max) {
  return Math.floor(Math.random() * (max - min + 1)) + min;
}

async function processVerificationMessage(message) {
  const request = JSON.parse(message.value.toString());
  console.log(`[VERIFICATION] Received — case=${request.caseId} type=${request.verificationType} file="${request.documentFileName || 'none'}"`);

  const delay = randomInt(3000, 5000);
  console.log(`[VERIFICATION] Simulating ${request.verificationType} agency call (${delay}ms)...`);
  await new Promise(resolve => setTimeout(resolve, delay));

  const handler = verificationHandlers[request.verificationType];
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
    topic: VERIFICATION_RESPONSE_TOPIC,
    messages: [{ key: request.caseId, value: JSON.stringify(responseEvent) }],
  });

  console.log(`[VERIFICATION] Published response for case=${request.caseId} type=${request.verificationType} to status=${result.status}`);
}

async function processComplianceMessage(message) {
  const request = JSON.parse(message.value.toString());
  console.log(`[COMPLIANCE] Received — case=${request.caseId} type=${request.documentType} file="${request.documentFileName || 'none'}"`);

  const delay = randomInt(500, 1500);
  console.log(`[COMPLIANCE] Simulating ${request.documentType} review (${delay}ms)...`);
  await new Promise(resolve => setTimeout(resolve, delay));

  const handler = complianceHandlers[request.documentType];
  if (!handler) {
    console.error(`[ERROR] Unknown compliance document type: ${request.documentType}`);
    return;
  }

  const result = handler(request);

  const responseEvent = {
    caseId: request.caseId,
    documentType: request.documentType,
    externalReference: request.externalReference,
    status: result.status,
    reason: result.reason,
    completedAt: new Date().toISOString(),
  };

  await producer.send({
    topic: COMPLIANCE_RESPONSE_TOPIC,
    messages: [{ key: request.caseId, value: JSON.stringify(responseEvent) }],
  });

  console.log(`[COMPLIANCE] Published response for case=${request.caseId} type=${request.documentType} to status=${result.status}`);
}

// ---------------------------------------------------------------------------
// Start
// ---------------------------------------------------------------------------

async function run() {
  console.log('============================================');
  console.log(' Mock External Verification & Compliance API');
  console.log('============================================');
  console.log(`Kafka broker            : ${KAFKA_BROKER}`);
  console.log(`Verification request    : ${VERIFICATION_REQUEST_TOPIC}`);
  console.log(`Verification response   : ${VERIFICATION_RESPONSE_TOPIC}`);
  console.log(`Compliance request      : ${COMPLIANCE_REQUEST_TOPIC}`);
  console.log(`Compliance response     : ${COMPLIANCE_RESPONSE_TOPIC}`);
  console.log('');

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
    eachMessage: async ({ topic, partition, message }) => {
      try {
        await processVerificationMessage(message);
      } catch (err) {
        console.error('[ERROR] Failed to process verification message:', err);
      }
    },
  });

  await complianceConsumer.run({
    eachMessage: async ({ topic, partition, message }) => {
      try {
        await processComplianceMessage(message);
      } catch (err) {
        console.error('[ERROR] Failed to process compliance message:', err);
      }
    },
  });

  console.log('[READY] Waiting for verification & compliance requests...');
}

run().catch(err => {
  console.error('[FATAL]', err);
  process.exit(1);
});
