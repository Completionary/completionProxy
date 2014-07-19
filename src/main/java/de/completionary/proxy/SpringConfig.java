package de.completionary.proxy;

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.config.CustomScopeConfigurer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.SimpleThreadScope;

@Configuration
public class SpringConfig {

    @Bean
    public CustomScopeConfigurer customScopeConfigurer() {
        System.out.println("SpringConfig called!!!!");
        CustomScopeConfigurer configuerer = new CustomScopeConfigurer();
        Map<String, Object> scopes = new HashMap<String, Object>();
        scopes.put("thread", new SimpleThreadScope());
        configuerer.setScopes(scopes);
        return configuerer;
    }

}
