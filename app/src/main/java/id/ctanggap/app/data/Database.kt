package id.ctanggap.app.data

import android.content.Context
import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "contacts")
data class Contact(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val phone: String,
    val priority: Int // 1 = dihubungi pertama
)

/** status: HITUNG_MUNDUR, TERKIRIM, DITANGANI (kontak membalas YA), DIBATALKAN */
@Entity(tableName = "incidents")
data class Incident(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timeMs: Long,
    val lat: Double?,
    val lng: Double?,
    val trigger: String, // AUTO atau SOS
    val status: String = "HITUNG_MUNDUR"
)

@Entity(tableName = "escalation_log")
data class EscalationLog(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val incidentId: Long,
    val stage: Int,            // 0 = pembatalan
    val recipientType: String, // KONTAK, FASKES, POLISI, 112
    val recipientName: String,
    val phone: String,
    val channel: String,       // SMS, CALL
    val timeMs: Long,
    val status: String         // TERKIRIM, GAGAL, DILEWATI, DIBATALKAN
)

@Entity(tableName = "trips")
data class Trip(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val startMs: Long,
    val endMs: Long? = null,
    val distanceM: Float = 0f,
    val hadIncident: Boolean = false
)

@Dao
interface AppDao {
    @Query("SELECT * FROM contacts ORDER BY priority, id") fun contacts(): Flow<List<Contact>>
    @Query("SELECT * FROM contacts ORDER BY priority, id") suspend fun contactsOnce(): List<Contact>
    @Insert suspend fun insert(c: Contact): Long
    @Delete suspend fun delete(c: Contact)

    @Insert suspend fun insertIncident(i: Incident): Long
    @Update suspend fun updateIncident(i: Incident)
    @Query("SELECT * FROM incidents WHERE id = :id") suspend fun incidentOnce(id: Long): Incident?
    @Query("SELECT * FROM incidents WHERE id = :id") fun incident(id: Long): Flow<Incident?>
    @Query("SELECT * FROM incidents ORDER BY timeMs DESC") fun incidents(): Flow<List<Incident>>

    @Insert suspend fun insertLog(l: EscalationLog)
    @Query("SELECT * FROM escalation_log WHERE incidentId = :id ORDER BY timeMs, id") fun logsFor(id: Long): Flow<List<EscalationLog>>

    @Insert suspend fun insertTrip(t: Trip): Long
    @Update suspend fun updateTrip(t: Trip)
    @Query("SELECT * FROM trips ORDER BY startMs DESC") fun trips(): Flow<List<Trip>>
}

@Database(
    entities = [Contact::class, Incident::class, EscalationLog::class, Trip::class],
    version = 1, exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun dao(): AppDao

    companion object {
        @Volatile private var instance: AppDatabase? = null
        fun get(ctx: Context): AppDatabase = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(ctx.applicationContext, AppDatabase::class.java, "ctanggap.db")
                .fallbackToDestructiveMigration() // hanya untuk tahap pengembangan
                .build().also { instance = it }
        }
    }
}
