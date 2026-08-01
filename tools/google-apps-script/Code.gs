/**
 * Google Form -> GitHub issue bridge for The Chosen Quest Enhanced.
 *
 * Install this script in the private response Sheet. Store GITHUB_TOKEN,
 * GITHUB_OWNER, and GITHUB_REPO in Apps Script Properties; never paste a token
 * into this file or the Sheet.
 */
function onBugReportSubmit(event) {
  if (!event || !event.range || !event.namedValues) {
    throw new Error("This function must run from an installable spreadsheet form-submit trigger.");
  }

  var lock = LockService.getScriptLock();
  lock.waitLock(30000);
  try {
    var sheet = event.range.getSheet();
    var row = event.range.getRow();
    var columns = ensureAutomationColumns_(sheet);
    var existingIssue = sheet.getRange(row, columns.issue).getDisplayValue();
    if (existingIssue) return;

    var config = loadConfig_();
    var answers = normalizedAnswers_(event.namedValues);
    var title = firstAnswer_(answers, [
      "bug title", "title", "short summary", "summary"
    ], "Beta tester report");
    var description = firstAnswer_(answers, [
      "description", "what happened", "describe the bug"
    ], "No description supplied.");
    var steps = firstAnswer_(answers, [
      "steps to reproduce", "reproduction steps", "how can we reproduce this"
    ], "No reproduction steps supplied.");
    var expected = firstAnswer_(answers, [
      "expected result", "what did you expect to happen"
    ], "Not supplied.");
    var actual = firstAnswer_(answers, [
      "actual result", "what actually happened"
    ], description);
    var severity = firstAnswer_(answers, [
      "severity", "impact", "how serious is this bug"
    ], "Unspecified");
    var version = firstAnswer_(answers, [
      "game version", "version", "build"
    ], "Unspecified");
    var operatingSystem = firstAnswer_(answers, [
      "operating system", "os", "platform"
    ], "Unspecified");
    var diagnostics = firstAnswer_(answers, [
      "diagnostics", "technical details", "system information"
    ], "Not supplied.");

    var body = [
      "## Tester report",
      "",
      actual,
      "",
      "## Steps to reproduce",
      "",
      steps,
      "",
      "## Expected result",
      "",
      expected,
      "",
      "## Environment",
      "",
      "- Version: " + version,
      "- Operating system: " + operatingSystem,
      "- Severity: " + severity,
      "",
      "## Privacy-safe diagnostics",
      "",
      "```text",
      diagnostics,
      "```",
      "",
      "_Created automatically from the private beta bug-report form. " +
        "Tester name and contact fields are intentionally excluded._"
    ].join("\n");

    var labels = ["tester-report", "needs-triage"];
    var severityLabel = severityLabel_(severity);
    if (severityLabel) labels.push(severityLabel);

    var response = UrlFetchApp.fetch(
      "https://api.github.com/repos/" + encodeURIComponent(config.owner) + "/" +
        encodeURIComponent(config.repo) + "/issues",
      {
        method: "post",
        contentType: "application/json",
        headers: {
          Authorization: "Bearer " + config.token,
          Accept: "application/vnd.github+json",
          "X-GitHub-Api-Version": "2022-11-28"
        },
        payload: JSON.stringify({
          title: "[Beta] " + sanitizeLine_(title),
          body: body,
          labels: labels
        }),
        muteHttpExceptions: true
      }
    );

    var status = response.getResponseCode();
    var payload = JSON.parse(response.getContentText() || "{}");
    if (status >= 200 && status < 300 && payload.html_url) {
      sheet.getRange(row, columns.issue).setValue(payload.html_url);
      sheet.getRange(row, columns.status).setValue("Created " + new Date().toISOString());
    } else {
      var safeMessage = payload.message || ("GitHub HTTP " + status);
      sheet.getRange(row, columns.status).setValue("Failed: " + safeMessage);
      throw new Error(safeMessage);
    }
  } finally {
    lock.releaseLock();
  }
}

function loadConfig_() {
  var properties = PropertiesService.getScriptProperties();
  var config = {
    token: properties.getProperty("GITHUB_TOKEN"),
    owner: properties.getProperty("GITHUB_OWNER") || "Rebelord",
    repo: properties.getProperty("GITHUB_REPO") || "The-Chosen-Quest-Enhanced"
  };
  if (!config.token) {
    throw new Error("Missing GITHUB_TOKEN in Apps Script Properties.");
  }
  return config;
}

function ensureAutomationColumns_(sheet) {
  var lastColumn = Math.max(1, sheet.getLastColumn());
  var headers = sheet.getRange(1, 1, 1, lastColumn).getDisplayValues()[0];
  var issue = headers.indexOf("GitHub Issue") + 1;
  var status = headers.indexOf("Automation Status") + 1;
  if (!issue) {
    issue = ++lastColumn;
    sheet.getRange(1, issue).setValue("GitHub Issue");
  }
  if (!status) {
    status = ++lastColumn;
    sheet.getRange(1, status).setValue("Automation Status");
  }
  return {issue: issue, status: status};
}

function normalizedAnswers_(namedValues) {
  var result = {};
  Object.keys(namedValues).forEach(function (key) {
    result[key.toLowerCase().trim()] = namedValues[key];
  });
  return result;
}

function firstAnswer_(answers, aliases, fallback) {
  for (var index = 0; index < aliases.length; index++) {
    var value = answers[aliases[index]];
    if (value && value.length && String(value[0]).trim()) {
      return String(value[0]).trim().substring(0, 12000);
    }
  }
  return fallback;
}

function severityLabel_(severity) {
  var value = String(severity || "").toLowerCase();
  if (value.indexOf("critical") >= 0 || value.indexOf("block") >= 0)
    return "severity: critical";
  if (value.indexOf("high") >= 0 || value.indexOf("major") >= 0)
    return "severity: high";
  if (value.indexOf("low") >= 0 || value.indexOf("minor") >= 0)
    return "severity: low";
  return "";
}

function sanitizeLine_(value) {
  return String(value || "Beta tester report").replace(/[\r\n]+/g, " ").substring(0, 180);
}
