package com.pleosjournal.data

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabase
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

// Room: @Entity = 테이블 한 개, @Dao = 그 테이블에 대한 쿼리 모음. 데이터는 기기 안에만 저장된다(CLAUDE.md 제약 7).

@Entity(tableName = "trip")
data class TripEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val startMs: Long,
    val endMs: Long,
    val distanceKm: Double,
    val source: String, // REAL / REPLAY / DUMMY
)

@Entity(tableName = "span")
data class SpanEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val tripId: Long,
    val kind: String, // ClueKind 이름
    val target: String,
    val startMs: Long,
    val endMs: Long,
    val source: String,
)

@Dao
abstract class JournalDao {
    @Insert abstract suspend fun insert(trip: TripEntity): Long
    @Insert abstract suspend fun insert(spans: List<SpanEntity>)

    // Flow를 반환하면 테이블이 바뀔 때마다 Room이 새 목록을 다시 내보낸다. UI는 이것만 구독한다.
    @Query("SELECT * FROM trip ORDER BY startMs DESC") abstract fun trips(): Flow<List<TripEntity>>
    @Query("SELECT * FROM span ORDER BY startMs") abstract fun spans(): Flow<List<SpanEntity>>

    @Query("DELETE FROM trip") abstract suspend fun deleteTrips()
    @Query("DELETE FROM span") abstract suspend fun deleteSpans()

    /** 트립과 그 단서 구간을 한 트랜잭션으로 저장한다. spans의 tripId는 여기서 채운다. */
    @Transaction
    open suspend fun save(trip: TripEntity, spans: List<SpanEntity>) {
        val id = insert(trip)
        insert(spans.map { it.copy(tripId = id) })
    }

    @Transaction
    open suspend fun deleteAll() {
        deleteSpans()
        deleteTrips()
    }
}

@Database(entities = [TripEntity::class, SpanEntity::class], version = 1, exportSchema = false)
abstract class JournalDb : RoomDatabase() {
    abstract fun dao(): JournalDao
}
