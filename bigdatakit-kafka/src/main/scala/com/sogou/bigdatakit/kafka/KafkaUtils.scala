package com.sogou.bigdatakit.kafka

import kafka.api.{OffsetRequest, PartitionOffsetRequestInfo}
import kafka.client.ClientUtils
import kafka.common.TopicAndPartition
import kafka.consumer.SimpleConsumer

/**
 * Created by Tao Li on 12/11/15.
 */
object KafkaUtils {
  def getLatestOffsets(brokerList: String, topic: String) = {
    val clientId = "GetTopicLatestOffset"
    val metadataTargetBrokers = ClientUtils.parseBrokerList(brokerList)
    val topicsMetadata = ClientUtils.fetchTopicMetadata(
      Set(topic), metadataTargetBrokers, clientId, 1000).topicsMetadata
    if (topicsMetadata.size != 1 || !topicsMetadata(0).topic.equals(topic)) {
      throw new RuntimeException(
        s"no valid topic metadata for topic: $topic, probably the topic does not exist")
    }
    val partitions = topicsMetadata.head.partitionsMetadata.map(_.partitionId)

    partitions.map { partitionId =>
      val partitionMetadataOpt =
        topicsMetadata.head.partitionsMetadata.find(_.partitionId == partitionId)
      partitionMetadataOpt match {
        case Some(metadata) =>
          metadata.leader match {
            case Some(leader) =>
              val consumer = new SimpleConsumer(leader.host, leader.port, 10000, 100000, clientId)
              val topicAndPartition = TopicAndPartition(topic, partitionId)
              val request = OffsetRequest(
                Map(topicAndPartition -> PartitionOffsetRequestInfo(-1, 1)))
              val offsets = consumer.getOffsetsBefore(request).
                partitionErrorAndOffsets(topicAndPartition).offsets
              consumer.close()
              (partitionId, offsets(0))
            case None => throw new RuntimeException(
              s"partition $partitionId does not have a leader. Skip getting offsets")
          }
        case None => throw new RuntimeException(s"partition $partitionId does not exist")
      }
    }.toMap
  }
}