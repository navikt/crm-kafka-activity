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
   1. Define new Push Topics in [/.pushtopics](/.pushtopics) (files not required in repo, but practical for future changes)
   1. Publish Push Topics by running Apex code in both Salesforce [production](https://navdialog.lightning.force.com) and [preproduction](https://navdialog--preprod.lightning.force.com)
1. Add Kafka Topics
   1. Define new Kafka Topics for `prod` and `dev` in [/.topics](/.topics) (files not required in repo, but practical for future changes)
   1. Publish Kafka Topics
      - Make sure [naisdevice](https://doc.nais.io/device/install/) is setup correctly on your computer
      - Run `kubectl apply -f topic.yml` for each new topic
      <!-- - Make sure you have access to the projects ```team-dialog-dev``` and ```team-dialog-prod``` in [GCP](https://console.cloud.google.com)
      - Make sure naisdevice icon is green and that you're logged into Google Cloud (`gcloud auth login`) -->
1. Add a new config file in [/.nais](/.nais) for both `prod` and `dev`
1. Edit [deploy.yml](/.github/.workflows/deploy.yml)
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
