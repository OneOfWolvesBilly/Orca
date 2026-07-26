const UTC_INSTANT =
  /^(\d{4})-(\d{2})-(\d{2})T(\d{2}):(\d{2}):(\d{2})(?:\.\d+)?Z$/;

export type SessionPresentationExpiry = {
  expiresAt: string;
  epochMilliseconds: number;
};

export function parseSessionPresentationExpiry(
  response: Response,
  now: number,
): SessionPresentationExpiry | null {
  const value = response.headers.get("Orca-Session-Expires-At");
  const match = value?.match(UTC_INSTANT);
  if (
    value === null ||
    value.trim() !== value ||
    value.length === 0 ||
    value.includes(",") ||
    !match
  ) {
    return null;
  }

  const epochMilliseconds = Date.parse(value);
  if (
    !Number.isFinite(epochMilliseconds) ||
    !matchesUtcCalendar(epochMilliseconds, match) ||
    epochMilliseconds <= now
  ) {
    return null;
  }

  return { expiresAt: value, epochMilliseconds };
}

function matchesUtcCalendar(epochMilliseconds: number, match: RegExpMatchArray) {
  const instant = new Date(epochMilliseconds);
  return (
    instant.getUTCFullYear() === Number(match[1]) &&
    instant.getUTCMonth() + 1 === Number(match[2]) &&
    instant.getUTCDate() === Number(match[3]) &&
    instant.getUTCHours() === Number(match[4]) &&
    instant.getUTCMinutes() === Number(match[5]) &&
    instant.getUTCSeconds() === Number(match[6])
  );
}
