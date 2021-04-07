# crm-kafka-activity

Framework to publish activity (data) from Salesforce to Kafka in GCP. Easily editable all-in-one repo which handles Salesforce SOQL queries, Kafka topics, retention policies and Kotlin code which listens to changes in Salesforce and publishes them to Salesforce as they occur.

# Available Topics (see [example data](/example-data))

- `team-dialog`.`crm-kafka-activity-oppgaver`
- `team-dialog`.`crm-kafka-activity-events`
  <!-- - `team-dialog`.`crm-kafka-activity-kurs` -->
  <!-- - `team-dialog`.`crm-kafka-activity-interne-kontaktpersoner` -->
  <!-- - `team-dialog`.`crm-kafka-activity-bedriftsavtaler` -->
  <!-- - `team-dialog`.`crm-kafka-activity-kampanje` -->

# Testing / Debugging Configurations

To create new Kafka streams or update existing streams with new data or fields, make your changes to a new branch. See [Defining Topics](#defining-topics) and [Publishing to Topics](#publishing-to-topics) how to make changes to configurations.

Pushing to any branch that is **not** the `main` branch will cause `navdialog--preprod` and `gcp-dev` to update only. When pushing changes to the `main` branch, changes will be applied to both prod and dev (`navdialog`, `navdialog--preprod`, `gcp-prod` and `gcp-dev`).

# Run / Build Project Locally

You can build the project locally to verify that the code works as intended. However, you cannot run the code locally as it is written currently because the code requires nais api's for both secrets and Kafka. For configuration changes only, it is recommended to push code to your branch to test in `navdialog--preprod` and `gcp-dev`. What you can run locally, though, is the EMP Connector from Salesforce to subscribe to a Push Topic.

## Setup Local Tools

Gradle is the building automation tool to build Kotlin. To build, make sure you have [AdoptOpenJDK 8 (HotSpot)](https://adoptopenjdk.net) installed, then install [Gradle](https://gradle.org/install/).

## Build Locally

By building the project, you can verify that [src/](/src) and [build.gradle](/build.gradle) are setup correctly (if you need to change those). To build the project, run `gradle build` from the root folder.

## Run Locally

The EMP Streaming API (EMP Connector) can be tested locally by commenting out `enableNAISAPI` in [Bootstrap.kt](/src/main/kotlin/no/nav/crm/kafka/activity/Bootstrap.kt) and hardcoding login info below it. The code inside `processData()` in [EMP.kt](/src/main/kotlin/no/nav/crm/kafka/activity/EMP.kt) will also need to be commented out, because Kafka is inaccessible locally. Then you can run the project by using `gradle run` or running the project inside IntelliJ.

# Defining Topics

See [.topics](/.topics) to define new topics.

Only the following changes will cause automatic deployment of topics:

- [.topics/push-topics/](/.topics/push-topics) (Salesforce Push Topic configurations)
- [.topics/kafka-topics/](/.topics/kafka-topics) (Kafka Topic configurations)

Only the newly added topics will be deployed, and edited topics will be re-deployed. However, deploying all topics have side effects.

# Publishing to Topics

See [.nais](/.nais) to start publishing to a topic.

Only the following changes will cause automatic deployment of nais pods:

- [.nais/](/.nais) (app configurations, one for each pod)
  - Only the newly added configurations will be deployed, and edited configurations will be re-deployed.
- [src/](/src) (Kotlin code)
  - All configurations (pods) are re-deployed by editing the code base
- [build.gradle](/build.gradle) (Kotlin dependencies)
  - All configurations (pods) are re-deployed by editing dependencies
- [Dockerfile](/Dockerfile) (OS Image)
  - All configurations (pods) are re-deployed by editing OS settings

**NOTE!** Re-deployment of apps means every record (for a given Push Topic) that is edited during the last 72 hours will be re-added into the Kafka stream. The `key` values is the same, so it might create duplicate rows. But because each change on a record is added to the Kafka queue anyway, it shouldn't be a problem for logic.

**<ins>Avoid re-deploying all pods unless absolutely necessary.</ins>**

# Updating Secrets in GCP

Google Secret Manager is used to maintain service users for Salesforce production (`navdialog`) and preproduction (`navdialog--preprod`) environments. To deploy push topics and nais configurations, some secrets are stored in GitHub. Everything related to Kafka and the nais platform are handled by the nais platform itself.

## Updating Google Secret Manager secrets

1. Go to [Google Secret Manager](https://console.cloud.google.com/security/secret-manager)
1. Choose project `team-dialog-dev` or `team-dialog-prod`
1. On `emp-login` → Click the three dots to the right → `Add new version`
1. Add username and password with the following format:

```
EMP_USERNAME=salesforce_service_user
EMP_PASSWORD=password+securitytoken
```

## Updating GitHub secrets

See [GitHub Secrets](https://github.com/navikt/crm-kafka-activity/settings/secrets/actions) (requires admin access to repo)

- `NAIS_DEPLOY_APIKEY`
  - Retrieve the key from [deploy.nais.io](https://deploy.nais.io/) → `API keys`
- `PROD_SFDX_URL`
  - Salesforce Production Auth URL (`crm-platform-team` on Slack)
- `PREPROD_SFDX_URL`
  - Salesforce Preproduction Auth URL (`crm-platform-team` on Slack)
