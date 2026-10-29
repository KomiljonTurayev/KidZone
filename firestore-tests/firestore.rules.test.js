/**
 * Firebase Emulator-backed unit tests for ../firestore.rules.
 *
 * Run with `npm test` from this directory (needs firebase-tools, which downloads and
 * launches the Firestore emulator — no real project/credentials touched).
 */
const fs = require('fs');
const path = require('path');
const {
  initializeTestEnvironment,
  assertSucceeds,
  assertFails,
} = require('@firebase/rules-unit-testing');
const firebase = require('firebase/compat/app');
require('firebase/compat/firestore');

const PROJECT_ID = 'kidzone-rules-test';

const OWNER_UID = 'owner-uid';
const OTHER_UID = 'other-uid';
const CLAIM_ADMIN_UID = 'claim-admin-uid';
const EMAIL_ADMIN_UID = 'email-admin-uid';
const UNVERIFIED_EMAIL_ADMIN_UID = 'unverified-email-admin-uid';

let testEnv;

beforeAll(async () => {
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules: fs.readFileSync(path.resolve(__dirname, '../firestore.rules'), 'utf8'),
      host: '127.0.0.1',
      port: 8087,
    },
  });
});

afterEach(async () => {
  await testEnv.clearFirestore();
});

afterAll(async () => {
  await testEnv.cleanup();
});

// ---- Context helpers -------------------------------------------------------

function ownerCtx() {
  return testEnv.authenticatedContext(OWNER_UID);
}
function otherCtx() {
  return testEnv.authenticatedContext(OTHER_UID);
}
function claimAdminCtx() {
  return testEnv.authenticatedContext(CLAIM_ADMIN_UID, { admin: true });
}
function emailAdminCtx() {
  return testEnv.authenticatedContext(EMAIL_ADMIN_UID, {
    email: 'admin@kidzone.uz',
    email_verified: true,
  });
}
function unverifiedEmailAdminCtx() {
  return testEnv.authenticatedContext(UNVERIFIED_EMAIL_ADMIN_UID, {
    email: 'admin@kidzone.uz',
    email_verified: false,
  });
}
function anonCtx() {
  return testEnv.unauthenticatedContext();
}

/** Seeds data bypassing security rules entirely. */
async function seed(fn) {
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await fn(context.firestore());
  });
}

// ---- users/{uid} -------------------------------------------------------------

