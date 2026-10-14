package com.moorthy.statussaver.data.model

/** What happened when the user asked to keep a status (FR4). */
enum class SaveResult {
    /** A new copy is now in the gallery. */
    SAVED,

    /** A copy with the same name was already in the gallery; nothing was written (I4). */
    ALREADY_SAVED,

    /** No copy could be made, and nothing was left behind. */
    FAILED,
}
