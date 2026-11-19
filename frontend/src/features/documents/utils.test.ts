import { describe, expect, it } from "vitest";
import {
  canManageDocuments,
  canUploadDocuments,
  formatFileSize,
  isExternalUrl,
} from "./utils";

describe("documents utils", () => {
  it("detects upload and manage permissions", () => {
    expect(canManageDocuments(["SECRETARY"])).toBe(true);
    expect(canManageDocuments(["OWNER"])).toBe(false);
    expect(canUploadDocuments(["OWNER"])).toBe(true);
    expect(canUploadDocuments(["TENANT"])).toBe(false);
  });

  it("detects external storage URLs", () => {
    expect(isExternalUrl("http://localhost:9090/bucket/file.pdf")).toBe(true);
    expect(isExternalUrl("https://s3.example.com/file.pdf")).toBe(true);
    expect(isExternalUrl("/communities/1/documents/2/download")).toBe(false);
  });

  it("formats file sizes", () => {
    expect(formatFileSize(512, "es")).toBe("512 B");
    expect(formatFileSize(1536, "es")).toBe("1,5 KB");
  });
});
