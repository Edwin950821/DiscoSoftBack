package com.kompralo.infrastructure.config

import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration

@Configuration
@ComponentScan(basePackages = [
    "com.kompralo.domain",
    "com.kompralo.application",
    "com.kompralo.infrastructure",
    "com.kompralo.presentation"
])
class DependencyInjectionConfig
