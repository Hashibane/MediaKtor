package database

import exceptions.DatabaseException

class FakeDatabase(val connectionString: String) {
    private var items: MutableList<ItemModel> = mutableListOf(
        ItemModel("item1", "apple", 2, "Some confidential data"),
        ItemModel("item2", "banana", 3, "Another dose of some confidentialData"),
        ItemModel("item3", "mango", 1, "And another one"),
        ItemModel("item4", "pineapple", 1, "And another")
    )

    fun writeItem(item: ItemModel) {
        if (items.map { it.itemId }.contains(item.itemId)) {
            throw DatabaseException("Cannot override existing item.")
        }
        items.add(item)
    }

    fun readItem(itemId: String): ItemModel? {
        return items.find { it.itemId == itemId }
    }
}