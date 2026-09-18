package repositories

import database.FakeDatabase
import database.ItemModel

class ReadRepository(val database: FakeDatabase) {

    fun readItem(itemId: String): ItemModel? = database.readItem(itemId)
}