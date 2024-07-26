package com.gamegoo.config;

import static org.springframework.security.config.Customizer.withDefaults;

import com.gamegoo.apiPayload.exception.handler.JWTExceptionHandlerFilter;
import com.gamegoo.filter.JWTFilter;
import com.gamegoo.filter.LoginFilter;
import com.gamegoo.repository.member.MemberRepository;
import com.gamegoo.security.CustomUserDetailService;
import com.gamegoo.util.JWTUtil;
import java.util.Arrays;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final AuthenticationConfiguration authenticationConfiguration;
    private final JWTUtil jwtUtil;
    private final CustomUserDetailService customUserDetailService;
    private final MemberRepository memberRepository;

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration configuration)
        throws Exception {
        return configuration.getAuthenticationManager();
    }

    @Bean
    public JWTFilter jwtFilter() {
        List<String> excludedPaths = Arrays.asList("/swagger-ui/", "/v3/api-docs",
            "/api/member/join", "/api/member/login", "/api/member/email", "/api/member/refresh",
            "/api/member/riot", "/api/security/permit");
        return new JWTFilter(jwtUtil, excludedPaths, customUserDetailService);

    }

    @Bean
    public BCryptPasswordEncoder bCryptPasswordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http

            .csrf(AbstractHttpConfigurer::disable)
            .formLogin(AbstractHttpConfigurer::disable)
            .httpBasic(AbstractHttpConfigurer::disable)
            .cors(withDefaults())
            .authorizeHttpRequests((auth) -> auth
                .antMatchers("/", "/api/member/join", "/api/member/login", "/api/member/email/**",
                    "/api/member/refresh", "/api/member/riot").permitAll()
                .antMatchers("/api/security/permit").permitAll()
                .antMatchers("/", "/swagger-ui/**", "/v3/api-docs/**").permitAll()
                .anyRequest().authenticated())
            .addFilterBefore(new JWTExceptionHandlerFilter(),
                UsernamePasswordAuthenticationFilter.class)
            .addFilterAt(
                new LoginFilter(authenticationManager(authenticationConfiguration), jwtUtil,
                    memberRepository), UsernamePasswordAuthenticationFilter.class)
            .addFilterBefore(jwtFilter(), LoginFilter.class)
            .sessionManagement((session) -> session
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS));

        return http.build();
    }

    @Bean
    public CorsFilter corsFilter() {
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowCredentials(true);
        config.addAllowedOrigin("http://localhost:3000");
        config.addAllowedHeader("*");
        config.addAllowedMethod("*");
        source.registerCorsConfiguration("/**", config);
        return new CorsFilter(source);
    }
}
