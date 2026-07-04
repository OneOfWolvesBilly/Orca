export type ClientDiagnosticCategory =
  | "TRANSPORT_FAILURE"
  | "MALFORMED_RESPONSE"
  | "UNEXPECTED_RESPONSE";

type ClientDiagnosticRequest = {
  category: ClientDiagnosticCategory;
  operation: "PASSWORD_LOGIN";
  clientApplication: "REACT";
  responseStatus?: number;
};

export async function recordClientDiagnostic(
  category: ClientDiagnosticCategory,
  responseStatus?: number,
): Promise<string | undefined> {
  const request: ClientDiagnosticRequest = {
    category,
    operation: "PASSWORD_LOGIN",
    clientApplication: "REACT",
    ...(responseStatus === undefined ? {} : { responseStatus }),
  };

  try {
    const response = await fetch("/api/client-diagnostics", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(request),
    });
    if (response.status !== 201) {
      return undefined;
    }

    const body: unknown = await response.json();
    if (!isRecord(body)) {
      return undefined;
    }
    const reference = body.clientFailureReferenceId;
    return typeof reference === "string" && reference.trim().length > 0
      ? reference
      : undefined;
  } catch {
    return undefined;
  }
}

function isRecord(value: unknown): value is Record<string, unknown> {
  return typeof value === "object" && value !== null && !Array.isArray(value);
}
