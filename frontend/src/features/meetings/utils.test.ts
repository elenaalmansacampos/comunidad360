import { describe, expect, it } from "vitest";
import { canManageMeetings, nextMeetingStatuses } from "./utils";

describe("meetings utils", () => {
  it("detects meeting management permissions", () => {
    expect(canManageMeetings(["SECRETARY"])).toBe(true);
    expect(canManageMeetings(["OWNER"])).toBe(false);
  });

  it("returns valid status transitions", () => {
    expect(nextMeetingStatuses("DRAFT")).toEqual(["CONVENED", "CANCELLED"]);
    expect(nextMeetingStatuses("CONVENED")).toEqual(["HELD", "CANCELLED"]);
    expect(nextMeetingStatuses("HELD")).toEqual([]);
  });
});
