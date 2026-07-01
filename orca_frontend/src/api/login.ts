export type LoginRequest = {
  loginIdentifier: string;
  password: string;
};

export type LoginResult =
  | { kind: "success" }
  | {
      kind: "stable-error";
      code: string;
      message: string;
      loginFailureReferenceId?: string;
    }
  | { kind: "generic-error" };

type StableApiError = {
  status: number;
  code: string;
  message: string;
  loginFailureReferenceId?: string;
};

export async function submitLogin(request: LoginRequest): Promise<LoginResult> {
  try {
    const response = await fetch("/api/auth/login", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      credentials: "include",
      body: JSON.stringify(request),
    });

    if (response.status === 204) {
      return { kind: "success" };
    }

    if (response.ok) {
      return { kind: "generic-error" };
    }

    const error = await parseStableApiError(response);
    if (!error) {
      return { kind: "generic-error" };
    }

    return {
      kind: "stable-error",
      code: error.code,
      message: error.message,
      loginFailureReferenceId:
        error.code === "LOGIN_REJECTED"
          ? error.loginFailureReferenceId
          : undefined,
    };
  } catch {
    return { kind: "generic-error" };
  }
}

async function parseStableApiError(
  response: Response,
): Promise<StableApiError | null> {
  let body: unknown;
  try {
    body = await response.json();
  } catch {
    return null;
  }

  if (!isRecord(body)) {
    return null;
  }

  const { status, code, message, loginFailureReferenceId } = body;
  if (
    typeof status !== "number" ||
    typeof code !== "string" ||
    code.length === 0 ||
    typeof message !== "string" ||
    message.length === 0
  ) {
    return null;
  }

  if (
    loginFailureReferenceId !== undefined &&
    loginFailureReferenceId !== null &&
    typeof loginFailureReferenceId !== "string"
  ) {
    return null;
  }

  return {
    status,
    code,
    message,
    loginFailureReferenceId:
      typeof loginFailureReferenceId === "string"
        ? loginFailureReferenceId
        : undefined,
  };
}

function isRecord(value: unknown): value is Record<string, unknown> {
  return typeof value === "object" && value !== null && !Array.isArray(value);
}
