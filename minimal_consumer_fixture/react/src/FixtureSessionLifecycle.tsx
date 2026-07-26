import { useEffect, useRef, useState } from "react";
import {
  OrcaLogin,
  logoutOrcaSession,
  type LogoutResult,
  type OrcaLoginBranding,
  type OrcaLoginSession,
} from "@oneofwolvesbilly/orca-react-login";
import {
  invokeFixtureActorContext,
  type FixtureCommandResult,
  type FixtureErrorPresentation,
} from "./api/fixtureActorContext";

type Phase = "login" | "protected";
type Operation = "idle" | "protected-command" | "manual-logout";
type SafeResult = FixtureCommandResult | LogoutResult;
const MAX_TIMER_DELAY_MILLISECONDS = 2_147_483_647;

type FixtureSessionLifecycleProps = {
  branding: OrcaLoginBranding;
};

export default function FixtureSessionLifecycle({
  branding,
}: FixtureSessionLifecycleProps) {
  const [phase, setPhase] = useState<Phase>("login");
  const [operation, setOperation] = useState<Operation>("idle");
  const [protectedResult, setProtectedResult] = useState<SafeResult | null>(null);
  const [loginResult, setLoginResult] =
    useState<FixtureErrorPresentation | null>(null);
  const generation = useRef(0);
  const timer = useRef<ReturnType<typeof setTimeout> | null>(null);
  const logoutStarted = useRef(false);
  const mounted = useRef(false);

  useEffect(() => {
    mounted.current = true;
    return () => {
      mounted.current = false;
      generation.current += 1;
      cancelTimer();
    };
  }, []);

  function establishSession(session: OrcaLoginSession) {
    const currentGeneration = generation.current + 1;
    generation.current = currentGeneration;
    cancelTimer();
    logoutStarted.current = false;
    setLoginResult(null);
    setProtectedResult(null);
    setOperation("idle");
    setPhase("protected");

    scheduleDeadline(Date.parse(session.expiresAt), currentGeneration);
  }

  function scheduleDeadline(deadline: number, currentGeneration: number) {
    if (!isCurrent(currentGeneration)) {
      return;
    }

    const remaining = deadline - Date.now();
    if (remaining <= 0) {
      handleDeadline(currentGeneration);
      return;
    }

    timer.current = setTimeout(
      () => scheduleDeadline(deadline, currentGeneration),
      Math.min(remaining, MAX_TIMER_DELAY_MILLISECONDS),
    );
  }

  function handleDeadline(currentGeneration: number) {
    if (!isCurrent(currentGeneration)) {
      return;
    }

    const shouldStartLogout = !logoutStarted.current;
    logoutStarted.current = true;
    endLifecycle(currentGeneration);
    if (shouldStartLogout) {
      void logoutOrcaSession();
    }
  }

  async function invokeProtectedCommand() {
    if (operation !== "idle") {
      return;
    }

    const currentGeneration = generation.current;
    setOperation("protected-command");
    setProtectedResult(null);
    const result = await invokeFixtureActorContext();
    if (!isCurrent(currentGeneration)) {
      return;
    }

    if (
      result.kind === "stable-error" &&
      result.status === 401 &&
      result.presentation.code === "UNAUTHENTICATED"
    ) {
      endLifecycle(currentGeneration, result.presentation);
      return;
    }

    setOperation("idle");
    setProtectedResult(result);
  }

  async function logoutManually() {
    if (operation !== "idle" || logoutStarted.current) {
      return;
    }

    const currentGeneration = generation.current;
    logoutStarted.current = true;
    setOperation("manual-logout");
    setProtectedResult(null);
    const result = await logoutOrcaSession();
    if (!isCurrent(currentGeneration)) {
      return;
    }

    if (result.kind === "success") {
      endLifecycle(currentGeneration);
      return;
    }

    logoutStarted.current = false;
    setOperation("idle");
    setProtectedResult(result);
  }

  function endLifecycle(
    currentGeneration: number,
    result: FixtureErrorPresentation | null = null,
  ) {
    if (!isCurrent(currentGeneration)) {
      return;
    }

    generation.current += 1;
    cancelTimer();
    setOperation("idle");
    setProtectedResult(null);
    setLoginResult(result);
    setPhase("login");
  }

  function isCurrent(currentGeneration: number) {
    return mounted.current && generation.current === currentGeneration;
  }

  function cancelTimer() {
    if (timer.current !== null) {
      clearTimeout(timer.current);
      timer.current = null;
    }
  }

  if (phase === "login") {
    return (
      <>
        <OrcaLogin
          branding={branding}
          onSessionEstablished={establishSession}
        />
        {loginResult && <SafeErrorView presentation={loginResult} />}
      </>
    );
  }

  const actionsDisabled = operation !== "idle";
  return (
    <main className="fixture-protected-page">
      <section
        className="fixture-protected-card"
        aria-labelledby="fixture-protected-title"
      >
        <p className="fixture-product-name">{branding.productName}</p>
        <h1 id="fixture-protected-title">Protected fixture session</h1>
        <p>
          Use this product-neutral command to verify the current server-owned
          session context.
        </p>
        <div className="fixture-actions">
          <button
            type="button"
            disabled={actionsDisabled}
            onClick={invokeProtectedCommand}
          >
            Run protected fixture command
          </button>
          <button
            type="button"
            disabled={actionsDisabled}
            onClick={logoutManually}
          >
            Log out
          </button>
        </div>
        <SafeResultView result={protectedResult} />
      </section>
    </main>
  );
}

function SafeResultView({ result }: { result: SafeResult | null }) {
  if (!result) {
    return null;
  }

  if (result.kind === "success") {
    return (
      <div className="fixture-result success" role="status">
        Protected fixture command succeeded.
      </div>
    );
  }

  return <SafeErrorView presentation={result.presentation} />;
}

function SafeErrorView({
  presentation,
}: {
  presentation: FixtureErrorPresentation;
}) {
  return (
    <div className="fixture-result error" role="alert">
      <strong>{presentation.code}</strong>
      <span>{presentation.message}</span>
    </div>
  );
}
