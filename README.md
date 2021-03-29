# crm-kafka-activity

Publishes Salesforce activity to the Kafka platform in GCP. The following Kafka topics are currently being produced by this repo:

- crm-kafka-activity-oppgaver
- crm-kafka-activity-moter
<!-- - crm-kafka-activity-kurs
- crm-kafka-activity-interne-kontaktpersoner
- crm-kafka-activity-bedriftsavtaler
- crm-kafka-activity-kampanje -->

See [example-data](/example-data) for the data that's being published.

# Adding Topics

1. Add Push Topics
   1. Define new Push Topics in [.push-topics](/.push-topics) (files not required in repo, but practical for future changes)
   1. Publish Push Topics by running Apex code in both Salesforce [production](https://navdialog.lightning.force.com) and [preproduction](https://navdialog--preprod.lightning.force.com)
1. Add Kafka Topics
   1. Define new Kafka Topics for `prod` and `dev` in [.kafka-topics](/.kafka-topics) (files not required in repo, but practical for future changes)
   1. Publish Kafka Topics
      - Make sure [naisdevice](https://doc.nais.io/device/install/) is setup correctly on your computer
      - Make sure naisdevice icon is green
      - Make sure you're in the correct context using `kubectl config use-context [ENV]`, with env `dev-gcp` or `prod-gcp`
      - Make sure you're logged into Google Cloud locally using `gcloud auth login`
      - Run `kubectl apply -f .kafka-topics/topic.yml` for each new topic
1. Add a new config file in [.nais](/.nais) for both `prod` and `dev`
1. Edit [deploy-nais.yml](/.github/.workflows/deploy-nais.yml)
   - Add the new nais config file name to the `namespace` array in both `deploy-dev` and `deploy-prod`
   - Make sure to **not** include `dev-` and `prod-` in the `namespace` array

# Updating Secrets in GCP

1. Go to [Google Secret Manager](https://console.cloud.google.com/security/secret-manager)
1. Choose project `team-dialog-dev` or `team-dialog-prod`
1. On `emp-login` → Click the three dots to the right → `Add new version`
1. Add username and password with the following format:

```javascript
EMP_USERNAME=[salesforce_username]
EMP_PASSWORD=[password + security token]
```