describe('users/{uid}', () => {
  test('owner can read their own user doc', async () => {
    await seed((db) => db.collection('users').doc(OWNER_UID).set({ status: 'active' }));
    await assertSucceeds(
      ownerCtx().firestore().collection('users').doc(OWNER_UID).get()
    );
  });

  test('another authenticated user cannot read someone else\'s user doc', async () => {
    await seed((db) => db.collection('users').doc(OWNER_UID).set({ status: 'active' }));
    await assertFails(
      otherCtx().firestore().collection('users').doc(OWNER_UID).get()
    );
  });

  test('unauthenticated user cannot read a user doc', async () => {
    await seed((db) => db.collection('users').doc(OWNER_UID).set({ status: 'active' }));
    await assertFails(
      anonCtx().firestore().collection('users').doc(OWNER_UID).get()
    );
  });

  test('claim-based admin can read any user doc', async () => {
    await seed((db) => db.collection('users').doc(OWNER_UID).set({ status: 'active' }));
    await assertSucceeds(
      claimAdminCtx().firestore().collection('users').doc(OWNER_UID).get()
    );
  });

  test('email-based admin (verified) can read any user doc', async () => {
    await seed((db) => db.collection('users').doc(OWNER_UID).set({ status: 'active' }));
    await assertSucceeds(
      emailAdminCtx().firestore().collection('users').doc(OWNER_UID).get()
    );
  });

  test('admin@kidzone.uz with email_verified=false is NOT treated as admin', async () => {
    await seed((db) => db.collection('users').doc(OWNER_UID).set({ status: 'active' }));
    await assertFails(
      unverifiedEmailAdminCtx().firestore().collection('users').doc(OWNER_UID).get()
    );
  });

  test('owner can create their own doc with no status field', async () => {
    await assertSucceeds(
      ownerCtx().firestore().collection('users').doc(OWNER_UID).set({ email: 'a@b.com' })
    );
  });

  test('owner can create their own doc with status="active"', async () => {
    await assertSucceeds(
      ownerCtx().firestore().collection('users').doc(OWNER_UID).set({ status: 'active' })
    );
  });

  test('owner CANNOT create their own doc pre-set to status="banned"', async () => {
    await assertFails(
      ownerCtx().firestore().collection('users').doc(OWNER_UID).set({ status: 'banned' })
    );
  });

  test('owner cannot create a doc for a different uid', async () => {
    await assertFails(
      ownerCtx().firestore().collection('users').doc(OTHER_UID).set({ status: 'active' })
    );
  });

  test('admin can create a doc with any status on behalf of a user', async () => {
    await assertSucceeds(
      claimAdminCtx().firestore().collection('users').doc(OWNER_UID).set({ status: 'banned' })
    );
  });

  test('owner can update their doc without touching status', async () => {
    await seed((db) => db.collection('users').doc(OWNER_UID).set({ status: 'active', displayName: 'Old' }));
    await assertSucceeds(
      ownerCtx().firestore().collection('users').doc(OWNER_UID)
        .set({ displayName: 'New' }, { merge: true })
    );
  });

  test('owner CANNOT self-unban by writing status=active over status=banned', async () => {
    await seed((db) => db.collection('users').doc(OWNER_UID).set({ status: 'banned' }));
    await assertFails(
      ownerCtx().firestore().collection('users').doc(OWNER_UID)
        .set({ status: 'active' }, { merge: true })
    );
  });

  test('owner re-sending the same unchanged status is allowed', async () => {
    await seed((db) => db.collection('users').doc(OWNER_UID).set({ status: 'active' }));
    await assertSucceeds(
      ownerCtx().firestore().collection('users').doc(OWNER_UID)
        .set({ status: 'active' }, { merge: true })
    );
  });

  test('admin can change a user\'s status (ban them)', async () => {
    await seed((db) => db.collection('users').doc(OWNER_UID).set({ status: 'active' }));
    await assertSucceeds(
      claimAdminCtx().firestore().collection('users').doc(OWNER_UID)
        .set({ status: 'banned' }, { merge: true })
    );
  });

  test('owner cannot delete their own user doc', async () => {
    await seed((db) => db.collection('users').doc(OWNER_UID).set({ status: 'active' }));
    await assertFails(
      ownerCtx().firestore().collection('users').doc(OWNER_UID).delete()
    );
  });

  test('admin can delete a user doc', async () => {
    await seed((db) => db.collection('users').doc(OWNER_UID).set({ status: 'active' }));
    await assertSucceeds(
      claimAdminCtx().firestore().collection('users').doc(OWNER_UID).delete()
    );
  });
});

// ---- users/{uid}/profiles/{profileId} ---------------------------------------

