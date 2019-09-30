package net.mbonnin.messages.jsonmodel

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
class Data(val messages: List<Message>, val users: List<User>)

@JsonClass(generateAdapter = true)
class Message(
    val id: Long,
    val userId: Long,
    val content: String,
    val attachments: List<Attachment>?
)

@JsonClass(generateAdapter = true)
class Attachment(
    val id: String,
    val title: String,
    val url: String,
    val thumbnailUrl: String
)

@JsonClass(generateAdapter = true)
class User(
    val id: Long,
    val name: String,
    val avatarId: String
)
