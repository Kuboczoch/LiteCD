package me.hubertus248.deployer.instance.spring

import me.hubertus248.deployer.data.dto.EnvironmentDTO
import me.hubertus248.deployer.data.entity.EnvironmentVariable
import me.hubertus248.deployer.data.entity.EnvironmentVariableName
import me.hubertus248.deployer.data.entity.EnvironmentVariableValue
import me.hubertus248.deployer.data.reposiotry.EnvironmentVariableRepository
import me.hubertus248.deployer.exception.NotFoundException
import me.hubertus248.deployer.instance.spring.application.SpringApplication
import me.hubertus248.deployer.instance.spring.application.SpringApplicationRepository
import me.hubertus248.deployer.instance.spring.instance.AvailableSpringInstanceRepository
import me.hubertus248.deployer.instance.spring.instance.SpringInstance
import me.hubertus248.deployer.instance.spring.instance.SpringInstanceRepository
import org.springframework.stereotype.Service
import java.lang.RuntimeException
import javax.transaction.Transactional

interface SpringEnvironmentService {

    fun updateApplicationEnvironment(appId: Long, environment: EnvironmentDTO)

    fun updateInstanceEnvironment(instanceId: Long, environment: EnvironmentDTO)

    fun setDefaultInstanceEnvironment(instance: SpringInstance)

    fun getEnvironment(application: SpringApplication): Map<String, String>

    fun getEnvironment(instance: SpringInstance): Map<String, String>
}

@Service
class SpringEnvironmentServiceImpl(
        private val springApplicationRepository: SpringApplicationRepository,
        private val springInstanceRepository: SpringInstanceRepository,
        private val environmentVariableRepository: EnvironmentVariableRepository
) : SpringEnvironmentService {

    @Transactional
    override fun updateApplicationEnvironment(appId: Long, environment: EnvironmentDTO) {
        val application = springApplicationRepository.findFirstById(appId) ?: throw NotFoundException()

        application.defaultEnvironment.forEach { environmentVariableRepository.delete(it) }
        application.defaultEnvironment.clear()

        environment.variables.map { EnvironmentVariable(0, EnvironmentVariableName(it.name), EnvironmentVariableValue(it.value)) }
                .forEach {
                    environmentVariableRepository.save(it)
                    application.defaultEnvironment.add(it)
                }
        springApplicationRepository.save(application)
    }

    override fun updateInstanceEnvironment(instanceId: Long, environment: EnvironmentDTO) {
        TODO("Not yet implemented")
    }

    @Transactional
    override fun setDefaultInstanceEnvironment(instance: SpringInstance) {
        val application = instance.application as SpringApplication
        instance.environment.forEach { environmentVariableRepository.delete(it) }
        instance.environment.clear()

        application.defaultEnvironment
                .map { EnvironmentVariable(0, it.name, EnvironmentVariableValue(replaceKeywords(it.value.value, instance))) }
                .forEach {
                    environmentVariableRepository.save(it)
                    instance.environment.add(it)
                }
        springInstanceRepository.save(instance)
    }

    override fun getEnvironment(application: SpringApplication): Map<String, String> {
        return mapOf(*application.defaultEnvironment.map { Pair(it.name.value, it.value.value) }.toTypedArray())
    }

    override fun getEnvironment(instance: SpringInstance): Map<String, String> {
        return mapOf(*instance.environment.map { Pair(it.name.value, replaceKeywords(it.value.value, instance)) }.toTypedArray())
    }

    private fun replaceKeywords(input: String, instance: SpringInstance): String {
        return input.replace("\$PORT\$", instance.port?.value.toString())
    }

}