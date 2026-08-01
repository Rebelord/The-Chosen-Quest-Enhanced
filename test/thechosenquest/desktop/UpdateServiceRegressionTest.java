package thechosenquest.desktop;

import java.io.IOException;
import java.util.List;

/** Offline contract tests for update parsing, channels, and semantic versions. */
public final class UpdateServiceRegressionTest {
    private static final String RELEASES =
        "[" +
        "{\"tag_name\":\"v0.7.0-beta.2\",\"name\":\"Beta 0.7\"," +
        "\"body\":\"## Changes\\n- New map\",\"draft\":false," +
        "\"prerelease\":true,\"html_url\":\"https://example.test/beta2\"," +
        "\"assets\":[{\"name\":\"game.zip.sha256\"," +
        "\"browser_download_url\":\"https://example.test/checksum\"}," +
        "{\"name\":\"game.zip\",\"digest\":\"sha256:abc\"," +
        "\"browser_download_url\":\"https://example.test/game.zip\"}]}," +
        "{\"tag_name\":\"v0.6.0\",\"name\":\"Stable 0.6\",\"body\":\"Stable\"," +
        "\"draft\":false,\"prerelease\":false," +
        "\"html_url\":\"https://example.test/stable\",\"assets\":[]}," +
        "{\"tag_name\":\"v9.0.0\",\"name\":\"Draft\",\"body\":\"Hidden\"," +
        "\"draft\":true,\"prerelease\":false,\"assets\":[]}" +
        "]";

    private UpdateServiceRegressionTest() { }

    public static void main(String[] args) {
        assertGreater("0.7.0-beta.10", "0.7.0-beta.2");
        assertGreater("0.7.0", "0.7.0-beta.99");
        assertGreater("1.0.0-rc.1", "1.0.0-beta.12");
        assertEqual("v0.6.0-beta.1", "0.6.0-beta.1");

        List<UpdateService.Release> parsed = UpdateService.parseReleases(RELEASES);
        if (parsed.size() != 2) {
            throw new AssertionError("Draft releases must be excluded");
        }
        UpdateService.Release beta = UpdateService.selectLatest(
            parsed, UpdateService.Channel.BETA);
        UpdateService.Release stable = UpdateService.selectLatest(
            parsed, UpdateService.Channel.STABLE);
        if (beta == null || !"0.7.0-beta.2".equals(beta.version) ||
                !"https://example.test/game.zip".equals(beta.downloadUrl) ||
                !"sha256:abc".equals(beta.sha256Digest)) {
            throw new AssertionError(
                "Beta selection must retain the package URL and published digest");
        }
        if (stable == null || !"0.6.0".equals(stable.version) ||
                stable.prerelease) {
            throw new AssertionError("Stable channel must exclude prereleases");
        }

        UpdateService service = new UpdateService(new UpdateService.Transport() {
            public String get(String url) { return RELEASES; }
        });
        if (service.check("0.6.0-beta.1", UpdateService.Channel.BETA).status !=
                UpdateService.Status.AVAILABLE) {
            throw new AssertionError("Older beta builds must see the newer beta");
        }
        if (service.check("0.7.0-beta.2", UpdateService.Channel.BETA).status !=
                UpdateService.Status.CURRENT) {
            throw new AssertionError("Matching versions must report current");
        }

        UpdateService offline = new UpdateService(new UpdateService.Transport() {
            public String get(String url) throws IOException {
                throw new IOException("offline");
            }
        });
        if (offline.check(AppVersion.VERSION, UpdateService.Channel.BETA).status !=
                UpdateService.Status.FAILED) {
            throw new AssertionError("Offline checks must fail safely");
        }
        System.out.println("Update service regression tests passed.");
    }

    private static void assertGreater(String left, String right) {
        if (UpdateService.compareVersions(left, right) <= 0) {
            throw new AssertionError(left + " should be newer than " + right);
        }
    }

    private static void assertEqual(String left, String right) {
        if (UpdateService.compareVersions(left, right) != 0) {
            throw new AssertionError(left + " should equal " + right);
        }
    }
}
