package com.example.studentcopilot.data.local.database

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_1_4 = object : Migration(1, 4) {
    override fun migrate(db: SupportSQLiteDatabase) {
        migrateToV4(db)
    }
}

val MIGRATION_2_4 = object : Migration(2, 4) {
    override fun migrate(db: SupportSQLiteDatabase) {
        migrateToV4(db)
    }
}

val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(db: SupportSQLiteDatabase) {
        migrateToV4(db)
    }
}

val MIGRATION_4_5 = object : Migration(4, 5) {
    override fun migrate(db: SupportSQLiteDatabase) {
        addOwnerUserIdColumns(db)
    }
}

val MIGRATION_5_6 = object : Migration(5, 6) {
    override fun migrate(db: SupportSQLiteDatabase) {
        addRemoteIdColumns(db)
    }
}

val MIGRATION_6_7 = object : Migration(6, 7) {
    override fun migrate(db: SupportSQLiteDatabase) {
        addCourseScheduleColumns(db)
    }
}

val MIGRATION_7_8 = object : Migration(7, 8) {
    override fun migrate(db: SupportSQLiteDatabase) {
        addTimetableEntriesTable(db)
    }
}

private fun migrateToV4(database: SupportSQLiteDatabase) {
    rebuildCoursesTable(database)
    rebuildAssignmentsTable(database)
    rebuildExamsTable(database)
}

private fun rebuildCoursesTable(database: SupportSQLiteDatabase) {
    val createTableSql = """
        CREATE TABLE IF NOT EXISTS `courses_new` (
            `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
            `name` TEXT NOT NULL
        )
    """.trimIndent()

    if (!tableExists(database, "courses")) {
        database.execSQL(
            createTableSql.replace("courses_new", "courses"),
        )
        return
    }

    val columns = getColumnNames(database, "courses")
    val idExpr = if ("id" in columns) "id" else "NULL"
    val nameExpr = firstAvailableColumn(columns, listOf("name", "title", "courseName"))
        ?.let { "COALESCE($it, 'Untitled course')" }
        ?: "'Untitled course'"

    database.execSQL(createTableSql)
    database.execSQL(
        """
            INSERT INTO `courses_new` (`id`, `name`)
            SELECT $idExpr, $nameExpr
            FROM `courses`
        """.trimIndent(),
    )
    database.execSQL("DROP TABLE `courses`")
    database.execSQL("ALTER TABLE `courses_new` RENAME TO `courses`")
}

