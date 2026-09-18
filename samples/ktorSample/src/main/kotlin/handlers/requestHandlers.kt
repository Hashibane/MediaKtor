package handlers

import annotations.HandlerLifespan
import annotations.RequestHandler
import database.ItemModel
import repositories.ReadRepository
import repositories.WriteRepository

@RequestHandler(HandlerLifespan.SINGLE)
fun readShopInventory(arg: ReadInventoryQuery, readRepository: ReadRepository): ItemModel? {
    return readRepository.readItem(arg.itemId)
}

@RequestHandler(HandlerLifespan.FACTORY)
fun writeShopInventory(arg: WriteInventoryCommand, writeRepository: WriteRepository) {
    return writeRepository.writeItem(arg.data)
}