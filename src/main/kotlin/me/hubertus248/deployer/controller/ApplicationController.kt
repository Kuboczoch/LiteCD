package me.hubertus248.deployer.controller

import me.hubertus248.deployer.data.dto.CreateApplicationDTO
import me.hubertus248.deployer.security.Authenticated
import me.hubertus248.deployer.service.ApplicationService
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.ModelAttribute
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.servlet.view.RedirectView
import java.security.Principal

@Controller
class ApplicationController(
        private val applicationService: ApplicationService
) {

    @GetMapping("/apps")
    fun applicationList(principal: Principal?, pageable: Pageable, model: Model): String {
        if (principal == null) model.addAttribute("applications",
                applicationService.listPublicApplications(pageable))
        else
            model.addAttribute("applications", applicationService.listApplications(pageable))

        return "apps"
    }

    //TODO restrict to admin
    @Authenticated
    @GetMapping("/newApp")
    fun newApp(): String = "newApp"

    //TODO restrict to admin
    @Authenticated
    @PostMapping("/newApp")
    fun newAppPost(@ModelAttribute @Validated createApplicationDTO: CreateApplicationDTO): RedirectView {
        applicationService.createApplication(createApplicationDTO.name, createApplicationDTO.visibility)
        return RedirectView("apps")//TODO redirect to app page
    }
}