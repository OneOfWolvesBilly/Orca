export type SupportReference = {
  label: string;
  value: string;
};

export type ErrorPresentation = {
  code: string;
  message: string;
  supportReference?: SupportReference;
};

export type ClientErrorCode = "REQUEST_UNAVAILABLE";

export const CLIENT_ERROR_CATALOG = {
  REQUEST_UNAVAILABLE: {
    code: "REQUEST_UNAVAILABLE",
    message: "We could not complete the login request. Please try again.",
  },
} as const satisfies Record<
  ClientErrorCode,
  Omit<ErrorPresentation, "supportReference">
>;

export function createClientErrorPresentation(
  code: ClientErrorCode,
  supportReference?: SupportReference,
): ErrorPresentation {
  return supportReference
    ? { ...CLIENT_ERROR_CATALOG[code], supportReference }
    : { ...CLIENT_ERROR_CATALOG[code] };
}