describe('users/{uid}/profiles/{profileId}', () => {
  const profilePath = () =>
    `users/${OWNER_UID}/profiles/p1`;

  test('owner can read/write their own profile', async () => {
    await assertSucceeds(
      ownerCtx().firestore().doc(profilePath()).set({ name: 'Child' })
    );
    await assertSucceeds(ownerCtx().firestore().doc(profilePath()).get());
  });

  test('another user cannot read or write someone else\'s profile', async () => {
    await seed((db) => db.doc(profilePath()).set({ name: 'Child' }));
    await assertFails(otherCtx().firestore().doc(profilePath()).get());
    await assertFails(otherCtx().firestore().doc(profilePath()).set({ name: 'Hacked' }));
  });

  test('admin can read/write any profile', async () => {
    await seed((db) => db.doc(profilePath()).set({ name: 'Child' }));
    await assertSucceeds(claimAdminCtx().firestore().doc(profilePath()).get());
  });

  test('owner can set timeLimitMinutes within range', async () => {
    await assertSucceeds(
      ownerCtx().firestore().doc(profilePath()).set({ name: 'Child', timeLimitMinutes: 30 })
    );
  });

  test('owner CANNOT set a negative timeLimitMinutes', async () => {
    await assertFails(
      ownerCtx().firestore().doc(profilePath()).set({ name: 'Child', timeLimitMinutes: -5 })
    );
  });

  test('owner CANNOT set timeLimitMinutes above one day (1440)', async () => {
    await assertFails(
      ownerCtx().firestore().doc(profilePath()).set({ name: 'Child', timeLimitMinutes: 999999 })
    );
  });

  test('owner CANNOT set timeLimitMinutes to a non-integer type', async () => {
    await assertFails(
      ownerCtx().firestore().doc(profilePath()).set({ name: 'Child', timeLimitMinutes: '30' })
    );
  });

  test('owner CANNOT set an empty name', async () => {
    await assertFails(
      ownerCtx().firestore().doc(profilePath()).set({ name: '' })
    );
  });

  test('owner CANNOT set an unsupported language', async () => {
    await assertFails(
      ownerCtx().firestore().doc(profilePath()).set({ name: 'Child', language: 'fr' })
    );
  });

  test('owner can set a supported language', async () => {
    await assertSucceeds(
      ownerCtx().firestore().doc(profilePath()).set({ name: 'Child', language: 'ru' })
    );
  });

  test('owner CANNOT write a field outside the known profile schema', async () => {
    await assertFails(
      ownerCtx().firestore().doc(profilePath()).set({ name: 'Child', role: 'admin' })
    );
  });

  test('owner can merge-write a valid streak map', async () => {
    await seed((db) => db.doc(profilePath()).set({ name: 'Child' }));
    await assertSucceeds(
      ownerCtx().firestore().doc(profilePath())
        .set({ streak: { count: 3, lastCompletedDate: '2026-09-06' } }, { merge: true })
    );
  });

  test('owner CANNOT write a streak with a non-integer count', async () => {
    await seed((db) => db.doc(profilePath()).set({ name: 'Child' }));
    await assertFails(
      ownerCtx().firestore().doc(profilePath())
        .set({ streak: { count: 'three', lastCompletedDate: '2026-09-06' } }, { merge: true })
    );
  });

  test('owner can write the Faza 16 sync fields as the app does', async () => {
    await assertSucceeds(
      ownerCtx().firestore().doc(profilePath()).set({
        name: 'Child',
        createdAt: 1000,
        updatedAt: 2000,
        deleted: false,
        serverUpdatedAt: firebase.firestore.FieldValue.serverTimestamp(),
      }, { merge: true })
    );
  });

  test('owner can write a tombstone', async () => {
    await seed((db) => db.doc(profilePath()).set({ name: 'Child' }));
    await assertSucceeds(
      ownerCtx().firestore().doc(profilePath()).set({ deleted: true, updatedAt: 3000 }, { merge: true })
    );
  });

  test('owner CANNOT write updatedAt as a non-integer', async () => {
    await assertFails(
      ownerCtx().firestore().doc(profilePath()).set({ name: 'Child', updatedAt: '2000' })
    );
  });

  test('owner CANNOT write deleted as a non-boolean', async () => {
    await assertFails(
      ownerCtx().firestore().doc(profilePath()).set({ name: 'Child', deleted: 'yes' })
    );
  });

  test('owner CANNOT forge serverUpdatedAt with a client-chosen time', async () => {
    await assertFails(
      ownerCtx().firestore().doc(profilePath()).set({
        name: 'Child',
        serverUpdatedAt: firebase.firestore.Timestamp.fromMillis(0),
      })
    );
  });

  test('another user CANNOT write a tombstone into a profile they do not own', async () => {
    await seed((db) => db.doc(profilePath()).set({ name: 'Child' }));
    await assertFails(
      otherCtx().firestore().doc(profilePath()).set({ deleted: true, updatedAt: 3000 }, { merge: true })
    );
  });

  test('admin can set an out-of-range timeLimitMinutes (admin override, not schema-restricted)', async () => {
    await assertSucceeds(
      claimAdminCtx().firestore().doc(profilePath()).set({ name: 'Child', timeLimitMinutes: 999999 })
    );
  });
});

// ---- users/{uid}/profiles/{profileId}/playtime/{date} -----------------------

