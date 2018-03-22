/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package eu.h2020.symbiote.ssp.rap;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

/**
 *
 * @author Matteo Pardi <m.pardi@nextworks.it>
 */

@Configuration
public class RapConfig {

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}
