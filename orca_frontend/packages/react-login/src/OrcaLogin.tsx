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

export function OrcaLogin({ branding }: { branding: OrcaLoginBranding }) {
  const [result, setResult] = useState<LoginResult | null>(null);
  const customerLogo = validCustomerLogo(branding.customerLogo);

  async function handleLogin(request: LoginRequest) {
    setResult(null);
    setResult(await submitLogin(request));
  }

  return (
    <main className="orca-login-page">
      <section className="orca-login-card" aria-labelledby="orca-login-title">
        <header className="orca-login-header">
          <div className="orca-login-brand-mark">
            {customerLogo ? (
              <img
                src={customerLogo.bundledAssetSource}
                alt={customerLogo.alternativeText.trim()}
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

function validCustomerLogo(
  logo: OrcaLoginBranding["customerLogo"],
): NonNullable<OrcaLoginBranding["customerLogo"]> | null {
  if (!logo || logo.alternativeText.trim().length === 0) {
    return null;
  }

  const source = logo.bundledAssetSource.trim();
  if (
    /^(?:[a-z][a-z\d+.-]*:|\/\/)/i.test(source) ||
    !/\.(?:png|webp)(?:\?.*)?$/i.test(source)
  ) {
    return null;
  }

  return { ...logo, bundledAssetSource: source };
}
