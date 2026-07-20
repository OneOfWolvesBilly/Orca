import { OrcaLogin, type OrcaLoginBranding } from "@oneofwolvesbilly/orca-react-login";

const branding: OrcaLoginBranding = {
  productName: "Orca",
  supportingCopy: "Enter your registered login identifier and password.",
};

export default function App() {
  return <OrcaLogin branding={branding} />;
}
