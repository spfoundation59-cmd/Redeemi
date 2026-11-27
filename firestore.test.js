const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ADMIN_UID = "admin_spfoundation";
const ADMIN_EMAIL = "spfoundation59@gmail.com";
const ALICE_UID = "alice_123";
const BOB_UID = "bob_456";

const [emulatorHost, emulatorPortStr] = (
  process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085"
).split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

function adminContext() {
  return testEnv.authenticatedContext(ADMIN_UID, {
    email: ADMIN_EMAIL,
    email_verified: true,
  });
}

function spoofedAdminContext() {
  return testEnv.authenticatedContext("spoof_uid", {
    email: ADMIN_EMAIL,
    email_verified: false,
  });
}

function customerContext(uid, email = `${uid}@example.com`) {
  return testEnv.authenticatedContext(uid, {
    email,
    email_verified: true,
  });
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

test("Guest user: can browse public storefront catalog, denied across all private collections & writes", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertSucceeds(unauthDb.collection("products").get());
  await assertSucceeds(unauthDb.collection("platformConfig").doc("settings").get());
  await assertFails(unauthDb.collection("redeemCodes").get());
  await assertFails(unauthDb.collection("codeSlots").get());
  await assertFails(unauthDb.collection("orders").get());
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).get());
  await assertFails(unauthDb.collection("admins").get());
  await assertFails(
    unauthDb.collection("products").doc("hack_prod").set({
      title: "Fake",
      priceInr: 1,
    })
  );
});

test("Email spoofing: unverified admin email cannot create products or read codes", async () => {
  const spoofDb = spoofedAdminContext().firestore();
  const past = new Date(Date.now() - 2000);
  await assertFails(
    spoofDb.collection("products").doc("prod_gplay_500").set({
      title: "Google Play ₹500",
      brand: "Google Play",
      category: "Gift Cards",
      description: "Official Google Play digital gift code for India.",
      redemptionInstructions: "Open Play Store -> Payments -> Redeem code.",
      priceInr: 490,
      mrpInr: 500,
      stockCount: 1,
      active: true,
      featured: true,
      accentStyle: "purple",
      createdAt: past,
      updatedAt: past,
    })
  );
  await assertFails(spoofDb.collection("redeemCodes").get());
});

test("Admin: can configure platform rules, create products, and upload redeem codes + slots", async () => {
  const adminDb = adminContext().firestore();
  const past = new Date(Date.now() - 2000);

  await assertSucceeds(
    adminDb.collection("platformConfig").doc("settings").set({
      platformName: "REDEEMI",
      tagline: "Buy. Redeem. Enjoy.",
      currency: "INR",
      platformFeePercent: 2,
      gstPercent: 0,
      maxCartQuantityPerItem: 5,
      maintenanceMode: false,
      announcementBanner: "Instant Digital Delivery • Verified INR Vouchers",
      enabledPaymentMethods: ["UPI", "CARD", "NETBANKING", "WALLET"],
      supportEmail: "support@redeemi.in",
      updatedAt: past,
    })
  );

  await assertSucceeds(
    adminDb.collection("products").doc("prod_gplay_500").set({
      title: "Google Play ₹500",
      brand: "Google Play",
      category: "Gift Cards",
      description: "Instant Google Play Store credit voucher for apps, games, and movies.",
      redemptionInstructions: "Open Google Play -> Profile -> Payments & subscriptions -> Redeem code.",
      priceInr: 490,
      mrpInr: 500,
      stockCount: 1,
      active: true,
      featured: true,
      accentStyle: "purple",
      createdAt: past,
      updatedAt: past,
    })
  );

  await assertSucceeds(
    adminDb.collection("codeSlots").doc("slot_1").set({
      productId: "prod_gplay_500",
      status: "available",
      createdAt: past,
      updatedAt: past,
    })
  );

  await assertSucceeds(
    adminDb.collection("redeemCodes").doc("slot_1").set({
      productId: "prod_gplay_500",
      productTitle: "Google Play ₹500",
      code: "RDM-GPLAY-9X4K-2P8M",
      status: "available",
      createdAt: past,
      updatedAt: past,
    })
  );
});

