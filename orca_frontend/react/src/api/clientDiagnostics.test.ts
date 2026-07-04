import { afterEach, describe, expect, it, vi } from "vitest";
import { recordClientDiagnostic } from "./clientDiagnostics";

describe("client diagnostic adapter", () => {
  afterEach(() => {
    vi.restoreAllMocks();
  });

  it("returns no reference for a non-created response", async () => {
    vi.spyOn(globalThis, "fetch").mockResolvedValue(
      Response.json(
        {
          status: 500,
          code: "INTERNAL_ERROR",
          message: "An unexpected server error occurred",
        },
        { status: 500 },
      ),
    );

    await expect(
      recordClientDiagnostic("MALFORMED_RESPONSE", 500),
    ).resolves.toBeUndefined();
  });

  it("returns no reference for a malformed created response", async () => {
    vi.spyOn(globalThis, "fetch").mockResolvedValue(
      Response.json({ clientFailureReferenceId: "" }, { status: 201 }),
    );

    await expect(
      recordClientDiagnostic("UNEXPECTED_RESPONSE", 200),
    ).resolves.toBeUndefined();
  });
});
