import { useState } from "react";
import { submitLogin, type LoginRequest, type LoginResult } from "./api/login";
import AuthShell from "./components/AuthShell";
import LoginForm from "./components/LoginForm";
import LoginResultView from "./components/LoginResultView";
import "./App.css";

const product = {
  name: "Orca",
  description: "Enter your registered login identifier and password.",
};

export default function App() {
  const [result, setResult] = useState<LoginResult | null>(null);

  async function handleLogin(request: LoginRequest) {
    setResult(null);
    const loginResult = await submitLogin(request);
    setResult(loginResult);
  }

  return (
    <AuthShell productName={product.name} description={product.description}>
      <LoginForm onSubmit={handleLogin} />
      <LoginResultView result={result} />
    </AuthShell>
  );
}
