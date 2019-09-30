package net.mbonnin.messages

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import androidx.viewbinding.ViewBinding
import kotlinx.coroutines.*
import net.mbonnin.messages.database.GetMessages
import net.mbonnin.messages.database.MessagesQueries
import net.mbonnin.messages.databinding.ItemMeMessageBinding
import net.mbonnin.messages.databinding.ItemMeNameBinding
import net.mbonnin.messages.databinding.ItemOtherMessageBinding
import net.mbonnin.messages.databinding.ItemOtherNameBinding
import net.mbonnin.messages.item.Item
import net.mbonnin.messages.item.toItems
import java.util.concurrent.Executors

class MessagesAdapter(val messagesQueries: MessagesQueries) :
    RecyclerView.Adapter<MessagesAdapter.ViewHolder>() {
    val scope = CoroutineScope(Job() + Executors.newSingleThreadExecutor().asCoroutineDispatcher())


    var itemList = emptyList<Item>()

    // touched by the background thread
    var dbMessageList = mutableListOf<GetMessages>()

    init {
        setHasStableIds(true)
        scope.launch {
            val offset = dbMessageList.size.toLong()
            dbMessageList.addAll(messagesQueries.getMessages(offset + 20, offset).executeAsList())

            val itemList = dbMessageList.toItems()
            scope.launch(Dispatchers.Main) {
                setItems(itemList)
            }
        }
    }

    private fun setItems(items: List<Item>) {
        itemList = items
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = when (viewType) {
            R.layout.item_me_name -> ItemMeNameBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            R.layout.item_other_name -> ItemOtherNameBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            R.layout.item_me_message -> ItemMeMessageBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            R.layout.item_other_message -> ItemOtherMessageBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            else -> throw Exception("unsupported viewType: $viewType")
        }

        return ViewHolder(binding)
    }

    override fun getItemCount(): Int {
        return itemList.size
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = itemList[position]
        when (item) {
            is Item.MeName -> {
                (holder.binding as ItemMeNameBinding).name.text = item.name
            }
            is Item.OtherName -> {
                (holder.binding as ItemOtherNameBinding).name.text = item.name
            }
            is Item.MeMessage -> {
                (holder.binding as ItemMeMessageBinding).message.text = item.content
            }
            is Item.OtherMessage -> {
                (holder.binding as ItemOtherMessageBinding).message.text = item.content
            }
        }
    }

    override fun getItemId(position: Int): Long {
        return itemList[position].id
    }

    override fun getItemViewType(position: Int): Int {
        return when (itemList[position]) {
            is Item.MeName -> R.layout.item_me_name
            is Item.OtherName -> R.layout.item_other_name
            is Item.MeMessage -> R.layout.item_me_message
            is Item.OtherMessage -> R.layout.item_other_message
        }
    }

    fun cancel() {
        scope.cancel()
    }

    class ViewHolder(val binding: ViewBinding) : RecyclerView.ViewHolder(binding.root)
}