import type { ReactNode } from "react";

type AuthShellProps = {
  productName: string;
  description: string;
  children: ReactNode;
};

export default function AuthShell({
  productName,
  description,
  children,
}: AuthShellProps) {
  return (
    <main className="auth-page">
      <section className="auth-card" aria-labelledby="login-title">
        <header className="auth-header">
          <div className="product-mark" aria-hidden="true">
            {productName.slice(0, 1).toUpperCase()}
          </div>
          <p className="product-name">{productName}</p>
          <h1 id="login-title">Sign in to {productName}</h1>
          <p className="auth-description">{description}</p>
        </header>

        {children}

        <p className="privacy-note">
          Passwords and session values are never displayed by this interface.
        </p>
      </section>
    </main>
  );
}
