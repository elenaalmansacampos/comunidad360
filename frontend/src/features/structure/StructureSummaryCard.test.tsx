import { render, screen } from "@testing-library/react";
import { beforeEach, describe, expect, it } from "vitest";
import i18n from "../../locales/i18n";
import { StructureSummaryCard } from "./StructureSummaryCard";

describe("StructureSummaryCard", () => {
  beforeEach(async () => {
    await i18n.changeLanguage("es");
  });

  it("shows a valid coefficient summary", () => {
    render(
      <StructureSummaryCard
        summary={{
          coefficientSum: "100.000000",
          tolerance: "0.0001",
          valid: true,
          totalUnits: 3,
          activeUnits: 3,
          buildings: 1,
          commonElements: 2,
        }}
      />,
    );

    expect(screen.getByText("Suma de coeficientes")).toBeInTheDocument();
    expect(screen.getByText(/100,00/)).toBeInTheDocument();
    expect(screen.getByText("Válida")).toBeInTheDocument();
    expect(screen.getByText("3")).toBeInTheDocument();
  });
});
