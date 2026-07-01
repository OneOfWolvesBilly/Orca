import { useState, type FormEvent } from "react";
import type { LoginRequest } from "../api/login";

type LoginFormProps = {
  onSubmit: (request: LoginRequest) => Promise<void>;
};

export default function LoginForm({ onSubmit }: LoginFormProps) {
  const [loginIdentifier, setLoginIdentifier] = useState("");
  const [password, setPassword] = useState("");
  const [submitting, setSubmitting] = useState(false);

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setSubmitting(true);

    try {
      await onSubmit({ loginIdentifier, password });
    } finally {
      setPassword("");
      setSubmitting(false);
    }
  }

  return (
    <form className="login-form" onSubmit={handleSubmit}>
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
  );
}
