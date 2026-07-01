import { cleanup, render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { afterEach, describe, expect, it, vi } from "vitest";
import App from "./App";

describe("frontend login result shell", () => {
  afterEach(() => {
    cleanup();
    vi.restoreAllMocks();
  });

  it("shows the login form without claiming an authenticated session", () => {
    render(<App />);

    expect(screen.getByRole("heading", { name: "Sign in to Orca" })).toBeVisible();
    expect(screen.getByLabelText("Login identifier")).toBeVisible();
    expect(screen.getByLabelText("Password")).toBeVisible();
    expect(screen.queryByText(/authenticated/i)).not.toBeInTheDocument();
  });

  it("submits credentials and shows a safe success result", async () => {
    const fetchMock = vi.spyOn(globalThis, "fetch").mockResolvedValue(
      new Response(null, { status: 204 }),
    );
    const user = userEvent.setup();

    render(<App />);
    await user.type(screen.getByLabelText("Login identifier"), "orca-user");
    await user.type(screen.getByLabelText("Password"), "not-shown");
    await user.click(screen.getByRole("button", { name: "Sign in" }));

    await waitFor(() => {
      expect(fetchMock).toHaveBeenCalledWith("/api/auth/login", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        credentials: "include",
        body: JSON.stringify({
          loginIdentifier: "orca-user",
          password: "not-shown",
        }),
      });
    });
    expect(screen.getByRole("status")).toHaveTextContent("Login request succeeded");
    expect(screen.queryByText("not-shown")).not.toBeInTheDocument();
  });

  it("shows the stable login rejection and opaque reference", async () => {
    vi.spyOn(globalThis, "fetch").mockResolvedValue(
      Response.json(
        {
          status: 401,
          code: "LOGIN_REJECTED",
          message: "Login was rejected",
          loginFailureReferenceId: "login-ref-123",
        },
        { status: 401 },
      ),
    );
    const user = userEvent.setup();

    render(<App />);
    await submitLogin(user);

    expect(await screen.findByRole("alert")).toHaveTextContent("LOGIN_REJECTED");
    expect(screen.getByRole("alert")).toHaveTextContent("Login was rejected");
    expect(screen.getByText("login-ref-123")).toBeVisible();
  });

  it("does not show a login reference for another stable API error", async () => {
    vi.spyOn(globalThis, "fetch").mockResolvedValue(
      Response.json(
        {
          status: 400,
          code: "INVALID_REQUEST",
          message: "Request was invalid",
          loginFailureReferenceId: "must-not-be-shown",
        },
        { status: 400 },
      ),
    );
    const user = userEvent.setup();

    render(<App />);
    await submitLogin(user);

    expect(await screen.findByRole("alert")).toHaveTextContent("INVALID_REQUEST");
    expect(screen.queryByText("must-not-be-shown")).not.toBeInTheDocument();
  });

  it("shows a generic result for a malformed API error", async () => {
    vi.spyOn(globalThis, "fetch").mockResolvedValue(
      new Response("backend implementation details", {
        status: 500,
        headers: { "Content-Type": "text/plain" },
      }),
    );
    const user = userEvent.setup();

    render(<App />);
    await submitLogin(user);

    expect(await screen.findByRole("alert")).toHaveTextContent(
      "We could not complete the login request",
    );
    expect(
      screen.queryByText("backend implementation details"),
    ).not.toBeInTheDocument();
  });

  it("shows a generic result for a transport failure", async () => {
    vi.spyOn(globalThis, "fetch").mockRejectedValue(
      new Error("internal connection details"),
    );
    const user = userEvent.setup();

    render(<App />);
    await submitLogin(user);

    expect(await screen.findByRole("alert")).toHaveTextContent(
      "We could not complete the login request",
    );
    expect(screen.queryByText("internal connection details")).not.toBeInTheDocument();
  });
});

async function submitLogin(user: ReturnType<typeof userEvent.setup>) {
  await user.type(screen.getByLabelText("Login identifier"), "orca-user");
  await user.type(screen.getByLabelText("Password"), "password");
  await user.click(screen.getByRole("button", { name: "Sign in" }));
}
