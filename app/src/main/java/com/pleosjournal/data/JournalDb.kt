package com.pleosjournal.data

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabase
import androidx.room.Transaction
import androidx.room.Update
import com.pleosjournal.domain.Stop
import com.pleosjournal.domain.Trip
import com.pleosjournal.domain.merge
import com.pleosjournal.domain.split
import kotlinx.coroutines.flow.Flow

// Room: @Entity = 테이블 한 개, @Dao = 그 테이블에 대한 쿼리 모음. 데이터는 기기 안에만 저장된다(CLAUDE.md 제약 7).

@Entity(tableName = "trip")
data class TripEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val startMs: Long,
    val endMs: Long,
    val distanceKm: Double,
    val source: String, // REAL / REPLAY / DUMMY
    val memo: String? = null,       // UC-07b 음성 회고 텍스트
    val memoSource: String? = null, // 그 텍스트를 만든 STT의 출처
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

/** UC-12 장면 후보. 근거 구간은 중복 저장하지 않고, 같은 트립의 span 중 같은 kind이며 시간이 겹치는 것으로 찾는다. */
@Entity(tableName = "scene")
data class SceneEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val tripId: Long,
    val kind: String,
    val startMs: Long,
    val endMs: Long,
    val status: String = PENDING,
    val answer: String? = null,       // 음성 답변 텍스트
    val answerSource: String? = null,
) {
    companion object {
        const val PENDING = "PENDING"     // 아직 묻는 중
        const val CONFIRMED = "CONFIRMED" // 사용자가 확인. 이것만 태그가 된다.
        const val DENIED = "DENIED"       // 사용자가 아니라고 함. 후보는 숨기고 원시 관측(span)은 보존.
    }
}

/** 트립 안의 정차 구간. 트립 분할(UC-10)의 기준점. */
@Entity(tableName = "stop")
data class StopEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val tripId: Long,
    val startMs: Long,
    val endMs: Long,
    val kmAtStop: Double,
)

@Dao
abstract class JournalDao {
    @Insert abstract suspend fun insert(trip: TripEntity): Long
    @Insert abstract suspend fun insert(spans: List<SpanEntity>)

    // Flow를 반환하면 테이블이 바뀔 때마다 Room이 새 목록을 다시 내보낸다. UI는 이것만 구독한다.
    @Query("SELECT * FROM trip ORDER BY startMs DESC") abstract fun trips(): Flow<List<TripEntity>>
    @Query("SELECT * FROM span ORDER BY startMs") abstract fun spans(): Flow<List<SpanEntity>>

    @Insert abstract suspend fun insertScenes(scenes: List<SceneEntity>)
    @Query("SELECT * FROM scene ORDER BY startMs") abstract fun scenes(): Flow<List<SceneEntity>>
    @Query("UPDATE scene SET status = :status, answer = :answer, answerSource = :answerSource WHERE id = :id")
    abstract suspend fun answerScene(id: Long, status: String, answer: String? = null, answerSource: String? = null)
    @Query("UPDATE trip SET memo = :memo, memoSource = :memoSource WHERE id = :id")
    abstract suspend fun setMemo(id: Long, memo: String, memoSource: String)
    @Query("DELETE FROM scene") abstract suspend fun deleteScenes()

    @Insert abstract suspend fun insertStops(stops: List<StopEntity>)
    @Query("SELECT * FROM stop ORDER BY startMs") abstract fun stops(): Flow<List<StopEntity>>
    @Query("SELECT * FROM stop WHERE tripId = :tripId ORDER BY startMs") abstract suspend fun stopsOf(tripId: Long): List<StopEntity>
    @Query("DELETE FROM stop WHERE tripId = :tripId") abstract suspend fun deleteStopsOf(tripId: Long)
    @Query("DELETE FROM stop") abstract suspend fun deleteStops()
    @Query("UPDATE span SET tripId = :to WHERE tripId = :from AND startMs >= :fromMs") abstract suspend fun moveSpansAfter(from: Long, to: Long, fromMs: Long)
    @Query("UPDATE scene SET tripId = :to WHERE tripId = :from AND startMs >= :fromMs") abstract suspend fun moveScenesAfter(from: Long, to: Long, fromMs: Long)
    @Query("UPDATE scene SET status = 'PENDING' WHERE tripId = :tripId AND status = 'DENIED'") abstract suspend fun restoreDeniedScenes(tripId: Long)

