import { describe, expect, it } from "vitest";
import { canManageIncidents, isActiveIncident, nextIncidentStatuses } from "./utils";

describe("incident utils", () => {
  it("detects roles that can manage incidents", () => {
    expect(canManageIncidents(["OWNER"])).toBe(false);
    expect(canManageIncidents(["SECRETARY"])).toBe(true);
    expect(canManageIncidents(["PRESIDENT"])).toBe(true);
  });

  it("detects active incidents", () => {
    expect(isActiveIncident("OPEN")).toBe(true);
    expect(isActiveIncident("CLOSED")).toBe(false);
  });

  it("returns allowed next statuses", () => {
    expect(nextIncidentStatuses("OPEN")).toContain("IN_PROGRESS");
    expect(nextIncidentStatuses("CLOSED")).toHaveLength(0);
  });
});
