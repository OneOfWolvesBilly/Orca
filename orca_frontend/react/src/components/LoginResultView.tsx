import type { LoginResult } from "../api/login";

const GENERIC_ERROR_MESSAGE =
  "We could not complete the login request. Please try again.";

type LoginResultViewProps = {
  result: LoginResult | null;
};

export default function LoginResultView({ result }: LoginResultViewProps) {
  if (!result) {
    return null;
  }

  if (result.kind === "success") {
    return (
      <div className="result success" role="status">
        <p className="result-label">Request accepted</p>
        <h2>Login request succeeded</h2>
        <p>The server accepted this login request.</p>
      </div>
    );
  }

  if (result.kind === "stable-error") {
    return (
      <div className="result error" role="alert">
        <p className="result-label">{result.code}</p>
        <h2>Login request was not completed</h2>
        <p>{result.message}</p>
        {result.loginFailureReferenceId && (
          <p className="reference">
            <span>Failure reference</span>
            <code>{result.loginFailureReferenceId}</code>
          </p>
        )}
      </div>
    );
  }

  return (
    <div className="result error" role="alert">
      <p className="result-label">REQUEST_UNAVAILABLE</p>
      <h2>Login request was not completed</h2>
      <p>{GENERIC_ERROR_MESSAGE}</p>
    </div>
  );
}