describe('users/{uid}/profiles/{profileId}/playtime/{date}', () => {
  const playtimePath = () =>
    `users/${OWNER_UID}/profiles/p1/playtime/2026-09-06`;

  test('owner can read their own playtime counter', async () => {
    await seed((db) => db.doc(playtimePath()).set({ elapsedSeconds: 120 }));
    await assertSucceeds(ownerCtx().firestore().doc(playtimePath()).get());
  });

  test('owner CANNOT write their own playtime counter (server-authoritative only)', async () => {
    await assertFails(
      ownerCtx().firestore().doc(playtimePath()).set({ elapsedSeconds: 0 })
    );
  });

  test('another user cannot read someone else\'s playtime counter', async () => {
    await seed((db) => db.doc(playtimePath()).set({ elapsedSeconds: 120 }));
    await assertFails(otherCtx().firestore().doc(playtimePath()).get());
  });

  test('admin (acting as the backend) can write the playtime counter', async () => {
    await assertSucceeds(
      claimAdminCtx().firestore().doc(playtimePath()).set({ elapsedSeconds: 600 })
    );
  });
});

// ---- profiles/{profileId}/{daily_challenges,stats}/{date} -------------------

describe('profiles/{profileId} sync subcollections (daily_challenges, stats)', () => {
  test('owner can read/write a nested profile subcollection doc (daily_challenges)', async () => {
    const p = `users/${OWNER_UID}/profiles/p1/daily_challenges/2026-09-06`;
    await assertSucceeds(
      ownerCtx().firestore().doc(p).set({ gameId: 'memory', completed: true })
    );
    await assertSucceeds(ownerCtx().firestore().doc(p).get());
  });

  test('owner can read/write a nested profile subcollection doc (stats)', async () => {
    const p = `users/${OWNER_UID}/profiles/p1/stats/2026-09-06`;
    await assertSucceeds(
      ownerCtx().firestore().doc(p).set({ minutesPlayed: 10 })
    );
  });

  test('another user cannot write into someone else\'s nested subcollection', async () => {
    const p = `users/${OWNER_UID}/profiles/p1/daily_challenges/2026-09-06`;
    await assertFails(
      otherCtx().firestore().doc(p).set({ gameId: 'memory', completed: true })
    );
  });

  test('owner CANNOT write a daily_challenges field outside the known schema', async () => {
    const p = `users/${OWNER_UID}/profiles/p1/daily_challenges/2026-09-06`;
    await assertFails(
      ownerCtx().firestore().doc(p).set({ gameId: 'memory', completed: true, bonus: 'unlocked' })
    );
  });

  test('owner CANNOT set daily_challenges.completed to a non-boolean', async () => {
    const p = `users/${OWNER_UID}/profiles/p1/daily_challenges/2026-09-06`;
    await assertFails(
      ownerCtx().firestore().doc(p).set({ gameId: 'memory', completed: 'yes' })
    );
  });

  test('owner CANNOT set a negative minutesPlayed', async () => {
    const p = `users/${OWNER_UID}/profiles/p1/stats/2026-09-06`;
    await assertFails(
      ownerCtx().firestore().doc(p).set({ minutesPlayed: -1 })
    );
  });

  test('owner CANNOT set minutesPlayed above one day (1440)', async () => {
    const p = `users/${OWNER_UID}/profiles/p1/stats/2026-09-06`;
    await assertFails(
      ownerCtx().firestore().doc(p).set({ minutesPlayed: 99999 })
    );
  });

  test('owner CANNOT write a stats field outside the known schema', async () => {
    const p = `users/${OWNER_UID}/profiles/p1/stats/2026-09-06`;
    await assertFails(
      ownerCtx().firestore().doc(p).set({ minutesPlayed: 10, hacked: true })
    );
  });

  test('owner can write both known stats fields together', async () => {
    const p = `users/${OWNER_UID}/profiles/p1/stats/2026-09-06`;
    await assertSucceeds(
      ownerCtx().firestore().doc(p).set({ minutesPlayed: 10, gamesPlayed: 3 })
    );
  });
});

// ---- config/{doc} -------------------------------------------------------------

describe('config/{doc}', () => {
  test('any authenticated user can read config', async () => {
    await seed((db) => db.collection('config').doc('banner').set({ active: true }));
    await assertSucceeds(ownerCtx().firestore().collection('config').doc('banner').get());
  });

  test('unauthenticated user cannot read config', async () => {
    await seed((db) => db.collection('config').doc('banner').set({ active: true }));
    await assertFails(anonCtx().firestore().collection('config').doc('banner').get());
  });

  test('non-admin cannot write config', async () => {
    await assertFails(
      ownerCtx().firestore().collection('config').doc('banner').set({ active: true })
    );
  });

  test('admin can write config', async () => {
    await assertSucceeds(
      claimAdminCtx().firestore().collection('config').doc('banner').set({ active: true })
    );
  });
});

