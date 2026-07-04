import type { LoginResult } from "../api/login";

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

  const { presentation } = result;
  return (
    <div className="result error" role="alert">
      <p className="result-label">{presentation.code}</p>
      <h2>Login request was not completed</h2>
      <p>{presentation.message}</p>
      {presentation.supportReference && (
        <p className="reference">
          <span>{presentation.supportReference.label}</span>
          <code>{presentation.supportReference.value}</code>
        </p>
      )}
    </div>
  );
}
