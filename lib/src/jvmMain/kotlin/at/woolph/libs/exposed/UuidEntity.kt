package at.woolph.libs.exposed

import org.jetbrains.exposed.dao.Entity
import org.jetbrains.exposed.dao.EntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IdTable
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.Table
import java.util.UUID
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/** Base class for an [Entity] instance identified by an [id] comprised of a wrapped `Uuid` value. */
@OptIn(ExperimentalUuidApi::class)
abstract class UuidEntity(id: EntityID<Uuid>) : Entity<Uuid>(id)

/**
 * Base class representing the [EntityClass] that manages [UuidEntity] instances and
 * maintains their relation to the provided [table].
 *
 * @param [table] The [IdTable] object that stores rows mapped to entities of this class.
 * @param [entityType] The expected [UUIDEntity] type. This can be left `null` if it is the class of type
 * argument [E] provided to this [UUIDEntityClass] instance. If this `UUIDEntityClass` is defined as a companion
 * object of a custom `UUIDEntity` class, the parameter will be set to this immediately enclosing class by default.
 * @sample org.jetbrains.exposed.sql.tests.shared.DDLTests.testDropTableFlushesCache
 * @param [entityCtor] The function invoked to instantiate a [UuidEntity] using a provided [EntityID] value.
 * If a reference to a specific constructor or a custom function is not passed as an argument, reflection will
 * be used to determine the primary constructor of the associated entity class on first access. If this `UUIDEntityClass`
 * is defined as a companion object of a custom `UUIDEntity` class, the constructor will be set to that of the
 * immediately enclosing class by default.
 * @sample org.jetbrains.exposed.sql.tests.shared.entities.EntityTests.testExplicitEntityConstructor
 */
@OptIn(ExperimentalUuidApi::class)
abstract class UuidEntityClass<out E : UuidEntity>(
  table: IdTable<Uuid>,
  entityType: Class<E>? = null,
  entityCtor: ((EntityID<Uuid>) -> E)? = null
) : EntityClass<Uuid, E>(table, entityType, entityCtor)

@OptIn(ExperimentalUuidApi::class)
fun Table.ktUuid(name: String): Column<Uuid> = uuid(name).transform(
  wrap = { Uuid.fromLongs(it.mostSignificantBits, it.leastSignificantBits) },
  unwrap = { it.toLongs { mostSignificantBits, leastSignificantBits -> UUID(mostSignificantBits, leastSignificantBits) } })