// ---- banners/{bannerId} & categories/{categoryId} ----------------------------

describe('banners/{bannerId} and categories/{categoryId}', () => {
  test('banners are publicly readable, even unauthenticated', async () => {
    await seed((db) => db.collection('banners').doc('b1').set({ title: 'Promo' }));
    await assertSucceeds(anonCtx().firestore().collection('banners').doc('b1').get());
  });

  test('categories are publicly readable, even unauthenticated', async () => {
    await seed((db) => db.collection('categories').doc('c1').set({ name: 'Animals' }));
    await assertSucceeds(anonCtx().firestore().collection('categories').doc('c1').get());
  });

  test('non-admin cannot write a banner', async () => {
    await assertFails(
      ownerCtx().firestore().collection('banners').doc('b1').set({ title: 'Hacked' })
    );
  });

  test('admin can write a banner', async () => {
    await assertSucceeds(
      claimAdminCtx().firestore().collection('banners').doc('b1').set({ title: 'Promo' })
    );
  });
});

// ---- stats/{dateKey} (global daily aggregate) --------------------------------

describe('stats/{dateKey}', () => {
  const VALID_KEYS_DOC = {
    dau: 1,
    totalMinutes: 5,
    totalSessions: 1,
    gameBreakdown: {},
  };

  test('authenticated user can create a valid daily aggregate doc', async () => {
    await assertSucceeds(
      ownerCtx().firestore().collection('stats').doc('2026-09-06').set(VALID_KEYS_DOC)
    );
  });

  test('unauthenticated user cannot create a daily aggregate doc', async () => {
    await assertFails(
      anonCtx().firestore().collection('stats').doc('2026-09-06').set(VALID_KEYS_DOC)
    );
  });

  test('create is rejected if it includes a field outside the allowed set', async () => {
    await assertFails(
      ownerCtx().firestore().collection('stats').doc('2026-09-06')
        .set({ ...VALID_KEYS_DOC, extraField: 'not allowed' })
    );
  });

  test('create is rejected when the document id is not a YYYY-MM-DD date', async () => {
    await assertFails(
      ownerCtx().firestore().collection('stats').doc('not-a-date').set(VALID_KEYS_DOC)
    );
  });

  test('non-admin cannot read the daily aggregate doc', async () => {
    await seed((db) => db.collection('stats').doc('2026-09-06').set(VALID_KEYS_DOC));
    await assertFails(
      ownerCtx().firestore().collection('stats').doc('2026-09-06').get()
    );
  });

  test('admin can read the daily aggregate doc', async () => {
    await seed((db) => db.collection('stats').doc('2026-09-06').set(VALID_KEYS_DOC));
    await assertSucceeds(
      claimAdminCtx().firestore().collection('stats').doc('2026-09-06').get()
    );
  });

  test('non-admin cannot delete the daily aggregate doc', async () => {
    await seed((db) => db.collection('stats').doc('2026-09-06').set(VALID_KEYS_DOC));
    await assertFails(
      ownerCtx().firestore().collection('stats').doc('2026-09-06').delete()
    );
  });

  test('admin can delete the daily aggregate doc', async () => {
    await seed((db) => db.collection('stats').doc('2026-09-06').set(VALID_KEYS_DOC));
    await assertSucceeds(
      claimAdminCtx().firestore().collection('stats').doc('2026-09-06').delete()
    );
  });
});

// ---- streak_reminder_runs/{runId} --------------------------------------------

describe('streak_reminder_runs/{runId}', () => {
  test('non-admin cannot read or write run logs', async () => {
    await assertFails(
      ownerCtx().firestore().collection('streak_reminder_runs').doc('r1').set({ ranAt: 1 })
    );
    await seed((db) => db.collection('streak_reminder_runs').doc('r1').set({ ranAt: 1 }));
    await assertFails(
      ownerCtx().firestore().collection('streak_reminder_runs').doc('r1').get()
    );
  });

  test('admin can read and write run logs', async () => {
    await assertSucceeds(
      claimAdminCtx().firestore().collection('streak_reminder_runs').doc('r1').set({ ranAt: 1 })
    );
    await assertSucceeds(
      claimAdminCtx().firestore().collection('streak_reminder_runs').doc('r1').get()
    );
  });
});

