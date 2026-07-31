import { act, fireEvent, render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import FixtureSessionLifecycle from "./FixtureSessionLifecycle";

const FUTURE_EXPIRY = "2999-07-24T00:00:00Z";
const SIGNED_OUT_MESSAGE = "You have signed out.";
const SESSION_ENDED_MESSAGE =
  "Your session has ended. Please sign in again.";
const BRANDING = {
  productName: "Example Product",
  supportingCopy: "Sign in to continue to your workspace.",
};

describe("frontend-04 React fixture protected session lifecycle", () => {
  beforeEach(() => {
    document.body.innerHTML = '<div id="root"></div>';
    vi.resetModules();
  });

  afterEach(() => {
    vi.useRealTimers();
    vi.restoreAllMocks();
    document.body.innerHTML = "";
  });

  it("starts at the login presentation without probing session state", async () => {
    const fetchMock = vi.spyOn(globalThis, "fetch");

    await renderFixture();

    expect(
      await screen.findByRole("heading", { name: "Sign in to Example Product" }),
    ).toBeVisible();
    expect(fetchMock).not.toHaveBeenCalled();
  });

  it("starts each fresh mount at login without probing or restoring session state", () => {
    const fetchMock = vi.spyOn(globalThis, "fetch");

    const firstMount = render(
      <FixtureSessionLifecycle branding={BRANDING} />,
    );
    expect(
      screen.getByRole("heading", { name: "Sign in to Example Product" }),
    ).toBeVisible();
    firstMount.unmount();

    const secondMount = render(
      <FixtureSessionLifecycle branding={BRANDING} />,
    );
    expect(
      screen.getByRole("heading", { name: "Sign in to Example Product" }),
    ).toBeVisible();
    expect(fetchMock).not.toHaveBeenCalled();
    secondMount.unmount();
  });

  it("enters protected presentation after valid login without invoking the command", async () => {
    const fetchMock = vi.spyOn(globalThis, "fetch").mockResolvedValue(
      loginResponse(FUTURE_EXPIRY),
    );
    const user = userEvent.setup();
    await renderFixture();

    await submitLogin(user);

    expect(
      await screen.findByRole("button", { name: /protected fixture command/i }),
    ).toBeVisible();
    expect(screen.getByRole("button", { name: /log out/i })).toBeVisible();
    expect(fetchMock).toHaveBeenCalledTimes(1);
    expect(document.body).not.toHaveTextContent("ORCA_SESSION");
    expect(document.body).not.toHaveTextContent("actorId");
  });

  it("invokes the protected fixture command explicitly and shows safe success", async () => {
    const fetchMock = vi
      .spyOn(globalThis, "fetch")
      .mockResolvedValueOnce(loginResponse(FUTURE_EXPIRY))
      .mockResolvedValueOnce(new Response(null, { status: 204 }));
    const user = userEvent.setup();
    await renderFixture();
    await submitLogin(user);

    await user.click(
      await screen.findByRole("button", { name: /protected fixture command/i }),
    );

    expect(fetchMock).toHaveBeenNthCalledWith(
      2,
      "/api/fixture/actor-context-check",
      {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        credentials: "include",
        body: "{}",
      },
    );
    expect(await screen.findByRole("status")).toHaveTextContent(
      /protected fixture command succeeded/i,
    );
    expect(document.body).not.toHaveTextContent("actorId");
    expect(document.body).not.toHaveTextContent("sessionId");
  });

  it("lets stable unauthenticated rejection end presentation before the deadline", async () => {
    const fetchMock = vi
      .spyOn(globalThis, "fetch")
      .mockResolvedValueOnce(loginResponse(FUTURE_EXPIRY))
      .mockResolvedValueOnce(
        Response.json(
          {
            status: 401,
            code: "UNAUTHENTICATED",
            message: "Authentication is required",
          },
          { status: 401 },
        ),
      );
    const user = userEvent.setup();
    await renderFixture();
    await submitLogin(user);

    await user.click(
      await screen.findByRole("button", { name: /protected fixture command/i }),
    );

    expect(
      await screen.findByRole("heading", { name: "Sign in to Example Product" }),
    ).toBeVisible();
    expect(screen.getByRole("status")).toHaveTextContent(
      SESSION_ENDED_MESSAGE,
    );
    expect(screen.getByRole("alert")).toHaveTextContent("UNAUTHENTICATED");
    expect(screen.getByRole("alert")).toHaveTextContent(
      "Authentication is required",
    );
    expect(fetchMock).toHaveBeenCalledTimes(2);
  });

  it("shows another stable failure without ending protected presentation", async () => {
    vi.spyOn(globalThis, "fetch")
      .mockResolvedValueOnce(loginResponse(FUTURE_EXPIRY))
      .mockResolvedValueOnce(
        Response.json(
          {
            status: 409,
            code: "FIXTURE_CONFLICT",
            message: "The fixture command could not be completed",
          },
          { status: 409 },
        ),
      );
    const user = userEvent.setup();
    await renderFixture();
    await submitLogin(user);

    await user.click(
      await screen.findByRole("button", { name: /protected fixture command/i }),
    );

    expect(await screen.findByRole("alert")).toHaveTextContent(
      "FIXTURE_CONFLICT",
    );
    expect(screen.getByRole("button", { name: /log out/i })).toBeEnabled();
  });

  it("does not treat an unauthenticated code on a non-401 response as authoritative", async () => {
    vi.spyOn(globalThis, "fetch")
      .mockResolvedValueOnce(loginResponse(FUTURE_EXPIRY))
      .mockResolvedValueOnce(
        Response.json(
          {
            status: 403,
            code: "UNAUTHENTICATED",
            message: "The request was forbidden",
          },
          { status: 403 },
        ),
      );
    const user = userEvent.setup();
    await renderFixture();
    await submitLogin(user);

    await user.click(
      await screen.findByRole("button", { name: /protected fixture command/i }),
    );

    expect(await screen.findByRole("alert")).toHaveTextContent(
      "UNAUTHENTICATED",
    );
    expect(screen.getByRole("button", { name: /log out/i })).toBeEnabled();
  });

  it("maps a malformed protected response safely without ending presentation", async () => {
    vi.spyOn(globalThis, "fetch")
      .mockResolvedValueOnce(loginResponse(FUTURE_EXPIRY))
      .mockResolvedValueOnce(
        new Response("private implementation detail", { status: 500 }),
      );
    const user = userEvent.setup();
    await renderFixture();
    await submitLogin(user);

    await user.click(
      await screen.findByRole("button", { name: /protected fixture command/i }),
    );

    expect(await screen.findByRole("alert")).toHaveTextContent(
      "REQUEST_UNAVAILABLE",
    );
    expect(document.body).not.toHaveTextContent("private implementation detail");
    expect(screen.getByRole("button", { name: /log out/i })).toBeEnabled();
  });

  it("uses existing logout and returns to login presentation on 204", async () => {
    const fetchMock = vi
      .spyOn(globalThis, "fetch")
      .mockResolvedValueOnce(loginResponse(FUTURE_EXPIRY))
      .mockResolvedValueOnce(new Response(null, { status: 204 }));
    const user = userEvent.setup();
    await renderFixture();
    await submitLogin(user);

    await user.click(await screen.findByRole("button", { name: /log out/i }));

    expect(fetchMock).toHaveBeenNthCalledWith(2, "/api/auth/logout", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      credentials: "include",
      body: "{}",
    });
    expect(
      await screen.findByRole("heading", { name: "Sign in to Example Product" }),
    ).toBeVisible();
    expect(screen.getByRole("status")).toHaveTextContent(SIGNED_OUT_MESSAGE);
    expect(screen.queryByText(SESSION_ENDED_MESSAGE)).not.toBeInTheDocument();
  });

  it("keeps failed manual logout safely retryable before the deadline", async () => {
    const fetchMock = vi
      .spyOn(globalThis, "fetch")
      .mockResolvedValueOnce(loginResponse(FUTURE_EXPIRY))
      .mockRejectedValueOnce(new Error("private transport detail"))
      .mockResolvedValueOnce(new Response(null, { status: 204 }));
    const user = userEvent.setup();
    await renderFixture();
    await submitLogin(user);

    await user.click(await screen.findByRole("button", { name: /log out/i }));

    expect(await screen.findByRole("alert")).toHaveTextContent(
      "REQUEST_UNAVAILABLE",
    );
    expect(screen.queryByText(SIGNED_OUT_MESSAGE)).not.toBeInTheDocument();
    expect(document.body).not.toHaveTextContent("private transport detail");
    const retry = screen.getByRole("button", { name: /log out/i });
    expect(retry).toBeEnabled();

    await user.click(retry);

    expect(fetchMock).toHaveBeenCalledTimes(3);
    expect(
      await screen.findByRole("heading", { name: "Sign in to Example Product" }),
    ).toBeVisible();
  });

  it("ends presentation and invokes logout once when the deadline arrives", async () => {
    vi.useFakeTimers();
    vi.setSystemTime(new Date("2026-07-24T00:00:00Z"));
    const fetchMock = vi
      .spyOn(globalThis, "fetch")
      .mockResolvedValueOnce(loginResponse("2026-07-24T00:00:01Z"))
      .mockResolvedValueOnce(new Response(null, { status: 204 }));
    await renderFixture();
    await submitLoginWithFireEvent();
    expect(screen.getByRole("button", { name: /log out/i })).toBeVisible();

    await act(async () => {
      await vi.advanceTimersByTimeAsync(1_000);
    });

    expect(fetchMock).toHaveBeenNthCalledWith(2, "/api/auth/logout", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      credentials: "include",
      body: "{}",
    });
    expect(fetchMock).toHaveBeenCalledTimes(2);
    expect(
      screen.getByRole("heading", { name: "Sign in to Example Product" }),
    ).toBeVisible();
    expect(screen.getByRole("status")).toHaveTextContent(
      SESSION_ENDED_MESSAGE,
    );
  });

  it("does not restore presentation when deadline logout fails", async () => {
    vi.useFakeTimers();
    vi.setSystemTime(new Date("2026-07-24T00:00:00Z"));
    const fetchMock = vi
      .spyOn(globalThis, "fetch")
      .mockResolvedValueOnce(loginResponse("2026-07-24T00:00:01Z"))
      .mockRejectedValueOnce(new Error("private transport detail"));
    await renderFixture();
    await submitLoginWithFireEvent();

    await act(async () => {
      await vi.advanceTimersByTimeAsync(1_000);
    });

    expect(fetchMock).toHaveBeenCalledTimes(2);
    expect(
      screen.getByRole("heading", { name: "Sign in to Example Product" }),
    ).toBeVisible();
    expect(screen.getByRole("status")).toHaveTextContent(
      SESSION_ENDED_MESSAGE,
    );
    expect(document.body).not.toHaveTextContent("private transport detail");
  });

  it("cancels deadline work on unmount without claiming server logout", async () => {
    vi.useFakeTimers();
    vi.setSystemTime(new Date("2026-07-24T00:00:00Z"));
    const fetchMock = vi
      .spyOn(globalThis, "fetch")
      .mockResolvedValueOnce(loginResponse("2026-07-24T00:00:01Z"));
    const fixture = render(<FixtureSessionLifecycle branding={BRANDING} />);
    await submitLoginWithFireEvent();

    fixture.unmount();
    await act(async () => {
      await vi.advanceTimersByTimeAsync(1_000);
    });

    expect(fetchMock).toHaveBeenCalledTimes(1);
  });

  it("does not duplicate a protected command while its request is pending", async () => {
    const pendingProtected = deferred<Response>();
    const fetchMock = vi
      .spyOn(globalThis, "fetch")
      .mockResolvedValueOnce(loginResponse(FUTURE_EXPIRY))
      .mockReturnValueOnce(pendingProtected.promise);
    const user = userEvent.setup();
    await renderFixture();
    await submitLogin(user);

    const command = await screen.findByRole("button", {
      name: /protected fixture command/i,
    });
    await user.click(command);
    fireEvent.click(command);

    expect(fetchMock).toHaveBeenCalledTimes(2);
    pendingProtected.resolve(new Response(null, { status: 204 }));
    expect(await screen.findByRole("status")).toHaveTextContent(
      /protected fixture command succeeded/i,
    );
  });

  it("does not duplicate logout when the deadline races with manual logout", async () => {
    vi.useFakeTimers();
    vi.setSystemTime(new Date("2026-07-24T00:00:00Z"));
    const pendingLogout = deferred<Response>();
    const fetchMock = vi
      .spyOn(globalThis, "fetch")
      .mockResolvedValueOnce(loginResponse("2026-07-24T00:00:01Z"))
      .mockReturnValueOnce(pendingLogout.promise);
    await renderFixture();
    await submitLoginWithFireEvent();

    fireEvent.click(screen.getByRole("button", { name: /log out/i }));
    await act(async () => {
      await vi.advanceTimersByTimeAsync(1_000);
    });

    expect(fetchMock).toHaveBeenCalledTimes(2);
    expect(
      screen.getByRole("heading", { name: "Sign in to Example Product" }),
    ).toBeVisible();
    expect(screen.getByRole("status")).toHaveTextContent(
      SESSION_ENDED_MESSAGE,
    );
    expect(screen.queryByText(SIGNED_OUT_MESSAGE)).not.toBeInTheDocument();
    pendingLogout.resolve(new Response(null, { status: 204 }));
    await act(async () => {
      await pendingLogout.promise;
    });
    expect(screen.getByRole("status")).toHaveTextContent(
      SESSION_ENDED_MESSAGE,
    );
    expect(screen.queryByText(SIGNED_OUT_MESSAGE)).not.toBeInTheDocument();
  });

  it("ignores a stale protected success after the deadline ends presentation", async () => {
    vi.useFakeTimers();
    vi.setSystemTime(new Date("2026-07-24T00:00:00Z"));
    const pendingProtected = deferred<Response>();
    const fetchMock = vi
      .spyOn(globalThis, "fetch")
      .mockResolvedValueOnce(loginResponse("2026-07-24T00:00:01Z"))
      .mockReturnValueOnce(pendingProtected.promise)
      .mockResolvedValueOnce(new Response(null, { status: 204 }));
    await renderFixture();
    await submitLoginWithFireEvent();

    fireEvent.click(
      screen.getByRole("button", { name: /protected fixture command/i }),
    );
    await act(async () => {
      await vi.advanceTimersByTimeAsync(1_000);
    });
    pendingProtected.resolve(new Response(null, { status: 204 }));
    await act(async () => {
      await pendingProtected.promise;
    });

    expect(fetchMock).toHaveBeenCalledTimes(3);
    expect(
      screen.getByRole("heading", { name: "Sign in to Example Product" }),
    ).toBeVisible();
    expect(screen.getByRole("status")).toHaveTextContent(
      SESSION_ENDED_MESSAGE,
    );
    expect(
      screen.queryByText(/protected fixture command succeeded/i),
    ).not.toBeInTheDocument();
  });

  it("clears the ended-session message for a replacement lifecycle and ignores its stale predecessor", async () => {
    vi.useFakeTimers();
    vi.setSystemTime(new Date("2026-07-24T00:00:00Z"));
    const pendingProtected = deferred<Response>();
    vi.spyOn(globalThis, "fetch")
      .mockResolvedValueOnce(loginResponse("2026-07-24T00:00:01Z"))
      .mockReturnValueOnce(pendingProtected.promise)
      .mockResolvedValueOnce(new Response(null, { status: 204 }))
      .mockResolvedValueOnce(loginResponse(FUTURE_EXPIRY));
    await renderFixture();
    await submitLoginWithFireEvent();

    fireEvent.click(
      screen.getByRole("button", { name: /protected fixture command/i }),
    );
    await act(async () => {
      await vi.advanceTimersByTimeAsync(1_000);
    });
    await submitLoginWithFireEvent();

    expect(
      screen.getByRole("button", { name: /protected fixture command/i }),
    ).toBeVisible();
    expect(screen.queryByText(SESSION_ENDED_MESSAGE)).not.toBeInTheDocument();

    pendingProtected.resolve(new Response(null, { status: 204 }));
    await act(async () => {
      await pendingProtected.promise;
    });

    expect(
      screen.getByRole("button", { name: /protected fixture command/i }),
    ).toBeVisible();
    expect(
      screen.queryByText(/protected fixture command succeeded/i),
    ).not.toBeInTheDocument();
  });
});

async function renderFixture() {
  await import("./main");
  await act(async () => undefined);
}

async function submitLogin(user: ReturnType<typeof userEvent.setup>) {
  await user.type(screen.getByLabelText("Login identifier"), "fixture-login");
  await user.type(screen.getByLabelText("Password"), "correct-password");
  await user.click(screen.getByRole("button", { name: "Sign in" }));
}

async function submitLoginWithFireEvent() {
  fireEvent.change(screen.getByLabelText("Login identifier"), {
    target: { value: "fixture-login" },
  });
  fireEvent.change(screen.getByLabelText("Password"), {
    target: { value: "correct-password" },
  });
  await act(async () => {
    fireEvent.click(screen.getByRole("button", { name: "Sign in" }));
    await Promise.resolve();
  });
}

function loginResponse(expiresAt: string) {
  return new Response(null, {
    status: 204,
    headers: { "Orca-Session-Expires-At": expiresAt },
  });
}

function deferred<T>() {
  let resolve!: (value: T) => void;
  const promise = new Promise<T>((fulfill) => {
    resolve = fulfill;
  });
  return { promise, resolve };
}
