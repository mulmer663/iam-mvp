package com.iam.core.application.service;

import groovy.lang.Binding;
import groovy.lang.GroovyShell;
import groovy.lang.Script;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.codehaus.groovy.control.CompilerConfiguration;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Slf4j
@RequiredArgsConstructor
public class RuleScriptEngine {

    private final CompilerConfiguration compilerConfiguration;
    private final Map<String, Class<? extends Script>> scriptCache = new ConcurrentHashMap<>();

    /**
     * Executes a Groovy script with given parameters.
     * 
     * @param scriptContent The Groovy script body
     * @param scriptHash    SHA-256 hash of the script for caching
     * @param params        Variables to be injected into the script
     * @return The result of the script execution
     */
    public Object execute(String scriptContent, String scriptHash, Map<String, Object> params) {
        try {
            Class<? extends Script> scriptClass = scriptCache.computeIfAbsent(scriptHash, hash -> {
                log.info("Compiling new script with hash: {}", hash);
                GroovyShell shell = new GroovyShell(compilerConfiguration);
                return shell.parse(scriptContent).getClass();
            });

            Script script = scriptClass.getDeclaredConstructor().newInstance();
            script.setBinding(new Binding(params));
            return script.run();
        } catch (Exception e) {
            log.error("Failed to execute groovy script: {}", e.getMessage());
            throw new RuntimeException("Script execution failed", e);
        }
    }
}
