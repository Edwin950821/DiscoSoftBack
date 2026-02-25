package com.kompralo.controller

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile
import java.nio.file.Files
import java.nio.file.Paths
import java.nio.file.StandardCopyOption
import java.util.UUID

@RestController
@CrossOrigin(origins = ["http://localhost:5173"], allowCredentials = "true")
class UploadController {

    private val logger = LoggerFactory.getLogger(javaClass)

    @Value("\${upload.dir:uploads}")
    private lateinit var uploadDir: String

    private val allowedTypes = setOf("image/jpeg", "image/png", "image/webp")
    private val maxFileSize = 5L * 1024 * 1024

    @PostMapping("/api/uploads")
    fun uploadFiles(@RequestParam("files") files: List<MultipartFile>): ResponseEntity<*> {
        return try {
            if (files.isEmpty()) {
                return ResponseEntity.badRequest().body(mapOf("message" to "No se enviaron archivos"))
            }

            if (files.size > 5) {
                return ResponseEntity.badRequest().body(mapOf("message" to "Maximo 5 archivos permitidos"))
            }

            val uploadPath = Paths.get(System.getProperty("user.dir"), uploadDir)
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath)
            }

            val urls = mutableListOf<String>()

            for (file in files) {
                if (file.contentType == null || file.contentType !in allowedTypes) {
                    return ResponseEntity.badRequest().body(
                        mapOf("message" to "Tipo de archivo no permitido: ${file.originalFilename}. Solo JPEG, PNG y WebP.")
                    )
                }
                if (file.size > maxFileSize) {
                    return ResponseEntity.badRequest().body(
                        mapOf("message" to "${file.originalFilename} excede el limite de 5MB.")
                    )
                }

                val ext = when (file.contentType) {
                    "image/jpeg" -> ".jpg"
                    "image/png" -> ".png"
                    "image/webp" -> ".webp"
                    else -> ".jpg"
                }
                val filename = "${UUID.randomUUID()}$ext"
                val targetPath = uploadPath.resolve(filename)
                file.inputStream.use { input ->
                    Files.copy(input, targetPath, StandardCopyOption.REPLACE_EXISTING)
                }

                urls.add("/uploads/$filename")
            }

            ResponseEntity.ok(mapOf("urls" to urls))
        } catch (e: Exception) {
            logger.error("Error uploading files", e)
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(mapOf("message" to "Error al subir archivos: ${e.message}"))
        }
    }
}
