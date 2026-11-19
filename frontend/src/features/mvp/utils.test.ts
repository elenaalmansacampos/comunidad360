import { describe, expect, it } from "vitest";
import { canManageProviders, canManageVotings, canModerateChat, canViewProviders, formatChatTime } from "./utils";

describe("MVP+ utils", () => {
  it("detects management permissions", () => {
    expect(canManageProviders(["TREASURER"])).toBe(true);
    expect(canManageProviders(["OWNER"])).toBe(false);
    expect(canViewProviders(["SECRETARY"])).toBe(true);
    expect(canViewProviders(["OWNER"])).toBe(false);
    expect(canManageVotings(["SECRETARY"])).toBe(true);
    expect(canModerateChat(["PRESIDENT"])).toBe(true);
    expect(canModerateChat(["TENANT"])).toBe(false);
  });

  it("formats chat time", () => {
    expect(formatChatTime("2026-11-19T10:30:00Z", "es")).toMatch(/\d{2}:\d{2}/);
  });
});
