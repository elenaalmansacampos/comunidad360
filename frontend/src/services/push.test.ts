import { describe, expect, it } from "vitest";
import { pushSupported, urlBase64ToArrayBuffer } from "../services/push";

describe("push helpers", () => {
  it("decodes base64url into bytes", () => {
    const buffer = urlBase64ToArrayBuffer("AQID");
    expect(Array.from(new Uint8Array(buffer))).toEqual([1, 2, 3]);
  });

  it("handles url-safe characters and padding", () => {
    const buffer = urlBase64ToArrayBuffer("-_8");
    expect(Array.from(new Uint8Array(buffer))).toEqual([251, 255]);
  });

  it("reports unsupported in jsdom", () => {
    expect(pushSupported()).toBe(false);
  });
});
