export type StableApiError = {
  status: number;
  code: string;
  message: string;
  loginFailureReferenceId?: string;
};

export async function parseStableApiError(
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

function isRecord(value: unknown): value is Record<string, unknown> {
  return typeof value === "object" && value !== null && !Array.isArray(value);
}
