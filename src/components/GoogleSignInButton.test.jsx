import { render, screen, waitFor } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";
import api from "../api/axiosInstance";
import GoogleSignInButton from "./GoogleSignInButton";

vi.mock("../api/axiosInstance", () => ({ default: { get: vi.fn() } }));

// Stands in for Google's script, which isn't loaded in tests.
function fakeGoogle() {
  const id = { initialize: vi.fn(), renderButton: vi.fn() };
  window.google = { accounts: { id } };
  return id;
}

describe("GoogleSignInButton", () => {
  afterEach(() => {
    delete window.google;
    vi.clearAllMocks();
  });

  it("shows nothing when Google sign-in isn't configured", async () => {
    api.get.mockResolvedValue({ data: { enabled: false, clientId: "" } });

    const { container } = render(<GoogleSignInButton onCredential={vi.fn()} />);

    await waitFor(() => expect(api.get).toHaveBeenCalledWith("/auth/google/config"));
    expect(container).toBeEmptyDOMElement();
  });

  it("sets Google up with our client ID and passes the credential on", async () => {
    const google = fakeGoogle();
    const onCredential = vi.fn();
    api.get.mockResolvedValue({ data: { enabled: true, clientId: "abc.apps.googleusercontent.com" } });

    render(<GoogleSignInButton onCredential={onCredential} />);

    await waitFor(() => expect(google.renderButton).toHaveBeenCalled());
    expect(screen.getByTestId("google-button")).toBeInTheDocument();

    const { client_id, callback } = google.initialize.mock.calls[0][0];
    expect(client_id).toBe("abc.apps.googleusercontent.com");

    callback({ credential: "google-id-token" });
    expect(onCredential).toHaveBeenCalledWith("google-id-token");
  });
});
