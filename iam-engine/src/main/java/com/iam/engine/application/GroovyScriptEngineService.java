package com.iam.engine.application;

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
public class GroovyScriptEngineService {

    private final CompilerConfiguration groovyCompilerConfig;
    private final Map<String, Script> scriptCache = new ConcurrentHashMap<>();

    /**
     * Executes a Groovy script using the provided input data.
     *
     * @param scriptContent the Groovy script code
     * @param scriptHash    hash identifier for caching
     * @param params        parameters injected into the script binding
     * @return Result evaluated by the script
     */
    public Object execute(String scriptContent, String scriptHash, Map<String, Object> params) {
        try {
            long startTime = System.currentTimeMillis();

            Script scriptObj = scriptCache.computeIfAbsent(scriptHash, k -> {
                GroovyShell shell = new GroovyShell(groovyCompilerConfig);
                return shell.parse(scriptContent);
            });

            Binding binding = new Binding(params);
            scriptObj.setBinding(binding);

            Object result = scriptObj.run();

            long elapsed = System.currentTimeMillis() - startTime;
            log.debug("Script execution completed in {} ms", elapsed);

            // Clear binding to avoid memory leak
            scriptObj.setBinding(new Binding());

            return result;
        } catch (Exception e) {
            log.error("Rule script execution failed", e);
            throw new RuntimeException("Script execution error", e);
        }
    }
}
