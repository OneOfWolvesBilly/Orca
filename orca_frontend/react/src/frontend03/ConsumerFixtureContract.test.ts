import { readdir, readFile, stat } from "node:fs/promises";
import { extname, resolve } from "node:path";
import { cwd } from "node:process";
import { describe, expect, it } from "vitest";

const fixtureDirectory = resolve(cwd(), "../../minimal_consumer_fixture/react");
const publicPackageName = "@oneofwolvesbilly/orca-react-login";

describe("frontend-03 React Minimal Consumer Fixture contract", () => {
  it("declares a normal repository-local dependency on the public package", async () => {
    const manifest = JSON.parse(
      await readFile(`${fixtureDirectory}/package.json`, "utf8"),
    ) as { dependencies?: Record<string, string> };

    expect(manifest.dependencies?.[publicPackageName]).toBe(
      "file:../../orca_frontend/packages/react-login",
    );
  });

  it("imports only the package root and contains no copied login implementation", async () => {
    const source = await readFixtureSource();

    expect(source).toContain(`from "${publicPackageName}"`);
    expect(source).not.toMatch(
      /@oneofwolvesbilly\/orca-react-login\/(src|api|components|errors)/,
    );
    expect(source).not.toContain("function submitLogin");
    expect(source).not.toContain("function parseStableApiError");
    expect(source).not.toContain("function recordClientDiagnostic");
  });

  it("bundles one compliant PNG or WebP customer logo", async () => {
    const assetDirectory = `${fixtureDirectory}/src/assets`;
    const assets = await readdir(assetDirectory);
    const logos = assets.filter((asset) => [".png", ".webp"].includes(extname(asset)));

    expect(logos).toHaveLength(1);
    const logo = await stat(`${assetDirectory}/${logos[0]}`);
    expect(logo.size).toBeLessThanOrEqual(256 * 1024);
  });

  it("contains no protected session lifecycle behavior", async () => {
    const source = await readFixtureSource();

    expect(source).not.toContain("/api/fixture/actor-context-check");
    expect(source).not.toContain("/api/auth/logout");
    expect(source).not.toContain("ORCA_SESSION");
    expect(source).not.toContain("AuthenticatedActor");
  });
});

async function readFixtureSource(): Promise<string> {
  const sourceDirectory = `${fixtureDirectory}/src`;
  const entries = await readdir(sourceDirectory, { recursive: true });
  const sourceFiles = entries.filter((entry) => /\.(ts|tsx)$/.test(entry));
  const contents = await Promise.all(
    sourceFiles.map((entry) => readFile(`${sourceDirectory}/${entry}`, "utf8")),
  );
  return contents.join("\n");
}
