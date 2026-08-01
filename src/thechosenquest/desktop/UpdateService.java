package thechosenquest.desktop;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Checks published GitHub Releases without touching saves or installation files.
 *
 * GitHub's "latest" endpoint excludes prereleases, so the beta channel inspects
 * the published release list and applies semantic-version ordering locally.
 * Network failures are returned as ordinary results and must never block launch.
 */
final class UpdateService {
    enum Channel {
        STABLE,
        BETA;

        static Channel forVersion(String version) {
            return normalizeVersion(version).indexOf('-') >= 0 ? BETA : STABLE;
        }
    }

    enum Status {
        AVAILABLE,
        CURRENT,
        FAILED
    }

    static final class Release {
        final String version;
        final String tag;
        final String name;
        final String notes;
        final String releaseUrl;
        final String downloadUrl;
        final String sha256Digest;
        final boolean prerelease;

        Release(String version, String tag, String name, String notes,
                String releaseUrl, String downloadUrl, String sha256Digest,
                boolean prerelease) {
            this.version = version;
            this.tag = tag;
            this.name = name;
            this.notes = notes;
            this.releaseUrl = releaseUrl;
            this.downloadUrl = downloadUrl;
            this.sha256Digest = sha256Digest;
            this.prerelease = prerelease;
        }
    }

    static final class Result {
        final Status status;
        final Release release;
        final String message;

        private Result(Status status, Release release, String message) {
            this.status = status;
            this.release = release;
            this.message = message;
        }

        static Result available(Release release) {
            return new Result(Status.AVAILABLE, release,
                "A newer " + (release.prerelease ? "beta" : "stable") +
                " release is available.");
        }

        static Result current(Release release) {
            return new Result(Status.CURRENT, release,
                "You already have the newest release in this update channel.");
        }

        static Result failed(String message) {
            return new Result(Status.FAILED, null,
                message == null || message.length() == 0
                    ? "The update service could not be reached." : message);
        }
    }

    interface Transport {
        String get(String url) throws IOException;
    }

    private static final String RELEASES_API =
        "https://api.github.com/repos/Rebelord/The-Chosen-Quest-Enhanced/" +
        "releases?per_page=30";
    private final Transport transport;

    UpdateService() {
        this(new GitHubTransport());
    }

    UpdateService(Transport transport) {
        this.transport = transport;
    }

    Result check(String currentVersion, Channel channel) {
        try {
            List<Release> releases = parseReleases(transport.get(RELEASES_API));
            Release latest = selectLatest(releases, channel);
            if (latest == null) {
                return Result.failed("No published releases were found for the " +
                    channel.name().toLowerCase() + " channel.");
            }
            return compareVersions(latest.version, currentVersion) > 0
                ? Result.available(latest) : Result.current(latest);
        } catch (Exception exception) {
            return Result.failed("Unable to check for updates. You can continue " +
                "playing offline and try again later.");
        }
    }

    static Release selectLatest(List<Release> releases, Channel channel) {
        Release latest = null;
        for (Release release : releases) {
            if (release == null || release.version.length() == 0) continue;
            if (channel == Channel.STABLE && release.prerelease) continue;
            if (latest == null ||
                    compareVersions(release.version, latest.version) > 0) {
                latest = release;
            }
        }
        return latest;
    }

    static int compareVersions(String left, String right) {
        SemanticVersion a = SemanticVersion.parse(left);
        SemanticVersion b = SemanticVersion.parse(right);
        return a.compareTo(b);
    }

    @SuppressWarnings("unchecked")
    static List<Release> parseReleases(String json) {
        Object parsed = new JsonReader(json).read();
        if (!(parsed instanceof List)) return Collections.emptyList();
        List<Release> releases = new ArrayList<Release>();
        for (Object entry : (List<Object>) parsed) {
            if (!(entry instanceof Map)) continue;
            Map<String, Object> release = (Map<String, Object>) entry;
            if (booleanValue(release.get("draft"))) continue;
            String tag = stringValue(release.get("tag_name"));
            String version = normalizeVersion(tag);
            String downloadUrl = "";
            String digest = "";
            Object assetsValue = release.get("assets");
            if (assetsValue instanceof List) {
                for (Object assetValue : (List<Object>) assetsValue) {
                    if (!(assetValue instanceof Map)) continue;
                    Map<String, Object> asset = (Map<String, Object>) assetValue;
                    String assetName = stringValue(asset.get("name"));
                    if (assetName.toLowerCase().endsWith(".zip") &&
                            !assetName.toLowerCase().endsWith(".zip.sha256")) {
                        downloadUrl = stringValue(asset.get("browser_download_url"));
                        digest = stringValue(asset.get("digest"));
                        break;
                    }
                }
            }
            releases.add(new Release(version, tag,
                stringValue(release.get("name")),
                stringValue(release.get("body")),
                stringValue(release.get("html_url")),
                downloadUrl, digest,
                booleanValue(release.get("prerelease"))));
        }
        return releases;
    }

