import {
  createClientErrorPresentation,
  type ErrorPresentation,
} from "../errors/clientErrorCatalog";
import {
  recordClientDiagnostic,
  type ClientDiagnosticCategory,
} from "./clientDiagnostics";
import { logoutOrcaSession } from "./logout";
import { parseStableApiError } from "./stableApiError";
import { parseSessionPresentationExpiry } from "../session/expiry";

export type LoginRequest = {
  loginIdentifier: string;
  password: string;
};

export type LoginResult =
  | { kind: "success" }
  | { kind: "session-established"; session: { expiresAt: string } }
  | { kind: "stable-error"; presentation: ErrorPresentation }
  | { kind: "generic-error"; presentation: ErrorPresentation };

export async function submitLogin(
  request: LoginRequest,
  sessionCoordination = false,
): Promise<LoginResult> {
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
    if (sessionCoordination) {
      const expiry = parseSessionPresentationExpiry(response, Date.now());
      if (!expiry) {
        void logoutOrcaSession();
        return safeGenericFailure();
      }

      return {
        kind: "session-established",
        session: { expiresAt: expiry.expiresAt },
      };
    }
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

function safeGenericFailure(): LoginResult {
  return {
    kind: "generic-error",
    presentation: createClientErrorPresentation("REQUEST_UNAVAILABLE"),
  };
}
