package net.mbonnin.messages.item

sealed class Item(val id: Long) {
    class MeName(val name: String, id: Long) : Item(id)
    class OtherName(val name: String, id: Long) : Item(id)
    class MeMessage(val content: String, id: Long) : Item(id)
    class OtherMessage(val content: String, val avatar: String?, id: Long ) : Item(id)
    class Attachment(val title: String, val thumbnailUrl: String, val dbId: String, id: Long) : Item(id)

    object Progress : Item(-1)
}