describe('telegram_logins/{nonce}', () => {
  // Login sessions are written and redeemed only by the backend (Admin SDK). A client that
  // could read one could steal another device's pending Telegram sign-in.
  test('no client can read or write Telegram login sessions, not even admins', async () => {
    await seed((db) => db.collection('telegram_logins').doc('n1').set({ requesterUid: OWNER_UID, status: 'CONFIRMED' }));
    for (const ctx of [ownerCtx(), otherCtx(), claimAdminCtx(), testEnv.unauthenticatedContext()]) {
      const db = ctx.firestore();
      await assertFails(db.collection('telegram_logins').doc('n1').get());
      await assertFails(db.collection('telegram_logins').doc('n2').set({ status: 'PENDING' }));
    }
  });
});

// ---- Faza 16: family members --------------------------------------------------

describe('family members (second parent)', () => {
  const profile = `users/${OWNER_UID}/profiles/p1`;
  const joinFamily = () => seed((db) => db.doc(`families/${OWNER_UID}/members/${OTHER_UID}`).set({ joinedAt: 1 }));

  test('a member can read and edit the owner\u2019s profiles', async () => {
    await seed((db) => db.doc(profile).set({ name: 'Child' }));
    await joinFamily();
    await assertSucceeds(otherCtx().firestore().doc(profile).get());
    await assertSucceeds(otherCtx().firestore().doc(profile).set({ timeLimitMinutes: 45, updatedAt: 5 }, { merge: true }));
  });

  test('a member\u2019s profile writes are still schema-checked', async () => {
    await joinFamily();
    await assertFails(otherCtx().firestore().doc(profile).set({ name: 'Child', role: 'admin' }));
  });

  test('a member can use the profile\u2019s stats and challenges, and read play time', async () => {
    await joinFamily();
    await assertSucceeds(otherCtx().firestore().doc(`${profile}/stats/2026-10-29`).set({ minutesPlayed: 10 }));
    await assertSucceeds(otherCtx().firestore().doc(`${profile}/daily_challenges/2026-10-29`).set({ completed: true }));
    await assertSucceeds(otherCtx().firestore().doc(`${profile}/playtime/2026-10-29`).get());
    await assertFails(otherCtx().firestore().doc(`${profile}/playtime/2026-10-29`).set({ elapsedSeconds: 0 }));
  });

  test('a non-member cannot read the owner\u2019s profiles', async () => {
    await seed((db) => db.doc(profile).set({ name: 'Child' }));
    await assertFails(otherCtx().firestore().doc(profile).get());
  });

  test('a member cannot read or change the owner\u2019s user document', async () => {
    await seed((db) => db.doc(`users/${OWNER_UID}`).set({ status: 'active' }));
    await joinFamily();
    await assertFails(otherCtx().firestore().doc(`users/${OWNER_UID}`).get());
    await assertFails(otherCtx().firestore().doc(`users/${OWNER_UID}`).set({ status: 'active', note: 'x' }, { merge: true }));
  });

  test('nobody can forge a familyOwner link on their own user document', async () => {
    await assertFails(otherCtx().firestore().doc(`users/${OTHER_UID}`).set({ familyOwner: OWNER_UID }));
    await seed((db) => db.doc(`users/${OTHER_UID}`).set({ status: 'active' }));
    await assertFails(otherCtx().firestore().doc(`users/${OTHER_UID}`).set({ familyOwner: OWNER_UID }, { merge: true }));
  });

  test('a member can still update their own user document once linked by the backend', async () => {
    await seed((db) => db.doc(`users/${OTHER_UID}`).set({ status: 'active', familyOwner: OWNER_UID }));
    await assertSucceeds(otherCtx().firestore().doc(`users/${OTHER_UID}`).set({ fcmToken: 't' }, { merge: true }));
  });

  test('clients cannot write membership or invites', async () => {
    await assertFails(otherCtx().firestore().doc(`families/${OWNER_UID}/members/${OTHER_UID}`).set({ joinedAt: 1 }));
    await assertFails(ownerCtx().firestore().doc(`families/${OWNER_UID}/members/${OTHER_UID}`).set({ joinedAt: 1 }));
    await assertFails(ownerCtx().firestore().doc('family_invites/123456').set({ ownerUid: OWNER_UID }));
    await assertFails(otherCtx().firestore().doc('family_invites/123456').get());
  });
});

