package me.hubertus248.deployer.data.entity

import javax.persistence.*

@Entity
class Application(
        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        val id: Long,

        @Column(nullable = false, unique = true, updatable = false, length = 255)
        val name: String,

        @Column(unique = false, updatable = true)
        val visibility: Visibility,

        @OneToMany(fetch = FetchType.LAZY)
        val instances: Set<Instance> = emptySet()
)

enum class Visibility {
    PUBLIC,
    RESTRICTED
}