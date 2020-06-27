package me.hubertus248.deployer.instance

import me.hubertus248.deployer.data.entity.Application
import me.hubertus248.deployer.data.entity.ApplicationName
import me.hubertus248.deployer.data.entity.Instance
import me.hubertus248.deployer.data.entity.Visibility
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Component

@Component
class DockerInstanceManager() : InstanceManager() {

    override fun getFriendlyName(): String = "Docker manager"

    override fun getUniqueName(): InstanceManagerName = InstanceManagerName("INSTANCE_MANAGER_CORE_DOCKER")
    override fun registerApplication(name: ApplicationName, visibility: Visibility): Application {
        TODO("Not yet implemented")
    }

    override fun listInstances(appId: Long, pageable: Pageable): List<Instance> {
        TODO("Not yet implemented")
    }

    override fun getAvailableFeatures(): Set<InstanceManagerFeature> {
        TODO("Not yet implemented")
    }

    override fun getOpenUrl(instance: Instance): String {
        TODO("Not yet implemented")
    }
}