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
  OrcaLogin: ComponentType<{ branding: unknown }>;
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

  it.each([
    ["null", null],
    ["a string", "not-a-logo-object"],
    ["a number", 42],
    ["a boolean", true],
    ["an array", []],
    ["an empty object", {}],
  ])(
    "uses the fallback when the runtime customer logo is %s",
    async (_description, customerLogo) => {
      await renderPublicLogin({
        ...baseBranding,
        customerLogo,
      });

      expect(screen.queryByRole("img")).not.toBeInTheDocument();
      expect(screen.getByTestId("orca-neutral-mark")).toBeVisible();
    },
  );

  it.each([
    ["absent", {}],
    ["null", { alternativeText: null }],
    ["a number", { alternativeText: 42 }],
    ["an object", { alternativeText: {} }],
    ["blank", { alternativeText: "" }],
    ["whitespace only", { alternativeText: "   " }],
  ])(
    "uses the fallback when runtime alternative text is %s",
    async (_description, alternativeTextInput) => {
      await renderPublicLogin({
        ...baseBranding,
        customerLogo: {
          bundledAssetSource: "/assets/example-product-logo.webp",
          ...alternativeTextInput,
        },
      });

      expect(screen.queryByRole("img")).not.toBeInTheDocument();
      expect(screen.getByTestId("orca-neutral-mark")).toBeVisible();
    },
  );

  it.each([
    ["absent", {}],
    ["null", { bundledAssetSource: null }],
    ["a number", { bundledAssetSource: 42 }],
    ["an object", { bundledAssetSource: {} }],
    ["blank", { bundledAssetSource: "" }],
    ["whitespace only", { bundledAssetSource: "   " }],
    [
      "remote HTTPS",
      { bundledAssetSource: "https://customer.example/logo.png" },
    ],
    ["protocol relative", { bundledAssetSource: "//customer.example/logo.png" }],
    ["remote FTP", { bundledAssetSource: "ftp://customer.example/logo.png" }],
    [
      "inline data",
      { bundledAssetSource: "data:image/png;base64,unsafe-inline-data" },
    ],
    ["SVG", { bundledAssetSource: "/assets/customer-logo.svg" }],
    [
      "an unsupported format",
      { bundledAssetSource: "/assets/customer-logo.gif" },
    ],
  ])(
    "uses the fallback when runtime bundled source is %s",
    async (_description, bundledSourceInput) => {
      await renderPublicLogin({
        ...baseBranding,
        customerLogo: {
          alternativeText: "Example Product logo",
          ...bundledSourceInput,
        },
      });

      expect(screen.queryByRole("img")).not.toBeInTheDocument();
      expect(screen.getByTestId("orca-neutral-mark")).toBeVisible();
    },
  );

  it.each([
    ["PNG", "/assets/example-product-logo.png"],
    ["WebP", "/assets/example-product-logo.webp"],
  ])(
    "continues to render a valid bundled %s logo",
    async (_format, bundledAssetSource) => {
      await renderPublicLogin({
        ...baseBranding,
        customerLogo: {
          bundledAssetSource,
          alternativeText: "  Example Product logo  ",
          unexpectedProperty: "must remain inert",
        },
        hideAttribution: true,
      });

      expect(
        screen.getByRole("img", { name: "Example Product logo" }),
      ).toHaveAttribute("src", bundledAssetSource);
      expect(
        screen.getByRole("link", { name: "Powered by Orca" }),
      ).toBeVisible();
      expect(screen.getByText("© 2026 Chen Chih-hao")).toBeVisible();
    },
  );

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

async function renderPublicLogin(branding: unknown) {
  const publicPackage = (await import(
    /* @vite-ignore */ packageName
  )) as PublicPackage;

  return render(createElement(publicPackage.OrcaLogin, { branding }));
}
