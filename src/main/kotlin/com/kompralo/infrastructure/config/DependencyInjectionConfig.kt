package com.kompralo.infrastructure.config

import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration

@Configuration
@ComponentScan(basePackages = [
    "com.kompralo.domain",
    "com.kompralo.application",
    "com.kompralo.presentation"
    // "com.kompralo.infrastructure" se excluye para evitar conflictos
    // con adapters/repos duplicados en infrastructure.persistence
])
class DependencyInjectionConfig
