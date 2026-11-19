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
const ADMIN_UID = "admin_789";

const [emulatorHost, emulatorPortStr] = (process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085").split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

function pastDate(msOffset = 2000) {
  return new Date(Date.now() - msOffset);
}

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

test("Unauthenticated users cannot read or write any collection", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("tournaments").get());
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).get());
  await assertFails(unauthDb.collection("deposits").where("userId", "==", ALICE_UID).get());
  await assertFails(unauthDb.collection("coupons").get());
  await assertFails(unauthDb.collection("app_config").doc("gateway").get());
  await assertFails(unauthDb.collection("app_config").get());
  await assertFails(unauthDb.collection("app_config").doc("otp").get());
});

test("User profile: owner creation, safe update, cross-user PII isolation, and role escalation block", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID, { email: "alice@example.com", email_verified: true }).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID, { email: "bob@example.com", email_verified: true }).firestore();
  const ts = pastDate(5000);

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      email: "alice@example.com",
      displayName: "AlicePro",
      depositBalance: 0,
      winningBalance: 0,
      bonusBalance: 0,
      totalWinnings: 0,
      totalKills: 0,
      matchesPlayed: 0,
      referralCode: "ALICE99",
      referralQualified: false,
      referralCount: 0,
      referralEarnings: 0,
      streakDays: 0,
      role: "user",
      banned: false,
      pushEnabled: true,
      createdAt: ts,
      updatedAt: ts,
    })
  );

  // Self-assigned admin role on creation must fail
  await assertFails(
    bobDb.collection("users").doc(BOB_UID).set({
      userId: BOB_UID,
      email: "bob@example.com",
      displayName: "HackerBob",
      depositBalance: 0,
      winningBalance: 0,
      bonusBalance: 0,
      referralCode: "BOB99",
      role: "admin",
      banned: false,
      createdAt: ts,
      updatedAt: ts,
    })
  );

  // Role escalation on update must fail
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      role: "admin",
      updatedAt: pastDate(1000),
    })
  );

  // Ghost field shadow update must fail
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      isVerifiedAdmin: true,
      updatedAt: pastDate(1000),
    })
  );

  // Cross-user PII read and unfiltered list must fail
  await assertFails(bobDb.collection("users").doc(ALICE_UID).get());
  await assertFails(bobDb.collection("users").get());

  // Safe profile edit succeeds
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).update({
      inGameName: "AhxanSlayer",
      gameUid: "5123456789",
      updatedAt: pastDate(1000),
    })
  );
});

test("Room secret gating: only joined players and admins can read roomId/roomPassword", async () => {
  const ts = pastDate(5000);
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    const db = ctx.firestore();
    await db.collection("tournaments").doc("match_1").set({
      title: "Cyber Scrims",
      game: "BGMI",
      mode: "Squad",
      entryType: "Paid",
      entryFee: 50,
      prizePool: 2000,
      maxSlots: 25,
      filledSlots: 1,
      status: "upcoming",
      scheduledAtMillis: Date.now() + 3600000,
      roomPublished: true,
      createdAt: ts,
      updatedAt: ts,
    });
    await db.collection("tournaments").doc("match_1").collection("secrets").doc("room").set({
      roomId: "99441122",
      roomPassword: "CYBER",
      updatedAt: ts,
    });
    await db.collection("tournaments").doc("match_1").collection("slots").doc(ALICE_UID).set({
      userId: ALICE_UID,
      matchId: "match_1",
      inGameName: "AliceIGN",
      gameUid: "112233",
      slotNumber: 1,
      entryPaid: 50,
      status: "joined",
      createdAt: ts,
      updatedAt: ts,
    });
  });

  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();

  // Joined player Alice can read room secret
  await assertSucceeds(
    aliceDb.collection("tournaments").doc("match_1").collection("secrets").doc("room").get()
  );

  // Signed-in player Bob CAN list tournament slots for participant list & slot picker
  await assertSucceeds(
    bobDb.collection("tournaments").doc("match_1").collection("slots").get()
  );

  // Non-joined player Bob CANNOT read room secret
  await assertFails(
    bobDb.collection("tournaments").doc("match_1").collection("secrets").doc("room").get()
  );
});

test("Deposits & Withdrawals: owner-scoped queries succeed, unfiltered list fails, cross-user access fails", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const ts = pastDate(3000);

  await assertSucceeds(
    aliceDb.collection("deposits").doc("dep_1").set({
      userId: ALICE_UID,
      clientOrderId: "AHXAN_1001",
      payzapiOrderId: "PZ_1001",
      amount: 100,
      provider: "fampay",
      status: "pending",
      expiryMillis: Date.now() + 900000,
      createdAt: ts,
      updatedAt: ts,
    })
  );

  // Cannot create deposit already marked completed
  await assertFails(
    aliceDb.collection("deposits").doc("dep_fake").set({
      userId: ALICE_UID,
      clientOrderId: "AHXAN_1002",
      payzapiOrderId: "PZ_1002",
      amount: 1000,
      provider: "fampay",
      status: "completed",
      expiryMillis: Date.now() + 900000,
      createdAt: ts,
      updatedAt: ts,
    })
  );

  // Owner query succeeds; unfiltered query or Bob reading Alice's deposit fails
  await assertSucceeds(aliceDb.collection("deposits").where("userId", "==", ALICE_UID).get());
  await assertFails(aliceDb.collection("deposits").get());
  await assertFails(bobDb.collection("deposits").doc("dep_1").get());

  // Complete deposit transition succeeds for owner while keeping amount immutable
  await assertSucceeds(
    aliceDb.collection("deposits").doc("dep_1").update({
      status: "completed",
      utr: "UTR44556677",
      promoBonusCredited: 20,
      paidAtMillis: Date.now(),
      updatedAt: pastDate(500),
    })
  );
});

test("SuperAdmin RBAC and immutable audit logs", async () => {
  const adminDb = testEnv.authenticatedContext(ADMIN_UID, {
    email: "ahxantalib@gmail.com",
    email_verified: true,
  }).firestore();
  const aliceDb = testEnv.authenticatedContext(ALICE_UID, {
    email: "alice@example.com",
    email_verified: true,
  }).firestore();
  const ts = pastDate(2000);

  await assertSucceeds(
    adminDb.collection("audit_logs").doc("log_1").set({
      adminUid: ADMIN_UID,
      adminEmail: "ahxantalib@gmail.com",
      actionType: "PUBLISH_ROOM",
      targetId: "match_1",
      summary: "Published Room ID",
      createdAt: ts,
    })
  );

  // Audit logs cannot be modified or deleted even by superAdmin
  await assertFails(
    adminDb.collection("audit_logs").doc("log_1").update({ summary: "Tampered" })
  );
  await assertFails(adminDb.collection("audit_logs").doc("log_1").delete());

  // Non-admin cannot read audit logs or create coupons
  await assertFails(aliceDb.collection("audit_logs").get());
  await assertFails(
    aliceDb.collection("coupons").doc("HACK100").set({
      code: "HACK100",
      couponType: "deposit_bonus",
      valueType: "flat",
      value: 1000,
      minAmount: 1,
      maxCap: 1000,
      maxGlobalUses: 10,
      maxPerUserUses: 1,
      usedCount: 0,
      audience: "all",
      active: true,
      createdAt: ts,
      updatedAt: ts,
    })
  );
});
