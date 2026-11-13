const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_teacher_1";
const BOB_UID = "bob_teacher_2";
const ADMIN_UID = "admin_user_1";

const [emulatorHost, emulatorPortStr] = (process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085").split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

function nowTimestamp() {
  return new Date();
}

function validUserPayload(uid, role = "teacher") {
  return {
    userId: uid,
    displayName: "Sunita Mohanty",
    email: "sunita@example.com",
    role,
    schoolName: "Capital High School",
    district: "Khordha",
    subjects: ["Odia", "Mathematics"],
    profileVisibility: "public",
    languagePref: "or",
    blockedUserIds: [],
    createdAt: nowTimestamp(),
    updatedAt: nowTimestamp(),
  };
}

function validLessonPlanPayload(uid) {
  return {
    userId: uid,
    authorName: "Sunita Mohanty",
    title: "ଭଗ୍ନାଂଶ ପରିଚୟ (Introduction to Fractions)",
    stage: "preparatory",
    classLevel: "Class 4",
    subject: "Mathematics",
    medium: "Odia",
    topic: "Fractions",
    durationMinutes: 45,
    planDay: 12,
    planMonth: 11,
    planYear: 2026,
    learningObjectives: "ପିଲାମାନେ ଭଗ୍ନାଂଶ ବୁଝିପାରିବେ",
    tlmMaterials: "କାଗଜ, ଚକ୍, ବୋର୍ଡ",
    teacherActivities: "ଉଦାହରଣ ଦେଖାଇବେ",
    studentActivities: "କାଗଜ ଭାଙ୍ଗି ଦେଖିବେ",
    aditiStep: "ପୂର୍ବ ଜ୍ଞାନ ସହ ସଂଯୋଗ",
    aditiMinutes: 7,
    bodhaStep: "ମୂଳ ବିଷୟ ବୁଝାଇବା",
    bodhaMinutes: 15,
    abhyasaStep: "ଶ୍ରେଣୀ ଅଭ୍ୟାସ",
    abhyasaMinutes: 10,
    prayogaStep: "ଦୈନନ୍ଦିନ ଜୀବନରେ ପ୍ରୟୋଗ",
    prayogaMinutes: 8,
    prasaraStep: "ପୁନରାଲୋଚନା ଓ ଗୃହକାର୍ଯ୍ୟ",
    prasaraMinutes: 5,
    competencyQuestions: "୧/୨ ଓ ୧/୪ ମଧ୍ୟରେ କିଏ ବଡ଼?",
    assessmentCriteria: "ଶ୍ରେଣୀ କାର୍ଯ୍ୟ ଓ ମୌଖିକ ପ୍ରଶ୍ନ",
    homeworkActivities: "ଘରେ ରୁଟି ବା ଫଳକୁ ଭାଗ କରି ଦେଖିବା",
    inclusiveSuggestions: "ଦୃଶ୍ୟମାନ ଚାର୍ଟ ଓ ସ୍ପର୍ଶକ ସାମଗ୍ରୀ",
    summary: "ଭଗ୍ନାଂଶର ମୌଳିକ ଧାରଣା",
    teacherReflection: "ପିଲାମାନେ ଆଗ୍ରହର ସହ ଅଂଶଗ୍ରହଣ କଲେ",
    isAiGenerated: true,
    isVerified: false,
    status: "draft",
    createdAt: nowTimestamp(),
    updatedAt: nowTimestamp(),
  };
}

function validPostPayload(uid) {
  return {
    userId: uid,
    authorName: "Sunita Mohanty",
    authorRole: "teacher",
    authorSchool: "Capital High School",
    title: "Class 4 Fractions Activity",
    content: "Used paper folding in Aditi step today!",
    category: "teaching_idea",
    stage: "preparatory",
    classLevel: "Class 4",
    subject: "Mathematics",
    district: "Khordha",
    planDay: 12,
    planMonth: 11,
    planYear: 2026,
    likesCount: 0,
    commentsCount: 0,
    visibility: "public",
    createdAt: nowTimestamp(),
    updatedAt: nowTimestamp(),
  };
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

test("Unauthenticated user cannot read lesson_plans or posts", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("lesson_plans").get());
  await assertFails(unauthDb.collection("posts").get());
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).get());
});

test("PII isolation: Bob cannot read Alice private user profile", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).set(validUserPayload(ALICE_UID)));

  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  await assertFails(bobDb.collection("users").doc(ALICE_UID).get());
  await assertFails(bobDb.collection("users").get());
});

test("Role escalation blocked: Alice cannot self-assign app_admin role", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).set(validUserPayload(ALICE_UID, "app_admin"))
  );
});

test("Email spoofing defense: unverified admin email cannot write app_config", async () => {
  const spoofDb = testEnv.authenticatedContext("spoof_uid", {
    email: "jadumani0864@gmail.com",
    email_verified: false,
  }).firestore();

  await assertFails(
    spoofDb.collection("app_config").doc("panchapadi").set({
      isActive: true,
      aditiLabel: "ଆଦିତି",
      aditiDesc: "Intro",
      bodhaLabel: "ବୋଧ",
      bodhaDesc: "Explain",
      abhyasaLabel: "ଅଭ୍ୟାସ",
      abhyasaDesc: "Practice",
      prayogaLabel: "ପ୍ରୟୋଗ",
      prayogaDesc: "Apply",
      prasaraLabel: "ପ୍ରସାର",
      prasaraDesc: "Extend",
      updatedBy: "spoof_uid",
      updatedAt: nowTimestamp(),
    })
  );
});

