# Temporary Cloud Run deployment

Use `Dockerfile.jvm` to build a Java 21 executable JAR for temporary hosting on Cloud Run. `cloudbuild.jvm.yaml` selects this Dockerfile explicitly and publishes the image to the `ailene` Artifact Registry repository in Singapore. The default `Dockerfile` and `railway.json` continue to select the native image for Railway. Building the JVM image needs no database credentials and skips tests that might connect to a database.

## Build in Cloud Shell

Enable billing and the Cloud Build, Artifact Registry, Cloud Run and Secret Manager APIs on the target project. Create a Docker-format Artifact Registry repository named `ailene` in `asia-southeast1` if it does not already exist. Clone the repository in Cloud Shell. If these deployment files are only available locally, upload `Dockerfile.jvm` and `cloudbuild.jvm.yaml` into the cloned repository's root through Cloud Shell Editor before running the build.

Run from the repository root:

```bash
gcloud builds submit . \
  --project=extreme-storm-462313-u9 \
  --region=us-central1 \
  --config=cloudbuild.jvm.yaml \
  --machine-type=e2-standard-2 \
  --async
```

The build runs in the US; the image and the Cloud Run service are in Singapore. The build config uses the selected project's ID in the image name. It does not use `--tag`, which would select the default native Dockerfile instead. Any earlier memory-limit edits to the default Dockerfile in Cloud Shell have no effect on this build.

Check the build using its returned ID:

```bash
gcloud builds describe BUILD_ID \
  --project=extreme-storm-462313-u9 \
  --region=us-central1 \
  --format='yaml(status,statusDetail,failureInfo,logUrl)'
```

## Configure the service

After the build succeeds, create a Cloud Run service using `asia-southeast1-docker.pkg.dev/extreme-storm-462313-u9/ailene/lms:temporary-jvm`. Select Singapore (`asia-southeast1`), request-based billing, 1 vCPU, 1 GiB RAM, minimum instances 0, maximum instances 2, concurrency 10, and port 8080. Allow public access so clients and QStash can reach the application's own authentication checks. The JVM image caps its heap at 65% of container memory, leaving space for the rest of the JVM; validate memory use under actual traffic before reducing the service limit.

Set the following before creating the first revision. Store credentials in Secret Manager and grant the service's runtime account access to the selected secret versions.

| Variable | Value |
|---|---|
| `DB_URL` | Existing JDBC PostgreSQL connection URL, including the existing SSL configuration |
| `DB_USERNAME`, `DB_PASSWORD` | Existing database credentials |
| `GOOGLE_OAUTH_ID` | Existing Google client ID |
| `SECRET_KEY` | Same value as Railway, to preserve existing JWT signatures |
| `DEEPSEEK_API_KEY` | Existing key for generation and grading |
| `QSTASH_URL`, `QSTASH_TOKEN` | Existing QStash configuration |
| `MAILTRAP_API_TOKEN` | Existing email sending token |
| `LMS_APP_URL` | Frontend URL used in invitation emails |

Cloud Run supplies `PORT` automatically. Keep the image's JVM `JAVA_TOOL_OPTIONS` unless intentionally tuning it; copying native-specific Railway options can break JVM startup. Keep `.env` out of the uploaded source and Docker image. The repository's `.gitignore` and `.dockerignore` exclude `.env`.

The application applies pending Flyway migrations at startup and validates the database schema. Its first start against a database can therefore change that database even before client traffic is redirected.

## Verify and switch traffic

Once the service URL is available, set `APP_BASE_URL` to that HTTPS URL and deploy the updated revision. New QStash jobs will use that URL. Existing delayed jobs retain their original destination and need to be handled separately if Railway is unavailable.

Check `/actuator/health` once, test login and an authenticated content request, then test the QStash callbacks before switching the frontend API base URL. Configure CORS at whichever layer already provides it if the new API hostname needs to be allowed. Avoid periodic database-backed health checks solely to keep the service warm, since these keep the database active.

To return to Railway, restore the frontend API base URL and its callback configuration, handle outstanding Cloud Run callback jobs, and delete the temporary Cloud Run service when it is no longer needed. Remove unused images and temporary secrets after confirming nothing still references them.
