package net.mbonnin.messages

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import androidx.viewbinding.ViewBinding
import coil.api.load
import coil.transform.CircleCropTransformation
import kotlinx.coroutines.*
import net.mbonnin.messages.database.GetMessages
import net.mbonnin.messages.database.MessagesQueries
import net.mbonnin.messages.databinding.*
import net.mbonnin.messages.item.Item
import net.mbonnin.messages.item.toItems
import java.util.concurrent.Executors

class MessagesAdapter(val messagesQueries: MessagesQueries) :
    RecyclerView.Adapter<MessagesAdapter.ViewHolder>() {
    private var displayedMessageCount = 0
    private val scope =
        CoroutineScope(Job() + Executors.newSingleThreadExecutor().asCoroutineDispatcher())

    private var dbCount = 0L
    private var itemList = listOf<Item>(Item.Progress)
    private var job: Job? = null

    // only touched by the background thread
    private var dbMessageList = mutableListOf<GetMessages>()

    init {
        setHasStableIds(true)
    }

    private fun setItems(
        items: List<Item>,
        displayedMessageCount: Int
    ) {
        this.itemList = items
        this.displayedMessageCount = displayedMessageCount
        notifyDataSetChanged()
    }

    fun loadMore() {
        if (job != null) {
            return
        }

        scope.launch {
            val offset = dbMessageList.size.toLong()
            if (offset < dbCount) {
                dbMessageList.addAll(
                    messagesQueries.getMessages(
                        offset + 20,
                        offset
                    ).executeAsList()
                )

                val itemList = dbMessageList.toItems()
                launch(Dispatchers.Main) {
                    setItems(itemList, dbMessageList.size)
                    job = null
                }
            }
        }
    }

    fun start(dbCount: Long) {
        this.dbCount = dbCount
        loadMore()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = when (viewType) {
            R.layout.item_me_name -> ItemMeNameBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
            R.layout.item_other_name -> ItemOtherNameBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
            R.layout.item_me_message -> ItemMeMessageBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
            R.layout.item_other_message -> ItemOtherMessageBinding.inflate(
                LayoutInflater.from(
                    parent.context
                ), parent, false
            )
            R.layout.item_progress -> ItemProgressBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
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
                (holder.binding as ItemOtherMessageBinding).avatar.load(item.avatar) {
                    crossfade(true)
                    placeholder(R.color.white)
                    transformations(CircleCropTransformation())
                }
            }
        }

        if (displayedMessageCount - 1 - position < 5) {
            loadMore()
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
            is Item.Progress -> R.layout.item_progress
        }
    }

    fun cancel() {
        scope.cancel()
    }

    class ViewHolder(val binding: ViewBinding) : RecyclerView.ViewHolder(binding.root)
}