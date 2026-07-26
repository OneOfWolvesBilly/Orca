import {
  createClientErrorPresentation,
  type ErrorPresentation,
} from "../errors/clientErrorCatalog";
import { parseStableApiError } from "./stableApiError";

export type LogoutResult =
  | { kind: "success" }
  | { kind: "stable-error"; presentation: ErrorPresentation }
  | { kind: "generic-error"; presentation: ErrorPresentation };

export async function logoutOrcaSession(): Promise<LogoutResult> {
  let response: Response;
  try {
    response = await fetch("/api/auth/logout", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      credentials: "include",
      body: "{}",
    });
  } catch {
    return genericFailure();
  }

  if (response.status === 204) {
    return { kind: "success" };
  }

  if (response.ok) {
    return genericFailure();
  }

  const error = await parseStableApiError(response);
  if (!error) {
    return genericFailure();
  }

  return {
    kind: "stable-error",
    presentation: { code: error.code, message: error.message },
  };
}

function genericFailure(): LogoutResult {
  return {
    kind: "generic-error",
    presentation: createClientErrorPresentation("REQUEST_UNAVAILABLE"),
  };
}
