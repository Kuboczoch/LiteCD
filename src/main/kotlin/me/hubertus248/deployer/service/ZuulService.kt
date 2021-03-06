package me.hubertus248.deployer.service

import me.hubertus248.deployer.data.entity.ZuulMappingId
import org.springframework.beans.factory.annotation.Value
import org.springframework.cloud.netflix.zuul.filters.ZuulProperties
import org.springframework.cloud.netflix.zuul.web.ZuulHandlerMapping
import org.springframework.stereotype.Service
import java.util.*

interface ZuulService {
    fun addMapping(path: String, target: String): ZuulMappingId

    fun removeMapping(zuulMappingId: ZuulMappingId)
}

@Service
class ZuulServiceImpl(
        private val zuulProperties: ZuulProperties,
        private val zuulHandlerMapping: ZuulHandlerMapping
) : ZuulService {

    override fun addMapping(path: String, target: String): ZuulMappingId {
        val newMappingId = ZuulMappingId(UUID.randomUUID())
        zuulProperties.routes[newMappingId.value.toString()] = ZuulProperties.ZuulRoute(
                newMappingId.value.toString(),
                path,
                "",
                target,
                true,
                false,
                setOf("Cookie", "Set-Cookie")
        )
        zuulHandlerMapping.setDirty(true)
        return newMappingId
    }

    override fun removeMapping(zuulMappingId: ZuulMappingId) {
        zuulProperties.routes.remove(zuulMappingId.value.toString())
        zuulHandlerMapping.setDirty(true)
    }

}