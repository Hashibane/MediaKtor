package handlers

import database.ItemModel

enum class Permission {
    AUTHORIZED,
    NOT_AUTHORIZED
}


interface AuthorizedRequest {
    val permissions: Permission
}

data class WriteInventoryCommand(val data: ItemModel,
                                 override val permissions: Permission) : AuthorizedRequest

data class ReadInventoryQuery(val itemId: String,
                              override val permissions: Permission) : AuthorizedRequest


object BackupEvent