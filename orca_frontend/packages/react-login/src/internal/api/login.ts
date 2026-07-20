import {
  createClientErrorPresentation,
  type ErrorPresentation,
} from "../errors/clientErrorCatalog";
import {
  recordClientDiagnostic,
  type ClientDiagnosticCategory,
} from "./clientDiagnostics";

export type LoginRequest = {
  loginIdentifier: string;
  password: string;
};

export type LoginResult =
  | { kind: "success" }
  | { kind: "stable-error"; presentation: ErrorPresentation }
  | { kind: "generic-error"; presentation: ErrorPresentation };

type StableApiError = {
  status: number;
  code: string;
  message: string;
  loginFailureReferenceId?: string;
};

export async function submitLogin(request: LoginRequest): Promise<LoginResult> {
  let response: Response;
  try {
    response = await fetch("/api/auth/login", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      credentials: "include",
      body: JSON.stringify(request),
    });
  } catch {
    return clientFailure("TRANSPORT_FAILURE");
  }

  if (response.status === 204) {
    return { kind: "success" };
  }

  if (response.ok) {
    return clientFailure("UNEXPECTED_RESPONSE", response.status);
  }

  const error = await parseStableApiError(response);
  if (!error) {
    return clientFailure("MALFORMED_RESPONSE", response.status);
  }

  const loginReference =
    error.code === "LOGIN_REJECTED" && error.loginFailureReferenceId
      ? {
          label: "Login failure reference",
          value: error.loginFailureReferenceId,
        }
      : undefined;

  return {
    kind: "stable-error",
    presentation: {
      code: error.code,
      message: error.message,
      ...(loginReference ? { supportReference: loginReference } : {}),
    },
  };
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
    status !== response.status ||
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

async function clientFailure(
  category: ClientDiagnosticCategory,
  responseStatus?: number,
): Promise<LoginResult> {
  const reference = await recordClientDiagnostic(category, responseStatus);
  return {
    kind: "generic-error",
    presentation: createClientErrorPresentation(
      "REQUEST_UNAVAILABLE",
      reference
        ? { label: "Client failure reference", value: reference }
        : undefined,
    ),
  };
}

function isRecord(value: unknown): value is Record<string, unknown> {
  return typeof value === "object" && value !== null && !Array.isArray(value);
}
