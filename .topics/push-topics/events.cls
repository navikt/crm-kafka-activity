String PUSH_TOPIC_NAME = 'kafka-events';              // Keep 'kafka-' but change what comes after to your preferred and unique push topic name
String SOBJECT_NAME = 'EventShadow__c';                    // Change to the Salesforce Object name
List<String> FIELDS = new List<String> { 'Id', 'LastModifiedById', 'SH_AccountOrgNumber__c', 'SH_AccountParentId__c', 'SH_AccountParentOrgNumber__c', 'SH_ActivityDateTime__c', 'SH_ActivityType__c', 'SH_CreatedById__c', 'SH_DateActivity__c', 'SH_Date__c', 'SH_DurationTime__c', 'SH_EndDateTime__c', 'SH_EventId__c', 'SH_EventSubtype__c', 'SH_LastModifiedById__c', 'SH_OwnerId__c', 'SH_Region__c', 'SH_RelatedToAccount__c', 'SH_Service__c', 'SH_StartDateTime__c', 'SH_Status__c', 'SH_Subject__c', 'SH_TaskEvent__c', 'SH_Type__c', 'SH_Unit__c', 'SH_WhatId__c', 'SH_WhoId__c' };    // Add fields to the list that should be published to Kafka (be vary of fields that might contain PII)
String PARAMETERS = '';       // After a WHERE clause. Empty or null to fetch all records

// --------------------------------------------------
// ---------- DO NOT CHANGE ANYTHING BELOW ----------
// --------------------------------------------------

List<PushTopic> oldVersion = [SELECT Id FROM PushTopic WHERE Name = : PUSH_TOPIC_NAME LIMIT 1];

PushTopic pushTopic = new PushTopic(ApiVersion = 47.0, NotifyForOperationCreate = true, NotifyForOperationUpdate = true, NotifyForOperationUndelete = true, NotifyForOperationDelete = true, IsActive = true, Description = 'DO NOT UPDATE! SOQL is maintained in https://github.com/navikt/crm-kafka-activity.');
pushTopic.Name = PUSH_TOPIC_NAME;
pushTopic.Id = oldVersion.size() > 0 ? oldVersion[0].Id : null;
pushTopic.Query =   ' SELECT ' + 
                    String.join(FIELDS, ', ') + 
                    ' FROM ' + SOBJECT_NAME + 
                    (String.isBlank(PARAMETERS) ? '' : ' WHERE ' + PARAMETERS);

upsert pushTopic;