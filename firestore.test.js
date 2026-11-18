const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_123";
const BOB_UID = "bob_456";
const ADMIN_UID = "admin_super_001";
const ADMIN_EMAIL = "tokpafclara@gmail.com";

const [emulatorHost, emulatorPortStr] = (
  process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085"
).split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

before(async () => {
  const rules = fs.readFileSync("./firestore.rules", "utf8");
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules,
      host: emulatorHost,
      port: emulatorPort,
    },
  });
});

after(async () => {
  if (testEnv) {
    await testEnv.cleanup();
  }
});

beforeEach(async () => {
  if (testEnv) {
    await testEnv.clearFirestore();
  }
});

test("Unauthenticated user: cannot read or write any collection", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).get());
  await assertFails(
    unauthDb.collection("users").doc(ALICE_UID).collection("files").get()
  );
  await assertFails(unauthDb.collection("payment_requests").get());
  await assertFails(unauthDb.collection("audit_logs").get());
});

test("UserProfile: owner can register on FREE tier and update profile, but cannot escalate subscriptionTier or read another user's PII", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const created = new Date(Date.now() - 10000);

  // Reject self-assigned PREMIUM tier on creation
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      fullName: "Alice Johnson",
      username: "alicej",
      email: "alice@example.com",
      phone: "+2348012345678",
      subscriptionTier: "PREMIUM",
      storageUsedBytes: 0,
      storageQuotaBytes: 104857600,
      createdAt: created,
      updatedAt: created,
    })
  );

  // Valid FREE tier registration succeeds
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      fullName: "Alice Johnson",
      username: "alicej",
      email: "alice@example.com",
      phone: "+2348012345678",
      subscriptionTier: "FREE",
      storageUsedBytes: 0,
      storageQuotaBytes: 52428800,
      createdAt: created,
      updatedAt: created,
    })
  );

  // Owner can update fullName and storageUsedBytes
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).update({
      fullName: "Alice A. Johnson",
      storageUsedBytes: 4096,
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Owner CANNOT self-upgrade subscriptionTier
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      subscriptionTier: "PRO",
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Bob CANNOT read Alice's PII profile
  await assertFails(bobDb.collection("users").doc(ALICE_UID).get());
});

test("OfficeFile: owner CRUD succeeds, extra @DocumentId id key and cross-user access fail", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const created = new Date(Date.now() - 10000);

  await assertSucceeds(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("files")
      .doc("doc_1")
      .set({
        userId: ALICE_UID,
        title: "Q4 Financial Proposal",
        fileType: "WORD",
        folderId: "",
        content: "<h1>Executive Summary</h1><p>Revenue grew by 34%.</p>",
        pageSettings: "A4|PORTRAIT|NORMAL",
        sizeBytes: 2048,
        trashed: false,
        sharedWithEmails: "",
        createdAt: created,
        updatedAt: created,
      })
  );

  // Partial update of content and updatedAt succeeds
  await assertSucceeds(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("files")
      .doc("doc_1")
      .update({
        content: "<h1>Updated Summary</h1><p>Revenue grew by 42%.</p>",
        sizeBytes: 2560,
        updatedAt: new Date(Date.now() - 1000),
      })
  );

  // List files in user-scoped subcollection succeeds
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("files").get()
  );

  // Reject extra 'id' field (@DocumentId guard)
  await assertFails(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("files")
      .doc("doc_2")
      .set({
        id: "doc_2",
        userId: ALICE_UID,
        title: "Invalid ID Field",
        fileType: "WORD",
        folderId: "",
        content: "Test",
        pageSettings: "A4|PORTRAIT|NORMAL",
        sizeBytes: 100,
        trashed: false,
        sharedWithEmails: "",
        createdAt: created,
        updatedAt: created,
      })
  );

  // Bob cannot read or modify Alice's files
  await assertFails(
    bobDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("files")
      .doc("doc_1")
      .get()
  );
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).collection("files").get()
  );
});

