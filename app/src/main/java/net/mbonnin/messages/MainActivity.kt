package net.mbonnin.messages

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.LayoutInflater
import androidx.recyclerview.widget.LinearLayoutManager
import com.squareup.moshi.Moshi
import com.squareup.sqldelight.android.AndroidSqliteDriver
import com.squareup.sqldelight.db.SqlDriver
import kotlinx.coroutines.*
import net.mbonnin.messages.database.MessagesDatabase
import net.mbonnin.messages.databinding.ActivityMainBinding
import net.mbonnin.messages.jsonmodel.Data
import okio.buffer
import okio.source

class MainActivity : AppCompatActivity() {
    private lateinit var adapter: MessagesAdapter
    private lateinit var binding: ActivityMainBinding
    private var job: Job? = null

    private val driver: SqlDriver =
        AndroidSqliteDriver(MessagesDatabase.Schema, this, "messages.db")
    private val database = MessagesDatabase(
        driver = driver
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(LayoutInflater.from(this))
        setContentView(binding.root)

        job = GlobalScope.launch(Dispatchers.IO) {
            val databaseCount = database.messagesQueries.messageCount().executeAsOne()
            if (databaseCount == 0L) {
                val adapter = Moshi.Builder().build().adapter(Data::class.java)

                val data = resources.openRawResource(R.raw.messages).source().buffer().use {
                    adapter.fromJson(it)
                }!!

                data.messages.forEach {
                    database.messagesQueries.insertMessage(
                        id = it.id,
                        userId = it.userId,
                        content = it.content
                    )
                }
                data.users.forEach {
                    database.messagesQueries.insertUser(
                        id = it.id,
                        avatarId = it.avatarId,
                        name = it.name
                    )
                }
            }

            launch(Dispatchers.Main){
                adapter.start(databaseCount)
            }
        }

        adapter = MessagesAdapter(database.messagesQueries)
        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        binding.recyclerView.adapter = adapter
    }

    override fun onDestroy() {
        super.onDestroy()
        job?.cancel()
        adapter.cancel()
    }
}
