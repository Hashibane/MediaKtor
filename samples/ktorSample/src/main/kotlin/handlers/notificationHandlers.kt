package handlers

import annotations.NotificationHandler
import database.FakeDatabase
import io.ktor.util.logging.*

@NotificationHandler
fun backupDatabase(event: BackupEvent, database: FakeDatabase, logger: Logger) {
    // Backup logic
    // ...
    // If there is an error, our error pipeline handler will catch it

    logger.info("Database backup completed.")
}