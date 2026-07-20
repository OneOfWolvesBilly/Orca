import { describe, expect, it } from "vitest";
import {
  CLIENT_ERROR_CATALOG,
  createClientErrorPresentation,
} from "../../../packages/react-login/src/internal/errors/clientErrorCatalog";

describe("client error catalog", () => {
  it("defines the safe request unavailable presentation", () => {
    expect(CLIENT_ERROR_CATALOG.REQUEST_UNAVAILABLE).toEqual({
      code: "REQUEST_UNAVAILABLE",
      message: "We could not complete the login request. Please try again.",
    });
  });

  it("adds an optional support reference without changing the catalog", () => {
    expect(
      createClientErrorPresentation("REQUEST_UNAVAILABLE", {
        label: "Client failure reference",
        value: "client-ref-123",
      }),
    ).toEqual({
      code: "REQUEST_UNAVAILABLE",
      message: "We could not complete the login request. Please try again.",
      supportReference: {
        label: "Client failure reference",
        value: "client-ref-123",
      },
    });

    expect(CLIENT_ERROR_CATALOG.REQUEST_UNAVAILABLE).not.toHaveProperty(
      "supportReference",
    );
  });
});
