import { FormEvent, useState } from "react";
import { LoginResult, submitLogin } from "./api/login";
import "./App.css";

const GENERIC_ERROR_MESSAGE =
  "We could not complete the login request. Please try again.";

export default function App() {
  const [loginIdentifier, setLoginIdentifier] = useState("");
  const [password, setPassword] = useState("");
  const [result, setResult] = useState<LoginResult | null>(null);
  const [submitting, setSubmitting] = useState(false);

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setSubmitting(true);
    setResult(null);

    const loginResult = await submitLogin({ loginIdentifier, password });
    setPassword("");
    setResult(loginResult);
    setSubmitting(false);
  }

  return (
    <main className="shell">
      <section className="intro" aria-labelledby="orca-title">
        <div className="brand" aria-hidden="true">
          ORCA
        </div>
        <p className="eyebrow">Frontend reference shell</p>
        <h1 id="orca-title">A focused place to verify Orca workflows.</h1>
        <p className="intro-copy">
          This first delivery slice connects the browser to Orca's existing
          password login API and presents only safe, stable results.
        </p>
        <dl className="scope-list">
          <div>
            <dt>Available now</dt>
            <dd>Password login result</dd>
          </div>
          <div>
            <dt>Coming later</dt>
            <dd>Session-aware workspace</dd>
          </div>
        </dl>
      </section>

      <section className="login-panel" aria-labelledby="login-title">
        <div className="panel-heading">
          <p className="step">Frontend 01</p>
          <h2 id="login-title">Sign in to Orca</h2>
          <p>Enter a registered login identifier and password.</p>
        </div>

        <form onSubmit={handleSubmit}>
          <label htmlFor="login-identifier">Login identifier</label>
          <input
            id="login-identifier"
            name="loginIdentifier"
            type="text"
            autoComplete="username"
            value={loginIdentifier}
            onChange={(event) => setLoginIdentifier(event.target.value)}
            disabled={submitting}
            required
          />

          <label htmlFor="password">Password</label>
          <input
            id="password"
            name="password"
            type="password"
            autoComplete="current-password"
            value={password}
            onChange={(event) => setPassword(event.target.value)}
            disabled={submitting}
            required
          />

          <button type="submit" disabled={submitting}>
            {submitting ? "Signing in..." : "Sign in"}
          </button>
        </form>

        {result?.kind === "success" && (
          <div className="result success" role="status">
            <p className="result-label">Request accepted</p>
            <h3>Login request succeeded</h3>
            <p>
              Orca accepted this login request. Session-aware navigation is
              outside this first frontend slice.
            </p>
          </div>
        )}

        {result?.kind === "stable-error" && (
          <div className="result error" role="alert">
            <p className="result-label">{result.code}</p>
            <h3>Login request was not completed</h3>
            <p>{result.message}</p>
            {result.loginFailureReferenceId && (
              <p className="reference">
                <span>Failure reference</span>
                <code>{result.loginFailureReferenceId}</code>
              </p>
            )}
          </div>
        )}

        {result?.kind === "generic-error" && (
          <div className="result error" role="alert">
            <p className="result-label">REQUEST_UNAVAILABLE</p>
            <h3>Login request was not completed</h3>
            <p>{GENERIC_ERROR_MESSAGE}</p>
          </div>
        )}

        <p className="privacy-note">
          Passwords and session values are never displayed by this interface.
        </p>
      </section>
    </main>
  );
}
