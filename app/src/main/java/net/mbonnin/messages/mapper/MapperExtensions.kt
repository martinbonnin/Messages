package net.mbonnin.messages.mapper

import net.mbonnin.messages.database.Dbmessage
import net.mbonnin.messages.jsonmodel.Message

fun Dbmessage.toMessage() = Message(id = id, content = content, userId = userId, attachments = null)