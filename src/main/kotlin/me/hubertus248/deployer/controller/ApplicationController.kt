package me.hubertus248.deployer.controller

import me.hubertus248.deployer.data.dto.CreateApplicationDTO
import me.hubertus248.deployer.data.entity.ApplicationName
import me.hubertus248.deployer.data.entity.InstanceKey
import me.hubertus248.deployer.exception.NotFoundException
import me.hubertus248.deployer.instance.InstanceManagerFeature
import me.hubertus248.deployer.security.IsAdmin
import me.hubertus248.deployer.service.ApplicationService
import me.hubertus248.deployer.service.InstanceManagerService
import me.hubertus248.deployer.service.InstanceService
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.data.web.PageableDefault
import org.springframework.security.core.Authentication
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.*
import org.springframework.web.servlet.view.RedirectView
import java.security.Principal

@Controller
class ApplicationController(
    private val applicationService: ApplicationService,
    private val instanceManagerService: InstanceManagerService,
    private val instanceService: InstanceService
) {

    @GetMapping("/apps")
    fun applicationList(
        principal: Principal?,
        @PageableDefault(size = 10) pageable: Pageable,
        model: Model
    ): String {
        if (principal == null) model.addAttribute(
            "applications",
            applicationService.listPublicApplications(pageable)
        )
        else
            model.addAttribute("applications", applicationService.listApplications(pageable))

        model.addAttribute("instanceManagerService", instanceManagerService)
        return "apps"
    }

    @IsAdmin
    @GetMapping("/newApp")
    fun newApp(model: Model): String {
        model.addAttribute("managers", instanceManagerService.getAvailableManagers())
        return "newApp"
    }

    @IsAdmin
    @PostMapping("/app/{appId}/deleteApp")
    fun deleteApp(@PathVariable appId: Long): RedirectView {
        applicationService.deleteApplication(appId)
        return RedirectView("/apps")
    }

    @IsAdmin
    @PostMapping("/newApp")
    fun newAppPost(@ModelAttribute @Validated createApplicationDTO: CreateApplicationDTO): RedirectView {
        applicationService.createApplication(
            ApplicationName(createApplicationDTO.name),
            createApplicationDTO.visibility,
            createApplicationDTO.manager
        )
        return RedirectView("apps")
    }

    @GetMapping("/app/{appId}")
    fun getApp(@PathVariable appId: Long, model: Model, authentication: Authentication?): String {
        val application = applicationService.findApplication(appId, authentication?.isAuthenticated ?: false)
            ?: throw NotFoundException()
        val instanceManager = instanceManagerService.getManagerForApplication(application)

        model.addAttribute("app", application)
        model.addAttribute("instanceManager", instanceManager)
        model.addAttribute(
            "instances",
            instanceManager.listInstances(application.id, PageRequest.of(0, 50))
        )//TODO pagination

        if (instanceManager.supportsFeature(InstanceManagerFeature.POSSIBLE_INSTANCE_LIST)) {
            model.addAttribute(
                "possibleInstances",
                instanceManager.getPossibleInstanceList(application.id, PageRequest.of(0, 50))
            )//TODO pagination
        }
        return "app"
    }

    @IsAdmin
    @PostMapping("/app/{appId}/create")
    fun create(@PathVariable appId: Long, @RequestParam key: String): RedirectView {
        instanceService.createAndStart(appId, InstanceKey(key))
        return RedirectView("/app/$appId")
    }

    @IsAdmin
    @PostMapping("/app/{appId}/start")
    fun start(@PathVariable appId: Long, @RequestParam key: String): RedirectView {
        instanceService.start(appId, InstanceKey(key))
        return RedirectView("/app/$appId")
    }

    @IsAdmin
    @PostMapping("/app/{appId}/stop")
    fun stop(@PathVariable appId: Long, @RequestParam key: String): RedirectView {
        instanceService.stop(appId, InstanceKey(key))
        return RedirectView("/app/$appId")
    }


    @IsAdmin
    @PostMapping("/app/{appId}/delete")
    fun delete(@PathVariable appId: Long, @RequestParam key: String): RedirectView {
        instanceService.delete(appId, InstanceKey(key))
        return RedirectView("/app/$appId")
    }

    /**
     * delete and create instance again, preserve instance configuration
     */
    @IsAdmin
    @PostMapping("/app/{appId}/recreate")
    fun recreate(@PathVariable appId: Long, @RequestParam key: String): RedirectView {
        instanceService.recreate(appId, InstanceKey(key))
        return RedirectView("/app/$appId")
    }

    @IsAdmin
    @PostMapping("/app/{appId}/deleteAvailableInstance")
    fun deleteAvailableInstance(@PathVariable appId: Long, @RequestParam key: String): RedirectView {
        instanceService.deleteAvailableInstance(appId, InstanceKey(key))
        return RedirectView("/app/$appId")
    }
}