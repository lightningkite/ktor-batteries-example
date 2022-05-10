@file:UseContextualSerialization(Instant::class, UUID::class, ServerFile::class)

package com.lightningkite.ktorbatteries.demo

import com.lightningkite.ktorbatteries.auth.*
import com.lightningkite.ktorbatteries.client
import com.lightningkite.ktorbatteries.db.*
import com.lightningkite.ktorbatteries.email.EmailSettings
import com.lightningkite.ktorbatteries.exceptions.ExceptionSettings
import com.lightningkite.ktorbatteries.files.FilesSettings
import com.lightningkite.ktorbatteries.files.configureFiles
import com.lightningkite.ktorbatteries.jsonschema.JsonSchema
import com.lightningkite.ktorbatteries.logging.LoggingSettings
import com.lightningkite.ktorbatteries.mongo.MongoSettings
import com.lightningkite.ktorbatteries.mongo.mongoDb
import com.lightningkite.ktorbatteries.serialization.Serialization
import com.lightningkite.ktorbatteries.serialization.configureSerialization
import com.lightningkite.ktorbatteries.serverhealth.configureHealth
import com.lightningkite.ktorbatteries.serverhealth.healthCheckPage
import com.lightningkite.ktorbatteries.settings.GeneralServerSettings
import com.lightningkite.ktorbatteries.settings.loadSettings
import com.lightningkite.ktorbatteries.settings.runServer
import com.lightningkite.ktorbatteries.typed.apiHelp
import com.lightningkite.ktorbatteries.typed.fileFieldNames
import com.lightningkite.ktordb.*
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.request.forms.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.Principal
import io.ktor.server.plugins.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.util.*
import kotlinx.serialization.*
import java.io.File
import java.lang.Exception
import java.time.Instant
import java.util.*

@Serializable
@DatabaseModel
data class TestModel(
    override val _id: UUID = UUID.randomUUID(),
    val timestamp: Instant = Instant.now(),
    val name: String = "No Name",
    val number: Int = 3123,
    @JsonSchema.Format("jodit") val content: String = "",
    val file: ServerFile? = null
) : HasId

data class DirectPrincipal(val id: String) : Principal

@Serializable
data class Settings(
    val general: GeneralServerSettings = GeneralServerSettings(),
    val auth: AuthSettings = AuthSettings(),
    val files: FilesSettings = FilesSettings(),
    val logging: LoggingSettings = LoggingSettings(),
    val mongo: MongoSettings = MongoSettings(),
    val exception: ExceptionSettings = ExceptionSettings(),
    val email: EmailSettings = EmailSettings(),
)

@OptIn(InternalAPI::class)
fun main(vararg args: String) {
    loadSettings(File("settings.yaml")) { Settings() }

    runServer {
        configureFiles()
        configureSerialization()
        authentication {
            quickJwt { creds ->
                DirectPrincipal(
                    creds.payload
                        .getClaim(AuthSettings.userIdKey)
                        .asString()
                )
            }
        }
        install(StatusPages) {
            exception<Exception> { call, cause -> call.respondText(cause.message ?: "Unknown Error") }
        }
        routing {
            authenticate(optional = true) {
                route("test-model") {
                    restApi(path = "rest", getCollection = { user: DirectPrincipal? -> database.collection<TestModel>() })
                    adminPages(path = "admin", defaultItem = { TestModel() }, getCollection = { user: DirectPrincipal? -> database.collection<TestModel>() })
                }
                get {
                    call.respondText("Welcome, ${call.principal<DirectPrincipal>()?.id}!")
                }
                healthCheckPage("health", listOf(
                    MongoSettings.instance,
                    ExceptionSettings.instance,
                    FilesSettings.instance,
                    EmailSettings.instance,
                ))
                adminIndex()
                apiHelp()
                route("auth") {
                    emailMagicLinkEndpoint(makeLink = {
                        GeneralServerSettings.instance.publicUrl + "?jwt=" + makeToken(it)
                    })
                    oauthGoogle() { it }
                    oauthGithub() { it }
                    oauthApple() { it }
                }
            }
        }
    }
}