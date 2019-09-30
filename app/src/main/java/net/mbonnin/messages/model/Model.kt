package net.mbonnin.messages.model

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
class Data(val messages: List<Message>)

@JsonClass(generateAdapter = true)
class Message(val id: Long,
              val userId: Long,
              val content: String,
              val attachments: List<Attachment>?)

@JsonClass(generateAdapter = true)
class Attachment(id: String,
                 title: String,
                 url: String,
                 thumbnailUrl: String)