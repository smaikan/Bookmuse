package com.kitap.pdf

import android.database.sqlite.SQLiteDatabase

/** Idempotent normalization of durable text marker styles. */
object MarkerStyle {
  const val YELLOW = "#FFEB3B"
  fun normalize(db: SQLiteDatabase) {
    db.execSQL("""CREATE TABLE IF NOT EXISTS annotation_style_history(
      annotation_id TEXT PRIMARY KEY REFERENCES annotations(id) ON DELETE CASCADE,
      kind TEXT NOT NULL,color TEXT NOT NULL,revision INTEGER NOT NULL)""")
    val changed="kind IN ('underline','highlight') AND (kind!='highlight' OR color!='#FFEB3B')"
    db.execSQL("INSERT OR IGNORE INTO annotation_style_history(annotation_id,kind,color,revision) SELECT id,kind,color,revision FROM annotations WHERE $changed")
    db.execSQL("UPDATE annotations SET kind='highlight',color='#FFEB3B',revision=revision+1 WHERE $changed")
  }
}
