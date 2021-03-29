String PUSH_TOPIC_NAME = 'kafka-NAME';              // Keep 'kafka-' but change what comes after to your preferred and unique push topic name
String SOBJECT_NAME = 'SOBJECT';                    // Change to the Salesforce Object name
List<String> FIELDS = new List<String> { 'Id' };    // Add fields to the list that should be published to Kafka (be vary of fields that might contain PII)

// --------------------------------------------------
// ---------- DO NOT CHANGE ANYTHING BELOW ----------
// --------------------------------------------------

List<PushTopic> oldVersion = [SELECT Id FROM PushTopic WHERE Name = : PUSH_TOPIC_NAME LIMIT 1];

PushTopic pushTopic = new PushTopic(ApiVersion = 47.0, NotifyForOperationCreate = true, NotifyForOperationUpdate = true, NotifyForOperationUndelete = true, NotifyForOperationDelete = true, IsActive = true, Description = 'Used in the Kafka integration crm-kafka-acticity. See https://github.com/navikt/crm-kafka-activity. Please update any SOQL query in that repo.');
pushTopic.Name = PUSH_TOPIC_NAME;
pushTopic.Id = oldVersion.size() > 0 ? oldVersion[0].Id : null;
pushTopic.Query = 'SELECT ' + String.join(FIELDS, ', ') + ' FROM ' + SOBJECT_NAME;

upsert pushTopic;