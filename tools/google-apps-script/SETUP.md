# Beta bug form → GitHub issue automation

The script in `Code.gs` is installed in the **private Google Sheet linked to the
beta bug-report Form**. It creates sanitized GitHub issues without publishing
tester names or contact details.

## One-time setup

1. Open the bug-report response Sheet.
2. Choose **Extensions → Apps Script**.
3. Replace the editor contents with `Code.gs`, then save.
4. Open **Project Settings → Script Properties** and add:
   - `GITHUB_OWNER` = `Rebelord`
   - `GITHUB_REPO` = `The-Chosen-Quest-Enhanced`
   - `GITHUB_TOKEN` = a fine-grained token restricted to this repository with
     **Issues: Read and write** permission.
5. Open **Triggers → Add Trigger**:
   - Function: `onBugReportSubmit`
   - Event source: **From spreadsheet**
   - Event type: **On form submit**
6. Submit a disposable test response and confirm that:
   - one GitHub issue is created;
   - its URL appears in the `GitHub Issue` response column;
   - the issue contains no tester identity or contact information;
   - resubmitting/re-running the same response does not create a duplicate.

Do not commit, email, paste into chat, or store the GitHub token in the Sheet.
Rotate the token immediately if it is ever exposed.
