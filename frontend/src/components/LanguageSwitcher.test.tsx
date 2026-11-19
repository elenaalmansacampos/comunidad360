import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { beforeEach, describe, expect, it } from "vitest";
import i18n from "../locales/i18n";
import { useAuthStore } from "../features/auth/authStore";
import { LanguageSwitcher } from "./LanguageSwitcher";

describe("LanguageSwitcher", () => {
  beforeEach(async () => {
    useAuthStore.setState({ status: "anonymous", user: null });
    await i18n.changeLanguage("es");
  });

  it("changes the interface language", async () => {
    const user = userEvent.setup();
    render(<LanguageSwitcher />);

    await user.selectOptions(screen.getByRole("combobox", { name: "Idioma" }), "ca");

    expect(i18n.language).toBe("ca");
  });
});
