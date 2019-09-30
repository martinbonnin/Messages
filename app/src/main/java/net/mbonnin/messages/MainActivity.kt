package net.mbonnin.messages

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.LayoutInflater
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.squareup.moshi.Moshi
import com.squareup.sqldelight.android.AndroidSqliteDriver
import com.squareup.sqldelight.db.SqlDriver
import kotlinx.coroutines.*
import net.mbonnin.messages.database.MessagesDatabase
import net.mbonnin.messages.databinding.ActivityMainBinding
import net.mbonnin.messages.item.Item
import net.mbonnin.messages.jsonmodel.Data
import okio.buffer
import okio.source
import java.util.concurrent.Executors
import androidx.recyclerview.widget.DefaultItemAnimator
import android.R.string.no
import android.R.attr.name
import androidx.core.app.ComponentActivity.ExtraData
import androidx.core.content.ContextCompat.getSystemService
import android.icu.lang.UCharacter.GraphemeClusterBreak.T



class MainActivity : AppCompatActivity() {
    private lateinit var adapter: MessagesAdapter
    private lateinit var binding: ActivityMainBinding
    private val scope = CoroutineScope(Job() + Dispatchers.IO)

    private val driver: SqlDriver =
        AndroidSqliteDriver(MessagesDatabase.Schema, this, "messages.db")
    private val database = MessagesDatabase(
        driver = driver
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(LayoutInflater.from(this))
        setContentView(binding.root)

        scope.launch(Dispatchers.IO) {
            var databaseCount = database.messagesQueries.messageCount().executeAsOne()
            if (databaseCount == 0L) {
                val adapter = Moshi.Builder().build().adapter(Data::class.java)

                val data = resources.openRawResource(R.raw.messages).source().buffer().use {
                    adapter.fromJson(it)
                }!!

                data.messages.forEach { message ->
                    database.messagesQueries.insertMessage(
                        id = message.id,
                        userId = message.userId,
                        content = message.content
                    )
                    message.attachments?.forEachIndexed { index, attachment ->
                        database.messagesQueries.insertAttachment(
                            id = attachment.id,
                            messageId = message.id,
                            idx = index.toLong(),
                            title = attachment.title,
                            url = attachment.url,
                            thumbnailUrl = attachment.thumbnailUrl
                        )
                    }
                }

                data.users.forEach {
                    database.messagesQueries.insertUser(
                        id = it.id,
                        avatarId = it.avatarId,
                        name = it.name
                    )
                }
                databaseCount = database.messagesQueries.messageCount().executeAsOne()
            }

            launch(Dispatchers.Main) {
                adapter.start(databaseCount)
            }
        }

        adapter = MessagesAdapter(database.messagesQueries)

        val swipeHandler = object : SwipeToDeleteCallback(this) {
            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                // optimistic removal. We could remove from the DB and have the adapter react on that which would
                // make the data flow more unidirectional but that wouldn't work with the requirement that items
                // are loaded 20 by 20...
                val item = adapter.removeAt(viewHolder.adapterPosition)
                scope.launch {
                    when (item) {
                        is Item.MeMessage -> database.messagesQueries.deleteMessage(item.id)
                        is Item.OtherMessage -> database.messagesQueries.deleteMessage(item.id)
                        is Item.Attachment -> database.messagesQueries.deleteAttachment(item.dbId)
                    }
                }
            }
        }

        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        binding.recyclerView.adapter = adapter
        ItemTouchHelper(swipeHandler).attachToRecyclerView(binding.recyclerView)
        binding.recyclerView.setItemAnimator(DefaultItemAnimator())
    }

    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
        adapter.cancel()
    }
}