test("PaymentRequest & Super Admin Verification: only verified Super Admin can approve payments and activate subscriptions", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const spoofedAdminDb = testEnv
    .authenticatedContext(BOB_UID, {
      email: ADMIN_EMAIL,
      email_verified: false,
    })
    .firestore();
  const verifiedAdminDb = testEnv
    .authenticatedContext(ADMIN_UID, {
      email: ADMIN_EMAIL,
      email_verified: true,
    })
    .firestore();
  const created = new Date(Date.now() - 10000);

  // Seed Alice profile
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      fullName: "Alice Johnson",
      username: "alicej",
      email: "alice@example.com",
      phone: "+2348012345678",
      subscriptionTier: "FREE",
      storageUsedBytes: 0,
      storageQuotaBytes: 52428800,
      createdAt: created,
      updatedAt: created,
    })
  );

  // Customer CANNOT submit a pre-verified payment request
  await assertFails(
    aliceDb.collection("payment_requests").doc("pay_1").set({
      userId: ALICE_UID,
      customerName: "Alice Johnson",
      customerEmail: "alice@example.com",
      phoneNumber: "+2348012345678",
      selectedPlan: "PRO",
      amountTransferred: 5000,
      transactionReference: "OPAY-2026-998877",
      paymentDate: "2026-11-17",
      receiptSummary: "OPay transfer receipt to 9080301021 (Emmanuel Ajibade)",
      status: "VERIFIED",
      verifiedByAdminId: ALICE_UID,
      verificationNotes: "Self-approved",
      createdAt: created,
      updatedAt: created,
    })
  );

  // Customer CAN submit a PENDING payment request
  await assertSucceeds(
    aliceDb.collection("payment_requests").doc("pay_1").set({
      userId: ALICE_UID,
      customerName: "Alice Johnson",
      customerEmail: "alice@example.com",
      phoneNumber: "+2348012345678",
      selectedPlan: "PRO",
      amountTransferred: 5000,
      transactionReference: "OPAY-2026-998877",
      paymentDate: "2026-11-17",
      receiptSummary: "OPay transfer receipt to 9080301021 (Emmanuel Ajibade)",
      status: "PENDING",
      verifiedByAdminId: "",
      verificationNotes: "",
      createdAt: created,
      updatedAt: created,
    })
  );

  // Customer CANNOT approve their own payment request
  await assertFails(
    aliceDb.collection("payment_requests").doc("pay_1").update({
      status: "VERIFIED",
      verifiedByAdminId: ALICE_UID,
      verificationNotes: "Approved",
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Spoofed admin (email_verified: false) CANNOT approve payment request
  await assertFails(
    spoofedAdminDb.collection("payment_requests").doc("pay_1").update({
      status: "VERIFIED",
      verifiedByAdminId: BOB_UID,
      verificationNotes: "Spoofed approval",
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Verified Super Admin CAN approve payment request, upgrade user subscription, and write immutable audit log
  await assertSucceeds(
    verifiedAdminDb.collection("payment_requests").doc("pay_1").update({
      status: "VERIFIED",
      verifiedByAdminId: ADMIN_UID,
      verificationNotes: "Confirmed credit of NGN 5,000 in OPay 9080301021",
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  await assertSucceeds(
    verifiedAdminDb.collection("users").doc(ALICE_UID).update({
      subscriptionTier: "PRO",
      storageQuotaBytes: 5368709120,
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  await assertSucceeds(
    verifiedAdminDb.collection("audit_logs").doc("log_1").set({
      actorUid: ADMIN_UID,
      actorName: "amazingdigitals",
      actionType: "PAYMENT_VERIFIED",
      targetId: "pay_1",
      details: "Approved PRO subscription for alice@example.com (Ref: OPAY-2026-998877)",
      createdAt: new Date(Date.now() - 1000),
    })
  );

  // Audit logs are immutable (cannot be deleted even by admin)
  await assertFails(
    verifiedAdminDb.collection("audit_logs").doc("log_1").delete()
  );
});

test("PricingPlans: filtered query succeeds for signed-in user; only Super Admin can update pricing", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const verifiedAdminDb = testEnv
    .authenticatedContext(ADMIN_UID, {
      email: ADMIN_EMAIL,
      email_verified: true,
    })
    .firestore();
  const now = new Date(Date.now() - 1000);

  await assertSucceeds(
    verifiedAdminDb.collection("pricing_plans").doc("PRO").set({
      tier: "PRO",
      name: "Pro Suite",
      priceNgn: 5000,
      storageQuotaMb: 5120,
      featuresSummary: "Full Word, Sheets, Slides, PDF Studio, Image Editor & AI Assistant",
      updatedByUid: ADMIN_UID,
      updatedAt: now,
    })
  );

  // Filtered query matching rule succeeds
  await assertSucceeds(
    aliceDb.collection("pricing_plans").where("priceNgn", ">=", 0).get()
  );

  // Ordinary user cannot modify pricing plans
  await assertFails(
    aliceDb.collection("pricing_plans").doc("PRO").update({
      priceNgn: 100,
      updatedByUid: ALICE_UID,
      updatedAt: now,
    })
  );
});