// ---- Faza 17: backend-only Premium copy and AI allowance ---------------------
// Written only by the backend (Admin SDK). No rule matches these paths, so clients are
// denied by default; a client that could write them would give itself Premium or reset
// its daily AI counter.

describe('entitlements/{uid} and users/{uid}/ai_usage/{date}', () => {
  test('an owner can neither read nor write their own Premium copy', async () => {
    await seed((db) => db.doc(`entitlements/${OWNER_UID}`).set({ premiumUntil: 1, eventAtMs: 1 }));

    await assertFails(ownerCtx().firestore().doc(`entitlements/${OWNER_UID}`).get());
    await assertFails(ownerCtx().firestore().doc(`entitlements/${OWNER_UID}`).set({ premiumUntil: 9e15 }));
    await assertFails(ownerCtx().firestore().doc(`entitlements/${OWNER_UID}`).delete());
  });

  test('an owner can neither read nor reset their AI allowance', async () => {
    const usage = `users/${OWNER_UID}/ai_usage/2026-10-29`;
    await seed((db) => db.doc(usage).set({ stories: 1, questions: 10 }));

    await assertFails(ownerCtx().firestore().doc(usage).get());
    await assertFails(ownerCtx().firestore().doc(usage).set({ stories: 0 }));
    await assertFails(ownerCtx().firestore().doc(usage).delete());
  });

  test('a family member cannot touch the owner\'s allowance either', async () => {
    await seed((db) => db.doc(`families/${OWNER_UID}/members/${OTHER_UID}`).set({ joinedAt: 1 }));

    await assertFails(otherCtx().firestore().doc(`users/${OWNER_UID}/ai_usage/2026-10-29`).set({ stories: 0 }));
    await assertFails(otherCtx().firestore().doc(`entitlements/${OWNER_UID}`).get());
  });
});

// ---- Faza 18: best level per game -------------------------------------------

describe('profiles/{profileId}/progress/{gameId}', () => {
  const p = `users/${OWNER_UID}/profiles/p1/progress/animals`;
  const now = () => firebase.firestore.FieldValue.serverTimestamp();

  test('the owner can record and read a level', async () => {
    await assertSucceeds(ownerCtx().firestore().doc(p).set({ level: 21, updatedAt: now() }));
    await assertSucceeds(ownerCtx().firestore().doc(p).get());
  });

  test('a family member can record a level and read it', async () => {
    await seed((db) => db.doc(`families/${OWNER_UID}/members/${OTHER_UID}`).set({ joinedAt: 1 }));
    await assertSucceeds(otherCtx().firestore().doc(p).set({ level: 3 }));
    await assertSucceeds(otherCtx().firestore().doc(p).get());
  });

  test('someone outside the family cannot read or write it', async () => {
    await seed((db) => db.doc(p).set({ level: 5 }));
    await assertFails(otherCtx().firestore().doc(p).get());
    await assertFails(otherCtx().firestore().doc(p).set({ level: 6 }));
  });

  test('a level never goes down', async () => {
    await seed((db) => db.doc(p).set({ level: 30 }));
    await assertFails(ownerCtx().firestore().doc(p).set({ level: 12 }, { merge: true }));
    await assertSucceeds(ownerCtx().firestore().doc(p).set({ level: 31 }, { merge: true }));
  });

  test('levels outside 1..10000, non-integers and extra fields are refused', async () => {
    await assertFails(ownerCtx().firestore().doc(p).set({ level: 0 }));
    await assertFails(ownerCtx().firestore().doc(p).set({ level: 10001 }));
    await assertFails(ownerCtx().firestore().doc(p).set({ level: '5' }));
    await assertFails(ownerCtx().firestore().doc(p).set({ level: 5, stars: 999 }));
  });
});
