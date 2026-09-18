package repositories

import database.FakeDatabase
import database.ItemModel

class WriteRepository(val database: FakeDatabase) {

    fun writeItem(item: ItemModel) = database.writeItem(item)
}