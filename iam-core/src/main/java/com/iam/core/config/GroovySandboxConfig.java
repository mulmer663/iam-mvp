package com.iam.core.config;

import org.codehaus.groovy.control.CompilerConfiguration;
import org.codehaus.groovy.control.customizers.SecureASTCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Arrays;
import java.util.List;

@Configuration
public class GroovySandboxConfig {

    @Bean
    public CompilerConfiguration groovyCompilerConfig() {
        SecureASTCustomizer customizer = new SecureASTCustomizer();

        // 1. Basic restrictions
        customizer.setClosuresAllowed(false);
        customizer.setMethodDefinitionAllowed(false);
        customizer.setPackageAllowed(false);

        // 2. Performance & Security: Whitelist classes
        List<String> whiteList = Arrays.asList(
                "java.lang.Object",
                "java.lang.String",
                "java.lang.Integer",
                "java.lang.Double",
                "java.lang.Boolean",
                "java.lang.Math",
                "java.util.Map",
                "java.util.List",
                "java.util.ArrayList",
                "java.util.HashMap",
                "java.util.Date",
                "java.time.LocalDateTime",
                "java.time.format.DateTimeFormatter",
                "com.iam.core.domain.vo.UniversalData",
                "com.iam.core.domain.vo.StringData",
                "com.iam.core.domain.vo.IntData",
                "com.iam.core.domain.vo.TimeData",
                "com.iam.core.domain.vo.BooleanData");
        customizer.setAllowedImports(whiteList);
        customizer.setAllowedReceivers(whiteList);

        // 3. Blacklist dangerous stuff
        customizer.setDisallowedStaticStarImports(Arrays.asList("java.lang.System", "java.lang.Runtime"));

        CompilerConfiguration config = new CompilerConfiguration();
        config.addCompilationCustomizers(customizer);

        return config;
    }
}
