# crm-kafka-activity

Publishes Salesforce activity to the Kafka platform in GCP. The following Kafka topics are currently being produced by this repo:

- team-dialog.crm-kafka-activity-oppgaver
  <!-- - team-dialog.crm-kafka-activity-moter -->
  <!-- - team-dialog.crm-kafka-activity-kurs -->
  <!-- - team-dialog.crm-kafka-activity-interne-kontaktpersoner -->
  <!-- - team-dialog.crm-kafka-activity-bedriftsavtaler -->
  <!-- - team-dialog.crm-kafka-activity-kampanje -->

See [example-data](/example-data) for the data that's being published.

# Defining Topics

See [.topics](/.topics) to define new topics, a

# Publishing to Topics

See [.nais](/.nais) to start publishing to a topic (remember to create topics first, see [Defining Topics](#defining-topics))

# Updating Secrets in GCP

1. Go to [Google Secret Manager](https://console.cloud.google.com/security/secret-manager)
1. Choose project `team-dialog-dev` or `team-dialog-prod`
1. On `emp-login` → Click the three dots to the right → `Add new version`
1. Add username and password with the following format:

```
EMP_USERNAME=salesforce_service_user
EMP_PASSWORD=password+securitytoken
```
