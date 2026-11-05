package com.panelcut3d.app.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString

class ProjectRepository(private val dao: ProjectDao) {

    fun observeAll(): Flow<List<FurnitureProject>> = dao.observeAll().map { entities ->
        entities.mapNotNull { it.toProjectOrNull() }
    }

    suspend fun get(id: Long): FurnitureProject? = dao.get(id)?.toProjectOrNull()

    suspend fun save(project: FurnitureProject): Long {
        val now = System.currentTimeMillis()
        val entity = project.toEntity(now)
        return if (project.id == 0L) {
            dao.insert(entity)
        } else {
            dao.update(entity)
            project.id
        }
    }

    suspend fun delete(id: Long) = dao.delete(id)

    suspend fun duplicate(id: Long): Long? {
        val source = get(id) ?: return null
        val copy = FurnitureProject(
            name = "${source.name} (copy)",
            design = source.design,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis(),
        )
        return save(copy)
    }

    suspend fun clearAll() = dao.deleteAll()

    private fun ProjectEntity.toProjectOrNull(): FurnitureProject? = runCatching {
        val design = AppJson.decodeFromString<ProjectDesign>(designJson)
        FurnitureProject(
            id = id,
            name = name,
            design = design,
            createdAt = createdAt,
            updatedAt = updatedAt,
        )
    }.getOrNull()

    private fun FurnitureProject.toEntity(now: Long): ProjectEntity {
        val params = design.params
        return ProjectEntity(
            id = id,
            name = name.ifBlank { "Untitled project" },
            type = params.type.name,
            widthMm = params.width,
            heightMm = params.height,
            depthMm = params.depth,
            panelCount = design.panels.size,
            designJson = AppJson.encodeToString(design),
            createdAt = if (createdAt == 0L) now else createdAt,
            updatedAt = now,
        )
    }
}
