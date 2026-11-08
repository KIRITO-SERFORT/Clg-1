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

const [emulatorHost, emulatorPortStr] = (process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085").split(":");
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

test("Unauthenticated user: cannot read user profile", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).get());
});

test("Unauthenticated user: cannot write user profile", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).set({
    userId: ALICE_UID,
    name: "Alice",
    collegeName: "Tech Institute",
    batch: "2026",
    isPlus: false,
    createdAt: new Date(),
    updatedAt: new Date(),
  }));
});

test("Authenticated user: cannot read another user's profile", async () => {
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await context.firestore().collection("users").doc(BOB_UID).set({
      userId: BOB_UID,
      name: "Bob",
      collegeName: "Engineering College",
      batch: "2025",
      isPlus: true,
      createdAt: new Date(),
      updatedAt: new Date(),
    });
  });

  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertFails(aliceDb.collection("users").doc(BOB_UID).get());
});

test("Authenticated user: can create and read their own profile", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).set({
    userId: ALICE_UID,
    name: "Alice Wonderland",
    collegeName: "Global University",
    batch: "2027",
    isPlus: false,
    createdAt: new Date(),
    updatedAt: new Date(),
  }));

  const doc = await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).get());
  if (!doc.exists) throw new Error("Document should exist");
});

test("Authenticated user: cannot create profile with invalid schema", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertFails(aliceDb.collection("users").doc(ALICE_UID).set({
    userId: ALICE_UID,
    name: "", // Invalid empty name
    collegeName: "Global University",
    batch: "2027",
    isPlus: false,
    createdAt: new Date(),
    updatedAt: new Date(),
  }));
});

test("Authenticated user: can create and read academic records in subcollection", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).collection("academic_records").doc("sem1").set({
    userId: ALICE_UID,
    termName: "Semester 1",
    academicYear: "2026-27",
    averagePercentage: 85.5,
    sgpa: 8.7,
    createdAt: new Date(),
    updatedAt: new Date(),
  }));

  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).collection("academic_records").doc("sem1").get());
});

test("Cross-user isolation: Bob cannot read Alice's academic records", async () => {
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await context.firestore().collection("users").doc(ALICE_UID).collection("academic_records").doc("sem1").set({
      userId: ALICE_UID,
      termName: "Semester 1",
      academicYear: "2026-27",
      averagePercentage: 85.5,
      sgpa: 8.7,
      createdAt: new Date(),
      updatedAt: new Date(),
    });
  });

  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("academic_records").doc("sem1").get());
});
