# crm-kafka-activity

Framework to publish activity (data) from Salesforce to Kafka in GCP. Easily editable all-in-one repo which handles Salesforce SOQL queries, Kafka topics, retention policies and Kotlin code which listens to changes in Salesforce and publishes them to Salesforce as they occur.

# Topics (see [example data](/example-data))

- team-dialog.crm-kafka-activity-oppgaver
  <!-- - team-dialog.crm-kafka-activity-moter -->
  <!-- - team-dialog.crm-kafka-activity-kurs -->
  <!-- - team-dialog.crm-kafka-activity-interne-kontaktpersoner -->
  <!-- - team-dialog.crm-kafka-activity-bedriftsavtaler -->
  <!-- - team-dialog.crm-kafka-activity-kampanje -->

# Defining Topics

See [.topics](/.topics) to define new topics. Topics must be created and pushed to `main` before publishing to topics.

# Publishing to Topics

See [.nais](/.nais) to start publishing to a topic (remember to create topics first, see [Defining Topics](#defining-topics))

# Updating Secrets in GCP

Google Secret Manager is used to maintain service users for Salesforce production and preproduction environments. To deploy push topics and nais configurations, some secrets are stored in GitHub. Everything related to Kafka and the nais platform are handled by the nais platform itself.

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
