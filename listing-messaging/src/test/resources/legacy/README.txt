public.listing.created-listing-note.json is a real message captured on 2026-11-19 from the JEE
listing app (WildFly + cpp-developers-docker Artemis 2.18, Phase 1 build) on topic
jms.topic.public.event, after a create-listing-note command (test data only, no case data).

JMS shape of the captured message (ADR 013):
  type             ActiveMQTextMessage (jakarta.jms.TextMessage)
  JMSDeliveryMode  2 (PERSISTENT)   JMSPriority 4   JMSExpiration 0
  JMSType / JMSCorrelationID / JMSReplyTo  null
  JMSDestination   ActiveMQTopic[jms.topic.public.event]
  properties       CPPNAME = public.listing.created-listing-note (String)
                   JMSXDeliveryCount = 1 (set by the broker)
  text             the file content (compact JSON, _metadata first, payload fields at top level)
