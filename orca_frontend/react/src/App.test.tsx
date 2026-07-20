import { cleanup, render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { afterEach, describe, expect, it, vi } from "vitest";
import { OrcaLogin } from "@oneofwolvesbilly/orca-react-login";
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

  it("presents consumer branding with fixed Orca attribution", () => {
    render(
      <OrcaLogin
        branding={{ productName: "Example", supportingCopy: "Example access" }}
      />,
    );

    expect(screen.getByRole("heading", { name: "Sign in to Example" })).toBeVisible();
    expect(screen.getByText("Example access")).toBeVisible();
    expect(screen.getByRole("link", { name: "Powered by Orca" })).toBeVisible();
    expect(screen.getByText("© 2026 Chen Chih-hao")).toBeVisible();
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
    expect(screen.getByText("Login failure reference")).toBeVisible();
    expect(screen.getByText("login-ref-123")).toBeVisible();
  });

  it("does not show a login reference for another stable API error", async () => {
    const fetchMock = vi.spyOn(globalThis, "fetch").mockResolvedValue(
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
    expect(fetchMock).toHaveBeenCalledTimes(1);
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

  it("records a transport failure and displays the persisted client reference", async () => {
    const fetchMock = vi
      .spyOn(globalThis, "fetch")
      .mockRejectedValueOnce(new Error("internal connection details"))
      .mockResolvedValueOnce(
        Response.json(
          { clientFailureReferenceId: "client-ref-transport" },
          { status: 201 },
        ),
      );
    const user = userEvent.setup();

    render(<App />);
    await submitLogin(user);

    expect(fetchMock).toHaveBeenNthCalledWith(2, "/api/client-diagnostics", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({
        category: "TRANSPORT_FAILURE",
        operation: "PASSWORD_LOGIN",
        clientApplication: "REACT",
      }),
    });
    expect(await screen.findByRole("alert")).toHaveTextContent(
      "REQUEST_UNAVAILABLE",
    );
    expect(screen.getByText("Client failure reference")).toBeVisible();
    expect(screen.getByText("client-ref-transport")).toBeVisible();
    expect(screen.queryByText("TRANSPORT_FAILURE")).not.toBeInTheDocument();
  });

  it("records a malformed unsuccessful response without copying its body", async () => {
    const fetchMock = vi
      .spyOn(globalThis, "fetch")
      .mockResolvedValueOnce(
        new Response("sensitive backend implementation details", {
          status: 500,
        }),
      )
      .mockResolvedValueOnce(
        Response.json(
          { clientFailureReferenceId: "client-ref-malformed" },
          { status: 201 },
        ),
      );
    const user = userEvent.setup();

    render(<App />);
    await submitLogin(user);

    const diagnosticRequest = fetchMock.mock.calls[1]?.[1];
    expect(diagnosticRequest).toEqual({
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({
        category: "MALFORMED_RESPONSE",
        operation: "PASSWORD_LOGIN",
        clientApplication: "REACT",
        responseStatus: 500,
      }),
    });
    expect(JSON.stringify(diagnosticRequest)).not.toContain(
      "sensitive backend implementation details",
    );
    expect(await screen.findByText("client-ref-malformed")).toBeVisible();
  });

  it("treats a stable error status mismatch as malformed", async () => {
    const fetchMock = vi
      .spyOn(globalThis, "fetch")
      .mockResolvedValueOnce(
        Response.json(
          {
            status: 400,
            code: "INTERNAL_ERROR",
            message: "Safe message",
          },
          { status: 500 },
        ),
      )
      .mockResolvedValueOnce(
        Response.json(
          { clientFailureReferenceId: "client-ref-status-mismatch" },
          { status: 201 },
        ),
      );
    const user = userEvent.setup();

    render(<App />);
    await submitLogin(user);

    expect(fetchMock).toHaveBeenNthCalledWith(2, "/api/client-diagnostics", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({
        category: "MALFORMED_RESPONSE",
        operation: "PASSWORD_LOGIN",
        clientApplication: "REACT",
        responseStatus: 500,
      }),
    });
    expect(await screen.findByText("client-ref-status-mismatch")).toBeVisible();
  });

  it("records an unexpected successful response", async () => {
    const fetchMock = vi
      .spyOn(globalThis, "fetch")
      .mockResolvedValueOnce(Response.json({ accepted: true }, { status: 200 }))
      .mockResolvedValueOnce(
        Response.json(
          { clientFailureReferenceId: "client-ref-unexpected" },
          { status: 201 },
        ),
      );
    const user = userEvent.setup();

    render(<App />);
    await submitLogin(user);

    expect(fetchMock).toHaveBeenNthCalledWith(2, "/api/client-diagnostics", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({
        category: "UNEXPECTED_RESPONSE",
        operation: "PASSWORD_LOGIN",
        clientApplication: "REACT",
        responseStatus: 200,
      }),
    });
    expect(await screen.findByText("client-ref-unexpected")).toBeVisible();
  });

  it("keeps request unavailable without a reference when diagnostics fail", async () => {
    const fetchMock = vi
      .spyOn(globalThis, "fetch")
      .mockRejectedValueOnce(new Error("login transport detail"))
      .mockRejectedValueOnce(new Error("diagnostic transport detail"));
    const user = userEvent.setup();

    render(<App />);
    await submitLogin(user);

    expect(await screen.findByRole("alert")).toHaveTextContent(
      "REQUEST_UNAVAILABLE",
    );
    expect(
      screen.queryByText("Client failure reference"),
    ).not.toBeInTheDocument();
    expect(fetchMock).toHaveBeenCalledTimes(2);
    expect(screen.queryByText("login transport detail")).not.toBeInTheDocument();
    expect(
      screen.queryByText("diagnostic transport detail"),
    ).not.toBeInTheDocument();
    expect(screen.getByLabelText("Password")).toHaveValue("");
    expect(screen.getByRole("button", { name: "Sign in" })).toBeEnabled();
  });
});

async function submitLogin(user: ReturnType<typeof userEvent.setup>) {
  await user.type(screen.getByLabelText("Login identifier"), "orca-user");
  await user.type(screen.getByLabelText("Password"), "password");
  await user.click(screen.getByRole("button", { name: "Sign in" }));
}
