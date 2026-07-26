import { cleanup, render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import type { ComponentType } from "react";
import { afterEach, describe, expect, it, vi } from "vitest";
import * as PublicPackage from "@oneofwolvesbilly/orca-react-login";

type SessionPresentation = {
  expiresAt: string;
};

type LifecycleLoginProps = {
  branding: {
    productName: string;
    supportingCopy: string;
  };
  onSessionEstablished?: (session: SessionPresentation) => void;
};

type LogoutResult =
  | { kind: "success" }
  | {
      kind: "stable-error" | "generic-error";
      presentation: { code: string; message: string };
    };

type LogoutOrcaSession = () => Promise<LogoutResult>;

const OrcaLifecycleLogin = PublicPackage.OrcaLogin as ComponentType<LifecycleLoginProps>;
const branding = {
  productName: "Example Product",
  supportingCopy: "Sign in to continue.",
};

describe("frontend-04 public session coordination contract", () => {
  afterEach(() => {
    cleanup();
    vi.restoreAllMocks();
  });

  it("hands an opted-in consumer only the validated future expiry", async () => {
    const expiresAt = "2999-07-24T00:00:00Z";
    vi.spyOn(globalThis, "fetch").mockResolvedValue(loginResponse(expiresAt));
    const onSessionEstablished = vi.fn();
    const user = userEvent.setup();
    render(
      <OrcaLifecycleLogin
        branding={branding}
        onSessionEstablished={onSessionEstablished}
      />,
    );

    await submitLogin(user);

    expect(onSessionEstablished).toHaveBeenCalledOnce();
    expect(onSessionEstablished).toHaveBeenCalledWith({ expiresAt });
    expect(Object.keys(onSessionEstablished.mock.calls[0][0])).toEqual([
      "expiresAt",
    ]);
    expect(screen.queryByText("Login request succeeded")).not.toBeInTheDocument();
  });

  it.each([
    ["missing", undefined],
    ["blank", "   "],
    ["malformed", "not-an-instant"],
    ["invalid calendar", "2999-02-31T00:00:00Z"],
    ["non-UTC", "2999-07-24T08:00:00+08:00"],
    ["already reached", "2000-01-01T00:00:00Z"],
  ])(
    "rejects %s expiry metadata, performs one cleanup logout, and stays safe",
    async (_label, expiresAt) => {
      const fetchMock = vi
        .spyOn(globalThis, "fetch")
        .mockResolvedValueOnce(loginResponse(expiresAt))
        .mockResolvedValueOnce(new Response(null, { status: 204 }));
      const onSessionEstablished = vi.fn();
      const user = userEvent.setup();
      render(
        <OrcaLifecycleLogin
          branding={branding}
          onSessionEstablished={onSessionEstablished}
        />,
      );

      await submitLogin(user);

      expect(await screen.findByRole("alert")).toHaveTextContent(
        "REQUEST_UNAVAILABLE",
      );
      expect(onSessionEstablished).not.toHaveBeenCalled();
      expect(fetchMock).toHaveBeenNthCalledWith(2, "/api/auth/logout", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        credentials: "include",
        body: "{}",
      });
      expect(fetchMock).toHaveBeenCalledTimes(2);
      expect(screen.queryByText(/failure reference/i)).not.toBeInTheDocument();
    },
  );

  it("rejects duplicate expiry metadata instead of selecting one value", async () => {
    const headers = new Headers();
    headers.append("Orca-Session-Expires-At", "2999-07-24T00:00:00Z");
    headers.append("Orca-Session-Expires-At", "2999-07-25T00:00:00Z");
    const fetchMock = vi
      .spyOn(globalThis, "fetch")
      .mockResolvedValueOnce(new Response(null, { status: 204, headers }))
      .mockResolvedValueOnce(new Response(null, { status: 204 }));
    const onSessionEstablished = vi.fn();
    const user = userEvent.setup();
    render(
      <OrcaLifecycleLogin
        branding={branding}
        onSessionEstablished={onSessionEstablished}
      />,
    );

    await submitLogin(user);

    expect(await screen.findByRole("alert")).toHaveTextContent(
      "REQUEST_UNAVAILABLE",
    );
    expect(onSessionEstablished).not.toHaveBeenCalled();
    expect(fetchMock).toHaveBeenCalledTimes(2);
  });

  it("does not recursively report a failed invalid-metadata cleanup", async () => {
    const fetchMock = vi
      .spyOn(globalThis, "fetch")
      .mockResolvedValueOnce(loginResponse(undefined))
      .mockRejectedValueOnce(new Error("private cleanup failure"));
    const onSessionEstablished = vi.fn();
    const user = userEvent.setup();
    render(
      <OrcaLifecycleLogin
        branding={branding}
        onSessionEstablished={onSessionEstablished}
      />,
    );

    await submitLogin(user);

    expect(await screen.findByRole("alert")).toHaveTextContent(
      "REQUEST_UNAVAILABLE",
    );
    expect(screen.queryByText(/failure reference/i)).not.toBeInTheDocument();
    expect(document.body).not.toHaveTextContent("private cleanup failure");
    expect(fetchMock).toHaveBeenCalledTimes(2);
  });

  it("posts the existing empty logout command and returns safe success", async () => {
    const fetchMock = vi.spyOn(globalThis, "fetch").mockResolvedValue(
      new Response(null, { status: 204 }),
    );
    const logout = publicLogout();

    const result = await logout();

    expect(fetchMock).toHaveBeenCalledWith("/api/auth/logout", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      credentials: "include",
      body: "{}",
    });
    expect(result).toEqual({ kind: "success" });
  });

  it("preserves a stable logout error without exposing the raw response", async () => {
    vi.spyOn(globalThis, "fetch").mockResolvedValue(
      Response.json(
        {
          status: 400,
          code: "VALIDATION_ERROR",
          message: "Request validation failed",
        },
        { status: 400 },
      ),
    );
    const logout = publicLogout();

    const result = await logout();

    expect(result).toEqual({
      kind: "stable-error",
      presentation: {
        code: "VALIDATION_ERROR",
        message: "Request validation failed",
      },
    });
    expect(JSON.stringify(result)).not.toContain("Response");
  });

  it("maps logout transport failure to the safe generic result", async () => {
    vi.spyOn(globalThis, "fetch").mockRejectedValue(
      new Error("private transport detail"),
    );
    const logout = publicLogout();

    const result = await logout();

    expect(result.kind).toBe("generic-error");
    expect(result).toMatchObject({
      presentation: { code: "REQUEST_UNAVAILABLE" },
    });
    expect(JSON.stringify(result)).not.toContain("private transport detail");
  });

  it("maps an unexpected successful logout response to the safe generic result", async () => {
    vi.spyOn(globalThis, "fetch").mockResolvedValue(
      Response.json({ internal: "detail" }, { status: 200 }),
    );
    const logout = publicLogout();

    const result = await logout();

    expect(result.kind).toBe("generic-error");
    expect(result).toMatchObject({
      presentation: { code: "REQUEST_UNAVAILABLE" },
    });
    expect(JSON.stringify(result)).not.toContain("detail");
  });
});

function publicLogout(): LogoutOrcaSession {
  const candidate = (
    PublicPackage as typeof PublicPackage & {
      logoutOrcaSession?: LogoutOrcaSession;
    }
  ).logoutOrcaSession;
  expect(candidate).toBeTypeOf("function");
  return candidate as LogoutOrcaSession;
}

async function submitLogin(user: ReturnType<typeof userEvent.setup>) {
  await user.type(screen.getByLabelText("Login identifier"), "fixture-login");
  await user.type(screen.getByLabelText("Password"), "correct-password");
  await user.click(screen.getByRole("button", { name: "Sign in" }));
}

function loginResponse(expiresAt?: string) {
  return new Response(null, {
    status: 204,
    ...(expiresAt === undefined
      ? {}
      : { headers: { "Orca-Session-Expires-At": expiresAt } }),
  });
}