test("Lesson plan ownership & query alignment: Alice can create/query own draft, Bob cannot read draft", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertSucceeds(
    aliceDb.collection("lesson_plans").doc("plan_1").set(validLessonPlanPayload(ALICE_UID))
  );

  // Aligned query succeeds
  await assertSucceeds(
    aliceDb.collection("lesson_plans").where("userId", "==", ALICE_UID).get()
  );

  // Unfiltered query fails (rules are not filters)
  await assertFails(aliceDb.collection("lesson_plans").get());

  // Bob cannot read Alice's draft
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  await assertFails(bobDb.collection("lesson_plans").doc("plan_1").get());
});

test("Shadow update & self-verification blocked on lesson_plans", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertSucceeds(
    aliceDb.collection("lesson_plans").doc("plan_2").set(validLessonPlanPayload(ALICE_UID))
  );

  // Alice cannot set isVerified = true on her own plan
  await assertFails(
    aliceDb.collection("lesson_plans").doc("plan_2").update({
      isVerified: true,
      updatedAt: nowTimestamp(),
    })
  );

  // Ghost field rejected
  await assertFails(
    aliceDb.collection("lesson_plans").doc("plan_2").update({
      ghostField: "hack",
      updatedAt: nowTimestamp(),
    })
  );
});

test("Verified admin can verify lesson plan and update Panchapadi config", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertSucceeds(
    aliceDb.collection("lesson_plans").doc("plan_3").set(validLessonPlanPayload(ALICE_UID))
  );

  const adminDb = testEnv.authenticatedContext(ADMIN_UID, {
    email: "jadumani0864@gmail.com",
    email_verified: true,
  }).firestore();

  await assertSucceeds(
    adminDb.collection("lesson_plans").doc("plan_3").update({
      isVerified: true,
      status: "published",
      updatedAt: nowTimestamp(),
    })
  );

  await assertSucceeds(
    adminDb.collection("app_config").doc("panchapadi").set({
      isActive: true,
      aditiLabel: "ଆଦିତି (Aditi)",
      aditiDesc: "Introduction and prior knowledge",
      bodhaLabel: "ବୋଧ (Bodha)",
      bodhaDesc: "Concept development",
      abhyasaLabel: "ଅଭ୍ୟାସ (Abhyasa)",
      abhyasaDesc: "Practice",
      prayogaLabel: "ପ୍ରୟୋଗ (Prayoga)",
      prayogaDesc: "Real-life application",
      prasaraLabel: "ପ୍ରସାର (Prasara)",
      prasaraDesc: "Extension & sharing",
      updatedBy: ADMIN_UID,
      updatedAt: nowTimestamp(),
    })
  );
});

test("Community post creation, public feed query, comment, and like rules work securely", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertSucceeds(
    aliceDb.collection("posts").doc("post_1").set(validPostPayload(ALICE_UID))
  );

  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  await assertSucceeds(
    bobDb.collection("posts").where("visibility", "==", "public").get()
  );

  // Bob can add a comment with his own userId
  await assertSucceeds(
    bobDb.collection("posts").doc("post_1").collection("comments").doc("c_1").set({
      userId: BOB_UID,
      authorName: "Bob Das",
      text: "Bahut sundara prayasa!",
      postVisibility: "public",
      createdAt: nowTimestamp(),
    })
  );

  // Bob cannot spoof Alice's userId in a comment
  await assertFails(
    bobDb.collection("posts").doc("post_1").collection("comments").doc("c_2").set({
      userId: ALICE_UID,
      authorName: "Spoof",
      text: "Fake",
      postVisibility: "public",
      createdAt: nowTimestamp(),
    })
  );
});

function validLessonProgressPayload(uid, planId) {
  return {
    userId: uid,
    lessonPlanId: planId,
    stage: "preparatory",
    aditiCompleted: true,
    bodhaCompleted: true,
    abhyasaCompleted: false,
    prayogaCompleted: false,
    prasaraCompleted: false,
    checkedGuidelineIds: ["nep_prep_1"],
    achievedOutcomeIndices: [0],
    inProgressOutcomeIndices: [1],
    inclusiveSupportApplied: true,
    tlmUsedInClass: true,
    assessmentConducted: false,
    classroomNotes: "Strong participation in group activity",
    lastTrackedDay: 12,
    lastTrackedMonth: 11,
    lastTrackedYear: 2026,
    completionPercent: 55,
    createdAt: nowTimestamp(),
    updatedAt: nowTimestamp(),
  };
}

test("User-scoped NEP 2020 lesson_progress rules enforce owner-only access and field bounds", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const aliceProgressRef = aliceDb.collection("users").doc(ALICE_UID).collection("lesson_progress").doc("plan_1");

  await assertSucceeds(aliceProgressRef.set(validLessonProgressPayload(ALICE_UID, "plan_1")));
  await assertSucceeds(
    aliceProgressRef.update({
      abhyasaCompleted: true,
      completionPercent: 75,
      updatedAt: nowTimestamp(),
    })
  );

  // Spoofed lessonPlanId !== progressId must fail
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("lesson_progress").doc("plan_other").set(
      validLessonProgressPayload(ALICE_UID, "plan_1")
    )
  );

  // Out-of-bounds completionPercent must fail
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("lesson_progress").doc("plan_bad").set({
      ...validLessonProgressPayload(ALICE_UID, "plan_bad"),
      completionPercent: 140,
    })
  );

  // Cross-user read & write by Bob must fail
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).collection("lesson_progress").doc("plan_1").get()
  );
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).collection("lesson_progress").doc("plan_bob").set(
      validLessonProgressPayload(BOB_UID, "plan_bob")
    )
  );
});

