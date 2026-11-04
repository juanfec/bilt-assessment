import test from "node:test";
import assert from "node:assert/strict";

import { buildViewModel } from "./dashboard.js";

const member = {
  pointsThisMonth: 1_500,
  monthlyCap: 100_000,
  streakMonths: 6,
};

test("shows awarded points as a successful payment", () => {
  const view = buildViewModel(
    { pointsAwarded: 1_500, outcome: "AWARDED" },
    member,
  );

  assert.equal(view.title, "1,500 points credited");
  assert.equal(view.tone, "success");
  assert.equal(view.progressPercent, 1.5);
});

test("shows a duplicate event as skipped rather than credited", () => {
  const view = buildViewModel(
    { pointsAwarded: 0, outcome: "DUPLICATE" },
    member,
  );

  assert.equal(view.title, "Duplicate event skipped");
  assert.equal(view.tone, "neutral");
});

test("a duplicate keeps the member's real progress and explains nothing was added", () => {
  const view = buildViewModel(
    { pointsAwarded: 0, outcome: "DUPLICATE" },
    member,
  );

  assert.equal(view.progressPercent, 1.5);
  assert.match(view.description, /already processed/i);
  assert.doesNotMatch(view.title, /credited/i);
});

test("a capped result never reads as credited points", () => {
  const view = buildViewModel(
    { pointsAwarded: 0, outcome: "CAPPED" },
    { ...member, pointsThisMonth: 100_000 },
  );

  assert.doesNotMatch(view.title, /credited/i);
  assert.match(view.description, /maximum/i);
});

test("progress never exceeds 100% even if points pass the cap", () => {
  const view = buildViewModel(
    { pointsAwarded: 0, outcome: "CAPPED" },
    { ...member, pointsThisMonth: 120_000 },
  );

  assert.equal(view.progressPercent, 100);
});

test("an awarded result that fills the cap is still a success", () => {
  const view = buildViewModel(
    { pointsAwarded: 100_000, outcome: "AWARDED" },
    { ...member, pointsThisMonth: 100_000 },
  );

  assert.equal(view.title, "100,000 points credited");
  assert.equal(view.tone, "success");
  assert.equal(view.progressPercent, 100);
});

test("uses the outcome, not the point count, to pick the state", () => {
  const capped = buildViewModel(
    { pointsAwarded: 500, outcome: "CAPPED" },
    member,
  );
  const duplicate = buildViewModel(
    { pointsAwarded: 500, outcome: "DUPLICATE" },
    member,
  );

  assert.equal(capped.tone, "warning");
  assert.equal(duplicate.tone, "neutral");
});

test("shows when the member has reached the monthly cap", () => {
  const view = buildViewModel(
    { pointsAwarded: 0, outcome: "CAPPED" },
    { ...member, pointsThisMonth: 100_000 },
  );

  assert.equal(view.title, "Monthly cap reached");
  assert.equal(view.tone, "warning");
  assert.equal(view.progressPercent, 100);
});
