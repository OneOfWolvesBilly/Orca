import { useState } from "react";
import { submitLogin, type LoginRequest, type LoginResult } from "./internal/api/login";
import LoginForm from "./internal/components/LoginForm";
import LoginResultView from "./internal/components/LoginResultView";
import "./styles.css";

export type OrcaLoginBranding = {
  productName: string;
  supportingCopy: string;
  customerLogo?: {
    bundledAssetSource: string;
    alternativeText: string;
  };
};

export const OrcaLoginBranding = Object.freeze({
  customerLogoFormats: ["png", "webp"] as const,
  maximumLogoBytes: 256 * 1024,
  maximumRenderedLogoPixels: 64,
});

const ATTRIBUTION_URL = "https://github.com/OneOfWolvesBilly/Orca";

export type OrcaLoginSession = Readonly<{
  expiresAt: string;
}>;

export type OrcaLoginProps = {
  branding: OrcaLoginBranding;
  onSessionEstablished?: (session: OrcaLoginSession) => void;
};

export function OrcaLogin({ branding, onSessionEstablished }: OrcaLoginProps) {
  const [result, setResult] = useState<LoginResult | null>(null);
  const customerLogo = validCustomerLogo(branding.customerLogo);

  async function handleLogin(request: LoginRequest) {
    setResult(null);
    const nextResult = await submitLogin(
      request,
      onSessionEstablished !== undefined,
    );
    if (nextResult.kind === "session-established") {
      onSessionEstablished?.(nextResult.session);
      return;
    }
    setResult(nextResult);
  }

  return (
    <main className="orca-login-page">
      <section className="orca-login-card" aria-labelledby="orca-login-title">
        <header className="orca-login-header">
          <div className="orca-login-brand-mark">
            {customerLogo ? (
              <img
                src={customerLogo.bundledAssetSource}
                alt={customerLogo.alternativeText}
                style={{ maxWidth: "64px", maxHeight: "64px", objectFit: "contain" }}
              />
            ) : (
              <span className="orca-login-neutral-mark" data-testid="orca-neutral-mark" aria-hidden="true">
                <span />
                <span />
                <span />
              </span>
            )}
          </div>
          <p className="orca-login-product-name">{branding.productName}</p>
          <h1 id="orca-login-title">Sign in to {branding.productName}</h1>
          <p className="orca-login-description">{branding.supportingCopy}</p>
        </header>

        <LoginForm onSubmit={handleLogin} />
        <LoginResultView result={result} />

        <p className="orca-login-privacy-note">
          Passwords and session values are never displayed by this interface.
        </p>
        <footer className="orca-login-footer">
          <a href={ATTRIBUTION_URL} target="_blank" rel="noopener noreferrer">
            Powered by Orca
          </a>
          <span>© 2026 Chen Chih-hao</span>
        </footer>
      </section>
    </main>
  );
}

type ValidCustomerLogo = Readonly<{
  bundledAssetSource: string;
  alternativeText: string;
}>;

function validCustomerLogo(logo: unknown): ValidCustomerLogo | null {
  if (typeof logo !== "object" || logo === null || Array.isArray(logo)) {
    return null;
  }

  const candidate = logo as Record<string, unknown>;
  if (
    typeof candidate.alternativeText !== "string" ||
    typeof candidate.bundledAssetSource !== "string"
  ) {
    return null;
  }

  const alternativeText = candidate.alternativeText.trim();
  const source = candidate.bundledAssetSource.trim();
  if (
    alternativeText.length === 0 ||
    source.length === 0 ||
    /^(?:[a-z][a-z\d+.-]*:|\/\/)/i.test(source) ||
    !/\.(?:png|webp)(?:\?.*)?$/i.test(source)
  ) {
    return null;
  }

  return { bundledAssetSource: source, alternativeText };
}
