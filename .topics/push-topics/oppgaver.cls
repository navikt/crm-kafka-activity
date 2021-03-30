String PUSH_TOPIC_NAME = 'kafka-oppgaver';              // Keep 'kafka-' but change what comes after to your preferred and unique push topic name
String SOBJECT_NAME = 'Task';                    // Change to the Salesforce Object name
List<String> FIELDS = new List<String> { 'Id', 'WhatId', 'WhoId', 'Subject', 'ActivityDate', 'Status', 'Priority', 'Type', 'CreatedDate', 'LastModifiedDate', 'IsClosed', 'IsArchived', 'TaskSubtype', 'CompletedDateTime', 'TAG_ActivityType__c', 'TAG_service__c', 'CRM_Region__c', 'CRM_Unit__c', 'CRM_AccountOrgNumber__c', 'ReminderDateTime',
                                         'IsRecurrence', 'RecurrenceStartDateOnly', 'RecurrenceEndDateOnly', 'RecurrenceTimeZoneSidKey', 'RecurrenceType', 'RecurrenceInterval', 'RecurrenceDayOfWeekMask', 'RecurrenceDayOfMonth', 'RecurrenceInstance', 'RecurrenceMonthOfYear', 'RecurrenceRegeneratedType' };


// --------------------------------------------------
// ---------- DO NOT CHANGE ANYTHING BELOW ----------
// --------------------------------------------------

List<PushTopic> oldVersion = [SELECT Id FROM PushTopic WHERE Name = : PUSH_TOPIC_NAME LIMIT 1];

PushTopic pushTopic = new PushTopic(ApiVersion = 47.0, NotifyForOperationCreate = true, NotifyForOperationUpdate = true, NotifyForOperationUndelete = true, NotifyForOperationDelete = true, IsActive = true, Description = 'DO NOT UPDATE! SOQL is maintained in https://github.com/navikt/crm-kafka-activity.');
pushTopic.Name = PUSH_TOPIC_NAME;
pushTopic.Id = oldVersion.size() > 0 ? oldVersion[0].Id : null;
pushTopic.Query = 'SELECT ' + String.join(FIELDS, ', ') + ' FROM ' + SOBJECT_NAME;

upsert pushTopic;