import { StrictMode } from "react";
import { createRoot } from "react-dom/client";
import { type OrcaLoginBranding } from "@oneofwolvesbilly/orca-react-login";
import customerLogo from "./assets/example-product-logo.png";
import FixtureSessionLifecycle from "./FixtureSessionLifecycle";
import "./styles.css";

const branding: OrcaLoginBranding = {
  productName: "Example Product",
  supportingCopy: "Sign in to continue to your workspace.",
  customerLogo: {
    bundledAssetSource: customerLogo,
    alternativeText: "Example Product logo",
  },
};

createRoot(document.getElementById("root")!).render(
  <StrictMode>
    <FixtureSessionLifecycle branding={branding} />
  </StrictMode>,
);