    private static String normalizeVersion(String version) {
        String normalized = version == null ? "" : version.trim();
        return normalized.startsWith("v") || normalized.startsWith("V")
            ? normalized.substring(1) : normalized;
    }

    private static String stringValue(Object value) {
        return value instanceof String ? (String) value : "";
    }

    private static boolean booleanValue(Object value) {
        return value instanceof Boolean && ((Boolean) value).booleanValue();
    }

    private static final class GitHubTransport implements Transport {
        public String get(String url) throws IOException {
            HttpURLConnection connection =
                (HttpURLConnection) new URL(url).openConnection();
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(7000);
            connection.setInstanceFollowRedirects(true);
            connection.setRequestProperty("Accept", "application/vnd.github+json");
            connection.setRequestProperty("User-Agent",
                "The-Chosen-Quest-Enhanced/" + AppVersion.VERSION);
            connection.setRequestProperty("X-GitHub-Api-Version", "2026-03-10");
            int response = connection.getResponseCode();
            if (response != HttpURLConnection.HTTP_OK) {
                connection.disconnect();
                throw new IOException("GitHub returned HTTP " + response);
            }
            InputStream stream = connection.getInputStream();
            try {
                BufferedReader reader = new BufferedReader(new InputStreamReader(
                    stream, StandardCharsets.UTF_8));
                StringBuilder body = new StringBuilder();
                char[] buffer = new char[4096];
                int count;
                while ((count = reader.read(buffer)) >= 0) {
                    body.append(buffer, 0, count);
                    if (body.length() > 4 * 1024 * 1024) {
                        throw new IOException("Update response is unexpectedly large");
                    }
                }
                return body.toString();
            } finally {
                try { stream.close(); } finally { connection.disconnect(); }
            }
        }
    }

    private static final class SemanticVersion
            implements Comparable<SemanticVersion> {
        final int[] core;
        final String[] prerelease;

        SemanticVersion(int[] core, String[] prerelease) {
            this.core = core;
            this.prerelease = prerelease;
        }

        static SemanticVersion parse(String value) {
            String normalized = normalizeVersion(value);
            int build = normalized.indexOf('+');
            if (build >= 0) normalized = normalized.substring(0, build);
            String[] split = normalized.split("-", 2);
            String[] coreParts = split[0].split("\\.");
            int[] core = new int[Math.max(3, coreParts.length)];
            for (int i = 0; i < coreParts.length; i++) {
                core[i] = numericValue(coreParts[i]);
            }
            String[] prerelease = split.length > 1 && split[1].length() > 0
                ? split[1].split("\\.") : new String[0];
            return new SemanticVersion(core, prerelease);
        }

        public int compareTo(SemanticVersion other) {
            int length = Math.max(core.length, other.core.length);
            for (int i = 0; i < length; i++) {
                int left = i < core.length ? core[i] : 0;
                int right = i < other.core.length ? other.core[i] : 0;
                if (left != right) return left < right ? -1 : 1;
            }
            if (prerelease.length == 0 && other.prerelease.length == 0) return 0;
            if (prerelease.length == 0) return 1;
            if (other.prerelease.length == 0) return -1;
            int identifiers = Math.max(prerelease.length, other.prerelease.length);
            for (int i = 0; i < identifiers; i++) {
                if (i >= prerelease.length) return -1;
                if (i >= other.prerelease.length) return 1;
                String left = prerelease[i];
                String right = other.prerelease[i];
                boolean leftNumeric = isNumeric(left);
                boolean rightNumeric = isNumeric(right);
                int comparison;
                if (leftNumeric && rightNumeric) {
                    comparison = Integer.compare(numericValue(left), numericValue(right));
                } else if (leftNumeric != rightNumeric) {
                    comparison = leftNumeric ? -1 : 1;
                } else {
                    comparison = left.compareToIgnoreCase(right);
                }
                if (comparison != 0) return comparison;
            }
            return 0;
        }

        private static boolean isNumeric(String value) {
            if (value.length() == 0) return false;
            for (int i = 0; i < value.length(); i++) {
                if (!Character.isDigit(value.charAt(i))) return false;
            }
            return true;
        }

        private static int numericValue(String value) {
            try {
                return Integer.parseInt(value.replaceAll("[^0-9].*$", ""));
            } catch (RuntimeException ignored) {
                return 0;
            }
        }
    }

