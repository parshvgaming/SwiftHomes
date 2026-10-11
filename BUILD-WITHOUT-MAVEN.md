# Build SwiftHomes without installing Maven locally

This repository uses GitHub Actions to build the plugin in the cloud.

1. Create a GitHub repository and upload the contents of this `SwiftHomes` folder (including the hidden `.github` folder).
2. Open the repository's **Actions** tab and enable workflows if prompted.
3. Select **Build SwiftHomes** and click **Run workflow**, or push a commit to `main`/`master`.
4. Open the completed workflow run. Under **Artifacts**, download `SwiftHomes-test-jar`.
5. Extract the artifact ZIP and copy `SwiftHomes-1.0.0.jar` to your test server's `plugins/` folder.
6. Start the test server and inspect its console before trying commands.

## Build target

Paper API 1.21.11, Java 21. This targets the 1.21.x line, but the build and runtime compatibility must be verified. The workflow has not been run by the authoring environment.
