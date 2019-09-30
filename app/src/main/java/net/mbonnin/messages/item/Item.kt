package net.mbonnin.messages.item

import net.mbonnin.messages.database.Dbmessage
import net.mbonnin.messages.database.GetMessages

sealed class Item(val id: Long) {
    class MeName(val name: String, id: Long) : Item(id)
    class OtherName(val name: String, id: Long) : Item(id)
    class MeMessage(val content: String, id: Long) : Item(id)
    class OtherMessage(val content: String, val avatar: String, id: Long ) : Item(id)
}

fun List<GetMessages>.toItems(): List<Item> {
    var lastUser = -1L

    val list = mutableListOf<Item>()
    forEach {
        if (it.userId != lastUser) {
            if (it.userId == 1L) {
                list.add(Item.MeName(it.name, it.id + 1L.shl(32)))
            } else {
                list.add(Item.OtherName(it.name, it.id + 1L.shl(32)))
            }
        }

        if (it.userId == 1L) {
            list.add(Item.MeMessage(it.content, it.id))
        } else {
            list.add(Item.OtherMessage(it.content, it.avatarId, it.id))
        }
        lastUser = it.userId

    }
    return list
}