    /** Minimal JSON reader keeps the Java 8 distribution dependency-free. */
    private static final class JsonReader {
        private final String source;
        private int index;

        JsonReader(String source) {
            this.source = source == null ? "" : source;
        }

        Object read() {
            Object value = readValue();
            skipWhitespace();
            if (index != source.length()) throw error("Unexpected trailing data");
            return value;
        }

        private Object readValue() {
            skipWhitespace();
            if (index >= source.length()) throw error("Unexpected end of JSON");
            char character = source.charAt(index);
            if (character == '{') return readObject();
            if (character == '[') return readArray();
            if (character == '"') return readString();
            if (character == 't') return readLiteral("true", Boolean.TRUE);
            if (character == 'f') return readLiteral("false", Boolean.FALSE);
            if (character == 'n') return readLiteral("null", null);
            return readNumber();
        }

        private Map<String, Object> readObject() {
            Map<String, Object> object = new LinkedHashMap<String, Object>();
            index++;
            skipWhitespace();
            if (consume('}')) return object;
            do {
                skipWhitespace();
                String key = readString();
                skipWhitespace();
                require(':');
                object.put(key, readValue());
                skipWhitespace();
            } while (consume(','));
            require('}');
            return object;
        }

        private List<Object> readArray() {
            List<Object> array = new ArrayList<Object>();
            index++;
            skipWhitespace();
            if (consume(']')) return array;
            do {
                array.add(readValue());
                skipWhitespace();
            } while (consume(','));
            require(']');
            return array;
        }

        private String readString() {
            require('"');
            StringBuilder value = new StringBuilder();
            while (index < source.length()) {
                char character = source.charAt(index++);
                if (character == '"') return value.toString();
                if (character != '\\') {
                    value.append(character);
                    continue;
                }
                if (index >= source.length()) throw error("Incomplete escape");
                char escaped = source.charAt(index++);
                switch (escaped) {
                    case '"': value.append('"'); break;
                    case '\\': value.append('\\'); break;
                    case '/': value.append('/'); break;
                    case 'b': value.append('\b'); break;
                    case 'f': value.append('\f'); break;
                    case 'n': value.append('\n'); break;
                    case 'r': value.append('\r'); break;
                    case 't': value.append('\t'); break;
                    case 'u':
                        if (index + 4 > source.length()) throw error("Bad unicode escape");
                        value.append((char) Integer.parseInt(
                            source.substring(index, index + 4), 16));
                        index += 4;
                        break;
                    default: throw error("Unsupported escape");
                }
            }
            throw error("Unterminated string");
        }

        private Object readNumber() {
            int start = index;
            while (index < source.length() &&
                    "-+0123456789.eE".indexOf(source.charAt(index)) >= 0) {
                index++;
            }
            if (start == index) throw error("Expected value");
            String number = source.substring(start, index);
            try {
                return number.indexOf('.') >= 0 || number.indexOf('e') >= 0 ||
                    number.indexOf('E') >= 0
                    ? Double.valueOf(number) : Long.valueOf(number);
            } catch (NumberFormatException exception) {
                throw error("Invalid number");
            }
        }

        private Object readLiteral(String literal, Object value) {
            if (!source.regionMatches(index, literal, 0, literal.length())) {
                throw error("Invalid literal");
            }
            index += literal.length();
            return value;
        }

        private void skipWhitespace() {
            while (index < source.length() &&
                    Character.isWhitespace(source.charAt(index))) index++;
        }

        private boolean consume(char expected) {
            if (index < source.length() && source.charAt(index) == expected) {
                index++;
                return true;
            }
            return false;
        }

        private void require(char expected) {
            if (!consume(expected)) throw error("Expected '" + expected + "'");
        }

        private IllegalArgumentException error(String message) {
            return new IllegalArgumentException(message + " at character " + index);
        }
    }
}
