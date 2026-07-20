import { cleanup, render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { createElement, type ComponentType } from "react";
import { afterEach, describe, expect, it, vi } from "vitest";

const packageName = "@oneofwolvesbilly/orca-react-login";

type OrcaLoginBranding = {
  productName: string;
  supportingCopy: string;
  customerLogo?: {
    bundledAssetSource: string;
    alternativeText: string;
  };
};

type PublicPackage = {
  OrcaLogin: ComponentType<{ branding: OrcaLoginBranding }>;
};

const baseBranding: OrcaLoginBranding = {
  productName: "Example Product",
  supportingCopy: "Sign in to continue.",
};

describe("frontend-03 OrcaLogin contract", () => {
  afterEach(() => {
    cleanup();
    vi.restoreAllMocks();
  });

  it("renders compliant customer branding with mandatory Orca attribution", async () => {
    await renderPublicLogin({
      ...baseBranding,
      customerLogo: {
        bundledAssetSource: "/assets/example-product-logo.webp",
        alternativeText: "Example Product logo",
      },
    });

    expect(screen.getByText("Example Product")).toBeVisible();
    expect(screen.getByText("Sign in to continue.")).toBeVisible();
    expect(
      screen.getByRole("img", { name: "Example Product logo" }),
    ).toHaveAttribute("src", "/assets/example-product-logo.webp");

    const attribution = screen.getByRole("link", { name: "Powered by Orca" });
    expect(attribution).toHaveAttribute(
      "href",
      "https://github.com/OneOfWolvesBilly/Orca",
    );
    expect(attribution).toHaveAttribute("target", "_blank");
    expect(attribution).toHaveAttribute("rel", "noopener noreferrer");
    expect(screen.getByText("© 2026 Chen Chih-hao")).toBeVisible();
  });

  it("uses the generic neutral fallback when no customer logo is supplied", async () => {
    await renderPublicLogin(baseBranding);

    expect(screen.queryByRole("img")).not.toBeInTheDocument();
    expect(screen.getByTestId("orca-neutral-mark")).toBeVisible();
    expect(screen.getByText("Example Product")).toBeVisible();
  });

  it.each(["", "   "])(
    "uses the fallback when customer logo alt is %j",
    async (alternativeText) => {
      await renderPublicLogin({
        ...baseBranding,
        customerLogo: {
          bundledAssetSource: "/assets/example-product-logo.webp",
          alternativeText,
        },
      });

      expect(screen.queryByRole("img")).not.toBeInTheDocument();
      expect(screen.getByTestId("orca-neutral-mark")).toBeVisible();
    },
  );

  it.each([
    "https://customer.example/logo.png",
    "//customer.example/logo.png",
    "ftp://customer.example/logo.png",
    "data:image/png;base64,unsafe-inline-data",
    "/assets/customer-logo.svg",
  ])("does not render unsupported logo source %s", async (bundledAssetSource) => {
    await renderPublicLogin({
      ...baseBranding,
      customerLogo: {
        bundledAssetSource,
        alternativeText: "Example Product logo",
      },
    });

    expect(screen.queryByRole("img")).not.toBeInTheDocument();
    expect(screen.getByTestId("orca-neutral-mark")).toBeVisible();
  });

  it("keeps customer logo inside the approved layout boundary", async () => {
    await renderPublicLogin({
      ...baseBranding,
      customerLogo: {
        bundledAssetSource: "/assets/example-product-logo.png",
        alternativeText: "Example Product logo",
      },
    });

    expect(screen.getByRole("img", { name: "Example Product logo" })).toHaveStyle({
      maxWidth: "64px",
      maxHeight: "64px",
      objectFit: "contain",
    });
  });

  it("preserves the existing login request and safe success behavior", async () => {
    const fetchMock = vi.spyOn(globalThis, "fetch").mockResolvedValue(
      new Response(null, { status: 204 }),
    );
    const user = userEvent.setup();
    await renderPublicLogin(baseBranding);

    await user.type(screen.getByLabelText("Login identifier"), "consumer-user");
    await user.type(screen.getByLabelText("Password"), "not-displayed");
    await user.click(screen.getByRole("button", { name: "Sign in" }));

    await waitFor(() => {
      expect(fetchMock).toHaveBeenCalledWith("/api/auth/login", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        credentials: "include",
        body: JSON.stringify({
          loginIdentifier: "consumer-user",
          password: "not-displayed",
        }),
      });
    });
    expect(screen.getByRole("status")).toHaveTextContent(
      "Login request succeeded",
    );
    expect(screen.getByLabelText("Password")).toHaveValue("");
    expect(screen.queryByText("not-displayed")).not.toBeInTheDocument();
  });
});

async function renderPublicLogin(branding: OrcaLoginBranding) {
  const publicPackage = (await import(
    /* @vite-ignore */ packageName
  )) as PublicPackage;

  return render(createElement(publicPackage.OrcaLogin, { branding }));
}