test("Customer isolation: cannot escalate role, change prices, or view unsold redeem codes", async () => {
  const past = new Date(Date.now() - 5000);
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    const db = ctx.firestore();
    await db.collection("products").doc("prod_steam_1000").set({
      title: "Steam Wallet ₹1000",
      brand: "Steam",
      category: "Gaming",
      description: "Digital Steam Wallet funds for PC games.",
      redemptionInstructions: "Steam -> Account Details -> Add funds -> Redeem Steam Wallet Code.",
      priceInr: 999,
      mrpInr: 1000,
      stockCount: 1,
      active: true,
      featured: true,
      accentStyle: "blue",
      createdAt: past,
      updatedAt: past,
    });
    await db.collection("codeSlots").doc("slot_s1").set({
      productId: "prod_steam_1000",
      status: "available",
      createdAt: past,
      updatedAt: past,
    });
    await db.collection("redeemCodes").doc("slot_s1").set({
      productId: "prod_steam_1000",
      productTitle: "Steam Wallet ₹1000",
      code: "STM-SECRET-9988-7766",
      status: "available",
      createdAt: past,
      updatedAt: past,
    });
  });

  const aliceDb = customerContext(ALICE_UID).firestore();

  // Cannot self-assign admin role
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      email: "alice@example.com",
      displayName: "Alice",
      role: "admin",
      blocked: false,
      totalOrders: 0,
      totalSpentInr: 0,
      createdAt: past,
      updatedAt: past,
    })
  );

  // Valid customer profile creation succeeds
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      email: "alice@example.com",
      displayName: "Alice",
      role: "customer",
      blocked: false,
      totalOrders: 0,
      totalSpentInr: 0,
      createdAt: past,
      updatedAt: past,
    })
  );

  // Cannot change product price
  await assertFails(
    aliceDb.collection("products").doc("prod_steam_1000").update({
      priceInr: 1,
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Cannot read or list unsold redeem codes
  await assertFails(aliceDb.collection("redeemCodes").get());
  await assertFails(aliceDb.collection("redeemCodes").doc("slot_s1").get());
  await assertFails(
    aliceDb.collection("redeemCodes").where("status", "==", "available").get()
  );

  // Cannot modify platformConfig
  await assertFails(
    aliceDb.collection("platformConfig").doc("settings").set({
      platformName: "HACKED",
      tagline: "X",
      currency: "INR",
      platformFeePercent: 0,
      gstPercent: 0,
      maxCartQuantityPerItem: 5,
      maintenanceMode: false,
      announcementBanner: "",
      enabledPaymentMethods: ["UPI"],
      supportEmail: "x@x.com",
      updatedAt: past,
    })
  );
});

test("Atomic Checkout Batch: valid paid order claims code & reveals ONLY to buyer; price tamper fails", async () => {
  const past = new Date(Date.now() - 10000);
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    const db = ctx.firestore();
    await db.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      email: "alice@example.com",
      displayName: "Alice",
      role: "customer",
      blocked: false,
      totalOrders: 0,
      totalSpentInr: 0,
      createdAt: past,
      updatedAt: past,
    });
    await db.collection("products").doc("prod_steam_1000").set({
      title: "Steam Wallet ₹1000",
      brand: "Steam",
      category: "Gaming",
      description: "Digital Steam Wallet funds for PC games.",
      redemptionInstructions: "Steam -> Redeem Steam Wallet Code.",
      priceInr: 999,
      mrpInr: 1000,
      stockCount: 2,
      active: true,
      featured: true,
      accentStyle: "blue",
      createdAt: past,
      updatedAt: past,
    });
    await db.collection("codeSlots").doc("slot_s1").set({
      productId: "prod_steam_1000",
      status: "available",
      createdAt: past,
      updatedAt: past,
    });
    await db.collection("redeemCodes").doc("slot_s1").set({
      productId: "prod_steam_1000",
      productTitle: "Steam Wallet ₹1000",
      code: "STM-SECRET-9988-7766",
      status: "available",
      createdAt: past,
      updatedAt: past,
    });
    await db.collection("users").doc(ALICE_UID).collection("cart").doc("prod_steam_1000").set({
      productId: "prod_steam_1000",
      quantity: 1,
      createdAt: past,
      updatedAt: past,
    });
  });

  const aliceDb = customerContext(ALICE_UID).firestore();
  const bobDb = customerContext(BOB_UID).firestore();
  const now = new Date(Date.now() - 1000);

  // 1. Tampered price batch MUST FAIL
  const badBatch = aliceDb.batch();
  badBatch.set(aliceDb.collection("orders").doc("order_bad"), {
    userId: ALICE_UID,
    customerEmail: "alice@example.com",
    customerName: "Alice",
    status: "paid",
    paymentMethod: "UPI",
    paymentReference: "RDM-PAY-0001",
    subtotalInr: 1,
    platformFeeInr: 0,
    totalInr: 1,
    itemCount: 1,
    productSummary: "Steam Wallet ₹1000 × 1",
    createdAt: now,
    updatedAt: now,
  });
  badBatch.set(aliceDb.collection("orders").doc("order_bad").collection("items").doc("prod_steam_1000"), {
    userId: ALICE_UID,
    orderId: "order_bad",
    productId: "prod_steam_1000",
    productTitle: "Steam Wallet ₹1000",
    brand: "Steam",
    quantity: 1,
    unitPriceInr: 1, // tampered!
    lineTotalInr: 1,
    slotIds: ["slot_s1"],
    createdAt: now,
  });
  await assertFails(badBatch.commit());

  // 2. Valid atomic checkout batch MUST SUCCEED
  const goodBatch = aliceDb.batch();
  goodBatch.set(aliceDb.collection("orders").doc("order_good"), {
    userId: ALICE_UID,
    customerEmail: "alice@example.com",
    customerName: "Alice",
    status: "paid",
    paymentMethod: "UPI",
    paymentReference: "RDM-PAY-1001",
    subtotalInr: 999,
    platformFeeInr: 20,
    totalInr: 1019,
    itemCount: 1,
    productSummary: "Steam Wallet ₹1000 × 1",
    createdAt: now,
    updatedAt: now,
  });
  goodBatch.set(aliceDb.collection("orders").doc("order_good").collection("items").doc("prod_steam_1000"), {
    userId: ALICE_UID,
    orderId: "order_good",
    productId: "prod_steam_1000",
    productTitle: "Steam Wallet ₹1000",
    brand: "Steam",
    quantity: 1,
    unitPriceInr: 999,
    lineTotalInr: 999,
    slotIds: ["slot_s1"],
    createdAt: now,
  });
  goodBatch.update(aliceDb.collection("codeSlots").doc("slot_s1"), {
    status: "sold",
    claimedBy: ALICE_UID,
    orderId: "order_good",
    updatedAt: now,
  });
  goodBatch.update(aliceDb.collection("redeemCodes").doc("slot_s1"), {
    status: "sold",
    assignedToUserId: ALICE_UID,
    orderId: "order_good",
    updatedAt: now,
  });
  goodBatch.update(aliceDb.collection("products").doc("prod_steam_1000"), {
    stockCount: 1,
    updatedAt: now,
  });
  goodBatch.update(aliceDb.collection("users").doc(ALICE_UID), {
    totalOrders: 1,
    totalSpentInr: 1019,
    updatedAt: now,
  });
  goodBatch.delete(aliceDb.collection("users").doc(ALICE_UID).collection("cart").doc("prod_steam_1000"));
  await assertSucceeds(goodBatch.commit());

  // Alice can read her own order, order items, and assigned code
  await assertSucceeds(aliceDb.collection("orders").where("userId", "==", ALICE_UID).get());
  await assertSucceeds(
    aliceDb.collection("orders").doc("order_good").collection("items").where("userId", "==", ALICE_UID).get()
  );
  await assertSucceeds(
    aliceDb.collection("redeemCodes").where("assignedToUserId", "==", ALICE_UID).get()
  );

  // Bob CANNOT read Alice's order, order items, or assigned code; unfiltered orders list fails
  await assertFails(bobDb.collection("orders").doc("order_good").get());
  await assertFails(bobDb.collection("orders").get());
  await assertFails(bobDb.collection("redeemCodes").doc("slot_s1").get());
  await assertFails(
    bobDb.collection("orders").doc("order_good").collection("items").where("userId", "==", ALICE_UID).get()
  );
});