private fun rebuildAssignmentsTable(database: SupportSQLiteDatabase) {
    val createTableSql = """
        CREATE TABLE IF NOT EXISTS `assignments_new` (
            `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
            `courseId` INTEGER NOT NULL,
            `title` TEXT NOT NULL,
            `dueDate` INTEGER NOT NULL,
            `isCompleted` INTEGER NOT NULL DEFAULT 0,
            FOREIGN KEY(`courseId`) REFERENCES `courses`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
        )
    """.trimIndent()

    if (!tableExists(database, "assignments")) {
        database.execSQL(
            createTableSql.replace("assignments_new", "assignments"),
        )
        database.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_assignments_courseId` ON `assignments` (`courseId`)",
        )
        return
    }

    val columns = getColumnNames(database, "assignments")
    val idExpr = if ("id" in columns) "id" else "NULL"
    val courseExpr = firstAvailableColumn(columns, listOf("courseId", "course_id"))
    val titleExpr = firstAvailableColumn(columns, listOf("title", "name"))
        ?.let { "COALESCE($it, 'Untitled assignment')" }
        ?: "'Untitled assignment'"
    val dueDateExpr = firstAvailableColumn(columns, listOf("dueDate", "date"))
        ?.let { "COALESCE($it, 0)" }
        ?: "0"
    val completedExpr = firstAvailableColumn(columns, listOf("isCompleted", "completed"))
        ?.let { "COALESCE($it, 0)" }
        ?: "0"

    database.execSQL(createTableSql)
    if (courseExpr != null) {
        database.execSQL(
            """
                INSERT INTO `assignments_new` (`id`, `courseId`, `title`, `dueDate`, `isCompleted`)
                SELECT $idExpr, $courseExpr, $titleExpr, $dueDateExpr, $completedExpr
                FROM `assignments`
                WHERE $courseExpr IN (SELECT `id` FROM `courses`)
            """.trimIndent(),
        )
    }
    database.execSQL("DROP TABLE `assignments`")
    database.execSQL("ALTER TABLE `assignments_new` RENAME TO `assignments`")
    database.execSQL(
        "CREATE INDEX IF NOT EXISTS `index_assignments_courseId` ON `assignments` (`courseId`)",
    )
}

private fun rebuildExamsTable(database: SupportSQLiteDatabase) {
    val createTableSql = """
        CREATE TABLE IF NOT EXISTS `exams_new` (
            `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
            `courseId` INTEGER NOT NULL,
            `title` TEXT NOT NULL,
            `date` INTEGER NOT NULL,
            `type` TEXT NOT NULL,
            FOREIGN KEY(`courseId`) REFERENCES `courses`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
        )
    """.trimIndent()

    if (!tableExists(database, "exams")) {
        database.execSQL(
            createTableSql.replace("exams_new", "exams"),
        )
        database.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_exams_courseId` ON `exams` (`courseId`)",
        )
        return
    }

    val columns = getColumnNames(database, "exams")
    val idExpr = if ("id" in columns) "id" else "NULL"
    val courseExpr = firstAvailableColumn(columns, listOf("courseId", "course_id"))
    val titleExpr = firstAvailableColumn(columns, listOf("title", "name"))
        ?.let { "COALESCE($it, 'Untitled exam')" }
        ?: "'Untitled exam'"
    val dateExpr = firstAvailableColumn(columns, listOf("date", "examDate"))
        ?.let { "COALESCE($it, 0)" }
        ?: "0"
    val typeExpr = firstAvailableColumn(columns, listOf("type", "examType"))
        ?.let { "COALESCE($it, 'Other')" }
        ?: "'Other'"

    database.execSQL(createTableSql)
    if (courseExpr != null) {
        database.execSQL(
            """
                INSERT INTO `exams_new` (`id`, `courseId`, `title`, `date`, `type`)
                SELECT $idExpr, $courseExpr, $titleExpr, $dateExpr, $typeExpr
                FROM `exams`
                WHERE $courseExpr IN (SELECT `id` FROM `courses`)
            """.trimIndent(),
        )
    }
    database.execSQL("DROP TABLE `exams`")
    database.execSQL("ALTER TABLE `exams_new` RENAME TO `exams`")
    database.execSQL(
        "CREATE INDEX IF NOT EXISTS `index_exams_courseId` ON `exams` (`courseId`)",
    )
}

private fun tableExists(database: SupportSQLiteDatabase, tableName: String): Boolean {
    database.query(
        "SELECT 1 FROM sqlite_master WHERE type = 'table' AND name = ?",
        arrayOf(tableName),
    ).use { cursor ->
        return cursor.moveToFirst()
    }
}

private fun getColumnNames(database: SupportSQLiteDatabase, tableName: String): Set<String> {
    database.query("PRAGMA table_info(`$tableName`)").use { cursor ->
        val nameIndex = cursor.getColumnIndex("name")
        val columnNames = mutableSetOf<String>()
        while (cursor.moveToNext()) {
            columnNames += cursor.getString(nameIndex)
        }
        return columnNames
    }
}

private fun firstAvailableColumn(columns: Set<String>, candidates: List<String>): String? {
    return candidates.firstOrNull { it in columns }
}

private fun addOwnerUserIdColumns(database: SupportSQLiteDatabase) {
    database.execSQL(
        "ALTER TABLE `courses` ADD COLUMN `ownerUserId` TEXT NOT NULL DEFAULT ''",
    )
    database.execSQL(
        "ALTER TABLE `assignments` ADD COLUMN `ownerUserId` TEXT NOT NULL DEFAULT ''",
    )
    database.execSQL(
        "ALTER TABLE `exams` ADD COLUMN `ownerUserId` TEXT NOT NULL DEFAULT ''",
    )
    database.execSQL(
        "CREATE INDEX IF NOT EXISTS `index_courses_ownerUserId` ON `courses` (`ownerUserId`)",
    )
    database.execSQL(
        "CREATE INDEX IF NOT EXISTS `index_assignments_ownerUserId` ON `assignments` (`ownerUserId`)",
    )
    database.execSQL(
        "CREATE INDEX IF NOT EXISTS `index_exams_ownerUserId` ON `exams` (`ownerUserId`)",
    )
}

private fun addRemoteIdColumns(database: SupportSQLiteDatabase) {
    database.execSQL(
        "ALTER TABLE `courses` ADD COLUMN `remoteId` TEXT",
    )
    database.execSQL(
        "ALTER TABLE `assignments` ADD COLUMN `remoteId` TEXT",
    )
    database.execSQL(
        "ALTER TABLE `exams` ADD COLUMN `remoteId` TEXT",
    )
    database.execSQL(
        "CREATE UNIQUE INDEX IF NOT EXISTS `index_courses_ownerUserId_remoteId` ON `courses` (`ownerUserId`, `remoteId`)",
    )
    database.execSQL(
        "CREATE UNIQUE INDEX IF NOT EXISTS `index_assignments_ownerUserId_remoteId` ON `assignments` (`ownerUserId`, `remoteId`)",
    )
    database.execSQL(
        "CREATE UNIQUE INDEX IF NOT EXISTS `index_exams_ownerUserId_remoteId` ON `exams` (`ownerUserId`, `remoteId`)",
    )
}

private fun addCourseScheduleColumns(database: SupportSQLiteDatabase) {
    database.execSQL(
        "ALTER TABLE `courses` ADD COLUMN `classDayOfWeek` INTEGER",
    )
    database.execSQL(
        "ALTER TABLE `courses` ADD COLUMN `classStartMinuteOfDay` INTEGER",
    )
}

private fun addTimetableEntriesTable(database: SupportSQLiteDatabase) {
    database.execSQL(
        """
        CREATE TABLE IF NOT EXISTS `timetable_entries` (
            `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
            `remoteId` TEXT,
            `ownerUserId` TEXT NOT NULL,
            `courseId` INTEGER NOT NULL,
            `dayOfWeek` INTEGER NOT NULL,
            `startMinuteOfDay` INTEGER NOT NULL,
            `endMinuteOfDay` INTEGER,
            `classType` TEXT,
            `section` TEXT,
            `venue` TEXT,
            `lecturer` TEXT,
            `weekPattern` TEXT,
            `source` TEXT NOT NULL,
            FOREIGN KEY(`courseId`) REFERENCES `courses`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
        )
        """.trimIndent(),
    )
    database.execSQL(
        """
        INSERT INTO `timetable_entries` (
            `ownerUserId`,
            `courseId`,
            `dayOfWeek`,
            `startMinuteOfDay`,
            `source`
        )
        SELECT
            `ownerUserId`,
            `id`,
            `classDayOfWeek`,
            `classStartMinuteOfDay`,
            'course_schedule'
        FROM `courses`
        WHERE `classDayOfWeek` IS NOT NULL
            AND `classStartMinuteOfDay` IS NOT NULL
        """.trimIndent(),
    )
    database.execSQL(
        "CREATE INDEX IF NOT EXISTS `index_timetable_entries_courseId` ON `timetable_entries` (`courseId`)",
    )
    database.execSQL(
        "CREATE INDEX IF NOT EXISTS `index_timetable_entries_ownerUserId` ON `timetable_entries` (`ownerUserId`)",
    )
    database.execSQL(
        "CREATE UNIQUE INDEX IF NOT EXISTS `index_timetable_entries_ownerUserId_remoteId` ON `timetable_entries` (`ownerUserId`, `remoteId`)",
    )
}
