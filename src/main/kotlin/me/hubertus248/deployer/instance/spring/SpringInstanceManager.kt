package me.hubertus248.deployer.instance.spring

import me.hubertus248.deployer.data.dto.AvailableInstance
import me.hubertus248.deployer.data.entity.*
import me.hubertus248.deployer.exception.BadRequestException
import me.hubertus248.deployer.instance.InstanceManager
import me.hubertus248.deployer.instance.InstanceManagerFeature
import me.hubertus248.deployer.instance.InstanceManagerName
import me.hubertus248.deployer.instance.spring.application.SpringApplication
import me.hubertus248.deployer.instance.spring.application.SpringApplicationRepository
import me.hubertus248.deployer.instance.spring.instance.AvailableSpringInstance
import me.hubertus248.deployer.instance.spring.instance.AvailableSpringInstanceService
import me.hubertus248.deployer.instance.spring.instance.SpringInstance
import me.hubertus248.deployer.instance.spring.instance.SpringInstanceRepository
import me.hubertus248.deployer.service.FilesystemStorageService
import me.hubertus248.deployer.service.SubProcessService
import me.hubertus248.deployer.service.WorkspaceService
import me.hubertus248.deployer.service.ZuulService
import me.hubertus248.deployer.util.Util
import org.springframework.beans.factory.annotation.Value
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Component
import java.lang.Exception
import java.lang.IllegalStateException
import java.nio.file.Files
import java.nio.file.Path
import javax.swing.Spring
import javax.transaction.Transactional

val INSTANCE_MANAGER_SPRING_NAME = InstanceManagerName("INSTANCE_MANAGER_CORE_SPRING")


@Component
class SpringInstanceManager(
        private val springApplicationRepository: SpringApplicationRepository,
        private val springInstanceRepository: SpringInstanceRepository,
        private val availableSpringInstanceService: AvailableSpringInstanceService,
        private val workspaceService: WorkspaceService,
        private val filesystemStorageService: FilesystemStorageService,
        private val subProcessService: SubProcessService,
        private val zuulService: ZuulService
) : InstanceManager() {

    @Value("\${deployer.domain}")
    private val domain: String = ""

    @Value("\${deployer.protocol}")
    private val protocol: String = ""

    override fun getFriendlyName(): String = "Spring Application"

    override fun getUniqueName(): InstanceManagerName = INSTANCE_MANAGER_SPRING_NAME

    override fun registerApplication(name: ApplicationName, visibility: Visibility): Application {
        val springApplication = SpringApplication(Secret(Util.secureAlphanumericRandomString(32)), name, visibility)
        springApplicationRepository.save(springApplication)
        return springApplication
    }

    override fun listInstances(appId: Long, pageable: Pageable): List<Instance> {
        return springInstanceRepository.findAllByApplication_Id(appId, pageable)
    }

    override fun getAvailableFeatures(): Set<InstanceManagerFeature> = setOf(
            InstanceManagerFeature.CUSTOM_APPLICATION_INFO,
            InstanceManagerFeature.POSSIBLE_INSTANCE_LIST)

    override fun getOpenUrl(instance: Instance): String = "$protocol://${(instance as SpringInstance).subdomain.value}.$domain"

    override fun getCustomApplicationInfoFragment(): String = "application/spring/springApplicationInfo.html"

    override fun getPossibleInstanceList(appId: Long, pageable: Pageable): List<AvailableInstance> =
            availableSpringInstanceService.listArtifacts(appId, pageable).map { AvailableInstance(it.key, it.lastUpdate) }

    @Transactional
    override fun createInstance(appId: Long, instanceKey: InstanceKey): Instance {
        val application = springApplicationRepository.findFirstById(appId) ?: throw BadRequestException()
        val oldInstance = springInstanceRepository.findFirstByKeyAndApplication(instanceKey, application)
        if (oldInstance != null) throw BadRequestException()

        val instanceTemplate = availableSpringInstanceService.findArtifact(application, instanceKey)
                ?: throw BadRequestException()

        val newWorkspace = workspaceService.createWorkspace()

        try {
            prepareWorkspace(newWorkspace, instanceTemplate)
            return SpringInstance(newWorkspace, null, DomainLabel.randomLabel(), null, instanceKey, application)
        } catch (e: Exception) {
            workspaceService.deleteWorkspace(newWorkspace)
            throw e
        }
    }

    @Transactional
    override fun startInstance(instance: Instance) {
        val springInstance = instance as SpringInstance
        updateInstanceStatus(springInstance)
        if (springInstance.status == InstanceStatus.RUNNING) throw IllegalStateException("Instance already running")
        val workspaceRootPath = workspaceService.getWorkspaceRoot(springInstance.workspace)
        springInstance.process = subProcessService.startProcess(
                workspaceRootPath.toFile(),
                Path.of(workspaceRootPath.toString(), "log.txt").toFile(),
                listOf("java", "-jar", "app.jar")//TODO allow customization
        )
        springInstance.zuulMappingId = zuulService.addMapping(
                "/api/spring/${springInstance.subdomain.value}/**",
                "http://localhost:8080"//TODO dynamic port
        )

        springInstance.status = InstanceStatus.RUNNING
        springInstanceRepository.save(springInstance)
        //TODO create zuul redirect
    }

    private fun prepareWorkspace(workspace: Workspace, instanceTemplate: AvailableSpringInstance) {

        val workspaceRoot = workspaceService.getWorkspaceRoot(workspace)
        Files.copy(filesystemStorageService.getFileContent(instanceTemplate.artifact.fileKey)
                ?: throw IllegalStateException("Artifact not found"),
                Path.of(workspaceRoot.toString(), "app.jar"))
    }

    private fun updateInstanceStatus(instance: SpringInstance) {
        if (instance.status == InstanceStatus.STOPPED) return
        val process = instance.process
        if (process == null || subProcessService.getProcessStatus(process) == SubProcessStatus.DEAD) {
            instance.process = null
            instance.status = InstanceStatus.STOPPED
            springInstanceRepository.save(instance)
        }
    }
}