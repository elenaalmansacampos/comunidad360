import AxeBuilder from "@axe-core/playwright";
import { expect, test } from "@playwright/test";

const email = process.env.E2E_EMAIL ?? "admin@comunidad360.local";
const password = process.env.E2E_PASSWORD ?? "Admin1234!";

async function login(page: import("@playwright/test").Page) {
  await page.goto("/login");
  await page.getByLabel("Email").fill(email);
  await page.getByLabel("Contraseña", { exact: true }).fill(password);
  await page.getByRole("button", { name: "Iniciar sesión" }).click();
  await expect(page).toHaveURL(/\/communities\/[^/]+\/dashboard/);
  await expect(page.getByRole("heading", { name: "Resumen de la comunidad" })).toBeVisible();
}

test("login, dashboard and critical navigation work", async ({ page }) => {
  await login(page);
  const links = ["Estructura", "Finanzas", "Banca", "Incidencias", "Documentos", "Reuniones", "Chat", "Votaciones"];
  for (const link of links) {
    await page.getByRole("link", { name: link, exact: true }).first().click();
    await expect(page.locator("main")).toBeVisible();
  }
  await page.getByRole("link", { name: "Inicio", exact: true }).first().click();
  await expect(page.getByRole("heading", { name: "Resumen de la comunidad" })).toBeVisible();
});

test("login and dashboard have no critical accessibility violations", async ({ page }) => {
  await page.goto("/login");
  const loginResults = await new AxeBuilder({ page }).withTags(["wcag2a", "wcag2aa"]).analyze();
  const loginCritical = loginResults.violations.filter((violation) =>
    ["critical", "serious"].includes(violation.impact ?? ""));
  expect(loginCritical, JSON.stringify(loginCritical, null, 2)).toEqual([]);

  await login(page);
  const dashboardResults = await new AxeBuilder({ page }).withTags(["wcag2a", "wcag2aa"]).analyze();
  const dashboardCritical = dashboardResults.violations.filter((violation) =>
    ["critical", "serious"].includes(violation.impact ?? ""));
  expect(dashboardCritical, JSON.stringify(dashboardCritical, null, 2)).toEqual([]);
});

test.describe("mobile", () => {
  test.use({ viewport: { width: 390, height: 844 } });

  test("mobile login, dashboard and navigation work", async ({ page }) => {
    await login(page);
    await page.locator(".mobile-nav").getByRole("link", { name: "Incidencias", exact: true }).click();
    await expect(page.getByRole("heading", { name: "Incidencias", exact: true })).toBeVisible();
    const results = await new AxeBuilder({ page }).withTags(["wcag2a", "wcag2aa"]).analyze();
    const critical = results.violations.filter((violation) =>
      ["critical", "serious"].includes(violation.impact ?? ""));
    expect(critical, JSON.stringify(critical, null, 2)).toEqual([]);
  });
});
