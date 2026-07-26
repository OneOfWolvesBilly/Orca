import { readFile } from "node:fs/promises";
import { resolve } from "node:path";
import { cwd } from "node:process";
import { describe, expect, it } from "vitest";

const packageDirectory = resolve(cwd(), "../packages/react-login");
const packageName = "@oneofwolvesbilly/orca-react-login";

describe("frontend-03 public package contract", () => {
  it("declares the supported package name and root-only export map", async () => {
    const manifest = JSON.parse(
      await readFile(`${packageDirectory}/package.json`, "utf8"),
    ) as {
      name?: string;
      exports?: Record<string, unknown>;
    };

    expect(manifest.name).toBe(packageName);
    expect(Object.keys(manifest.exports ?? {})).toEqual(["."]);
  });

  it("exports the approved login and session coordination surface from the package root", async () => {
    const publicPackage = await importPublicPackage();

    expect(Object.keys(publicPackage).sort()).toEqual([
      "OrcaLogin",
      "OrcaLoginBranding",
      "logoutOrcaSession",
    ]);
  });

  it.each([
    `${packageName}/src/api/login`,
    `${packageName}/src/api/clientDiagnostics`,
    `${packageName}/src/components/LoginForm`,
  ])("does not resolve the internal import %s", async (internalPath) => {
    await expect(importModule(internalPath)).rejects.toBeDefined();
  });
});

function importPublicPackage(): Promise<Record<string, unknown>> {
  return importModule(packageName);
}

function importModule(moduleName: string): Promise<Record<string, unknown>> {
  return import(/* @vite-ignore */ moduleName) as Promise<Record<string, unknown>>;
}
