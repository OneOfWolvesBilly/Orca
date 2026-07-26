export type FixtureErrorPresentation = {
  code: string;
  message: string;
};

export type FixtureCommandResult =
  | { kind: "success" }
  | {
      kind: "stable-error";
      status: number;
      presentation: FixtureErrorPresentation;
    }
  | { kind: "generic-error"; presentation: FixtureErrorPresentation };

export async function invokeFixtureActorContext(): Promise<FixtureCommandResult> {
  let response: Response;
  try {
    response = await fetch("/api/fixture/actor-context-check", {
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

  const error = await parseStableError(response);
  return error
    ? {
        kind: "stable-error",
        status: response.status,
        presentation: error,
      }
    : genericFailure();
}

async function parseStableError(
  response: Response,
): Promise<FixtureErrorPresentation | null> {
  let body: unknown;
  try {
    body = await response.json();
  } catch {
    return null;
  }

  if (typeof body !== "object" || body === null || Array.isArray(body)) {
    return null;
  }

  const { status, code, message } = body as Record<string, unknown>;
  if (
    status !== response.status ||
    typeof code !== "string" ||
    code.length === 0 ||
    typeof message !== "string" ||
    message.length === 0
  ) {
    return null;
  }

  return { code, message };
}

function genericFailure(): FixtureCommandResult {
  return {
    kind: "generic-error",
    presentation: {
      code: "REQUEST_UNAVAILABLE",
      message: "We could not complete the request. Please try again.",
    },
  };
}
