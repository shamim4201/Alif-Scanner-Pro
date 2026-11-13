const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_uid_123";
const BOB_UID = "bob_uid_456";

const [emulatorHost, emulatorPortStr] = (process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085").split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

function safePastDate() {
  return new Date(Date.now() - 2000);
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

test("Unauthenticated user: cannot read or write user profiles", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  const ts = safePastDate();
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).get());
  await assertFails(
    unauthDb.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      displayName: "Alice",
      email: "alice@gmail.com",
      isPremium: false,
      createdAt: ts,
      updatedAt: ts,
    })
  );
});

test("Authenticated user: can create and read own Google user profile", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const ts = safePastDate();
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      displayName: "Alice Rahman",
      email: "alice@gmail.com",
      photoUrl: "https://example.com/alice.png",
      isPremium: false,
      createdAt: ts,
      updatedAt: ts,
    })
  );

  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).get());
});

test("Authenticated user: cannot read, create, or update another user's profile", async () => {
  const ts = safePastDate();
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await context.firestore().collection("users").doc(BOB_UID).set({
      userId: BOB_UID,
      displayName: "Bob",
      email: "bob@gmail.com",
      isPremium: false,
      createdAt: ts,
      updatedAt: ts,
    });
  });

  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertFails(aliceDb.collection("users").doc(BOB_UID).get());
  await assertFails(
    aliceDb.collection("users").doc(BOB_UID).set({
      userId: ALICE_UID,
      displayName: "Spoof",
      email: "spoof@gmail.com",
      isPremium: true,
      createdAt: ts,
      updatedAt: ts,
    })
  );
});

test("Authenticated user: cannot inject ghost fields or tamper with userId/createdAt", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const ts = safePastDate();

  // Ghost field on create
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      displayName: "Alice",
      email: "alice@gmail.com",
      isPremium: false,
      isAdmin: true,
      createdAt: ts,
      updatedAt: ts,
    })
  );

  // Valid create
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      displayName: "Alice",
      email: "alice@gmail.com",
      isPremium: false,
      createdAt: ts,
      updatedAt: ts,
    })
  );

  // Valid update
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).update({
      displayName: "Alice Updated",
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Tamper createdAt
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      createdAt: new Date(Date.now() - 500),
      updatedAt: new Date(Date.now() - 500),
    })
  );
});