    // UC-10 트립 보정용
    @Query("SELECT * FROM trip WHERE id = :id") abstract suspend fun trip(id: Long): TripEntity?
    @Update abstract suspend fun update(trip: TripEntity)
    @Query("UPDATE span SET tripId = :to WHERE tripId = :from") abstract suspend fun moveSpans(from: Long, to: Long)
    @Query("UPDATE scene SET tripId = :to WHERE tripId = :from") abstract suspend fun moveScenes(from: Long, to: Long)
    @Query("DELETE FROM trip WHERE id = :id") abstract suspend fun deleteTripRow(id: Long)
    @Query("DELETE FROM span WHERE tripId = :id") abstract suspend fun deleteSpansOf(id: Long)
    @Query("DELETE FROM scene WHERE tripId = :id") abstract suspend fun deleteScenesOf(id: Long)

    /** [dropId] 트립을 [keepId] 트립에 합친다. 단서 구간, 장면 후보, 회고 메모를 모두 옮긴다. */
    @Transaction
    open suspend fun mergeTrips(keepId: Long, dropId: Long) {
        val keep = trip(keepId) ?: return
        val drop = trip(dropId) ?: return
        val merged = merge(keep.toTrip(stopsOf(keepId)), drop.toTrip(stopsOf(dropId)))
        update(
            keep.copy(
                startMs = merged.startMs, endMs = merged.endMs, distanceKm = merged.distanceKm,
                memo = listOfNotNull(keep.memo, drop.memo).joinToString("\n").ifEmpty { null },
                memoSource = keep.memoSource ?: drop.memoSource,
            ),
        )
        moveSpans(dropId, keepId)
        moveScenes(dropId, keepId)
        replaceStops(keepId, merged.stops)
        deleteStopsOf(dropId)
        deleteTripRow(dropId)
    }

    /**
     * 트립을 정차 [stopId] 지점에서 둘로 나눈다(UC-10). 앞부분은 기존 트립에 남고 뒷부분은 새 트립이 된다.
     * 정차가 끝난 뒤에 시작한 관측 구간과 장면 후보는 뒷부분으로 옮긴다. 회고 메모는 앞부분에 남는다.
     */
    @Transaction
    open suspend fun splitTrip(tripId: Long, stopId: Long) {
        val trip = trip(tripId) ?: return
        val stops = stopsOf(tripId)
        val at = stops.firstOrNull { it.id == stopId } ?: return
        val (first, second) = split(trip.toTrip(stops), Stop(at.startMs, at.endMs, at.kmAtStop))
        update(trip.copy(startMs = first.startMs, endMs = first.endMs, distanceKm = first.distanceKm))
        val newId = insert(TripEntity(startMs = second.startMs, endMs = second.endMs, distanceKm = second.distanceKm, source = trip.source))
        moveSpansAfter(tripId, newId, at.endMs)
        moveScenesAfter(tripId, newId, at.endMs)
        replaceStops(tripId, first.stops)
        replaceStops(newId, second.stops)
    }

    private suspend fun replaceStops(tripId: Long, stops: List<Stop>) {
        deleteStopsOf(tripId)
        insertStops(stops.map { StopEntity(tripId = tripId, startMs = it.startMs, endMs = it.endMs, kmAtStop = it.kmAtStop) })
    }

    private fun TripEntity.toTrip(stops: List<StopEntity>) =
        Trip(startMs, endMs, distanceKm, stops.map { Stop(it.startMs, it.endMs, it.kmAtStop) })

    /** 트립 하나와 거기에 딸린 관측 구간, 장면 후보를 모두 지운다. 되돌릴 수 없다. */
    @Transaction
    open suspend fun deleteTrip(id: Long) {
        deleteStopsOf(id)
        deleteScenesOf(id)
        deleteSpansOf(id)
        deleteTripRow(id)
    }

    @Query("DELETE FROM trip") abstract suspend fun deleteTrips()
    @Query("DELETE FROM span") abstract suspend fun deleteSpans()

    /** 트립과 그 단서 구간을 한 트랜잭션으로 저장한다. spans의 tripId는 여기서 채운다. */
    @Transaction
    open suspend fun save(trip: TripEntity, spans: List<SpanEntity>, scenes: List<SceneEntity>, stops: List<StopEntity>) {
        val id = insert(trip)
        insertStops(stops.map { it.copy(tripId = id) })
        insert(spans.map { it.copy(tripId = id) })
        insertScenes(scenes.map { it.copy(tripId = id) })
    }

    @Transaction
    open suspend fun deleteAll() {
        deleteStops()
        deleteScenes()
        deleteSpans()
        deleteTrips()
    }
}

@Database(entities = [TripEntity::class, SpanEntity::class, SceneEntity::class, StopEntity::class], version = 3, exportSchema = false)
abstract class JournalDb : RoomDatabase() {
    abstract fun dao(): JournalDao
}
